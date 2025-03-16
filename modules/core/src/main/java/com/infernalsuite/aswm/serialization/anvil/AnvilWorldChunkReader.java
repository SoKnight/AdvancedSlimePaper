package com.infernalsuite.aswm.serialization.anvil;

import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AnvilWorldChunkReader {

    private static final List<String> REAL_CHUNK_STATUSES = List.of("postprocessed", "full");
    private static final int SECTOR_SIZE = 4096;

    static Long2ObjectMap<SlimeChunk> loadWorldChunks(Path environmentDir, int worldVersion) throws InvalidWorldException, IOException {
        Path regionDir = environmentDir.resolve("region");
        if (!Files.isDirectory(regionDir))
            throw new InvalidWorldException(environmentDir);

        Long2ObjectMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();
        try (var stream = Files.newDirectoryStream(regionDir, path -> path.toString().endsWith(".mca"))) {
            for (Path path : stream) {
                log.info("Loading region file '{}'...", path.getFileName());
                chunks.putAll(loadChunks(path, worldVersion).stream().collect(Collectors.toMap(
                        chunk -> SlimeWorld.chunkPosition(chunk.getX(), chunk.getZ()),
                        Function.identity()
                )));
            }
        }

        return chunks;
    }

    private static List<SlimeChunk> loadChunks(Path path, int worldVersion) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(path);
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionByteArray));
        List<ChunkEntry> chunkEntries = new ArrayList<>(1024);

        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            if (entry != 0) {
                int chunkOffset = entry >>> 8;
                int chunkSize = entry & 15;
                chunkEntries.add(new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE));
            }
        }

        return chunkEntries.stream()
                .map(entry -> readChunkFromEntry(regionByteArray, entry, worldVersion))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static SlimeChunk readChunkFromEntry(byte[] regionByteArray, ChunkEntry entry, int worldVersion) {
        try {
            DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset(), entry.paddedSize()));
            int chunkSize = headerStream.readInt() - 1;
            int compressionScheme = headerStream.readByte();

            DataInputStream rawChunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset() + 5, chunkSize));
            InputStream chunkStream = compressionScheme == 1
                    ? new GZIPInputStream(rawChunkStream)
                    : new InflaterInputStream(rawChunkStream);

            CompoundBinaryTag levelTag = BinaryTagIO.unlimitedReader().read(chunkStream).getCompound("Level");
            if (levelTag.keySet().isEmpty())
                throw new RuntimeException("Missing Level tag?");

            return readChunk(levelTag, worldVersion);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static SlimeChunk readChunk(CompoundBinaryTag levelTag, int worldVersion) {
        int chunkX = levelTag.getInt("xPos");
        int chunkZ = levelTag.getInt("zPos");

        int dataVersion = levelTag.getInt("DataVersion", -1);
        if (dataVersion != -1 && dataVersion != worldVersion) {
            log.error("Cannot load chunk at ({},{}): data version {} doesn't match world version {}", chunkX, chunkZ, dataVersion, worldVersion);
            return null;
        }

        String status = levelTag.getString("Status", "");
        if (!status.isEmpty() && !REAL_CHUNK_STATUSES.contains(status.toLowerCase()))
            // It's a protochunk
            return null;

        SlimeChunkSection[] sections = readChunkSections(getCompoundTagsList(levelTag, "Sections"));
        if (Arrays.stream(sections).allMatch(Objects::isNull))
            // Chunk is empty
            return null;

        CompoundBinaryTag heightMaps = levelTag.getCompound("Heightmaps");
        int[] biomes = levelTag.getIntArray("Biomes", null);
        List<CompoundBinaryTag> tileEntities = getCompoundTagsList(levelTag, "TileEntities");
        List<CompoundBinaryTag> entities = getCompoundTagsList(levelTag, "Entities");
        CompoundBinaryTag extraData = readExtraData(levelTag);
        return new SlimeChunkSkeleton(chunkX, chunkZ, sections, heightMaps, biomes, tileEntities, entities, extraData);
    }

    private static SlimeChunkSection[] readChunkSections(List<CompoundBinaryTag> sectionTags) {
        SlimeChunkSection[] sections = new SlimeChunkSection[16];
        for (CompoundBinaryTag sectionTag : sectionTags) {
            int index = sectionTag.getByte("Y", (byte) -1);
            if (index < 0)
                // Skip an empty section with Y = -1.
                continue;

            ListBinaryTag blocksPalette = sectionTag.getList("Palette", BinaryTagTypes.COMPOUND, null);
            long[] blockStates = sectionTag.getLongArray("BlockStates", null);
            if (blocksPalette == null || blockStates == null || isEmpty(blockStates))
                // Skip an empty section without palette and block states
                continue;

            NibbleArray blockLightArray = applyByteArrayOrNull(sectionTag, "BlockLight", NibbleArray::new);
            NibbleArray skyLightArray = applyByteArrayOrNull(sectionTag, "SkyLight", NibbleArray::new);

            sections[index] = new SlimeChunkSectionSkeleton(blocksPalette, blockStates, blockLightArray, skyLightArray);
        }

        return sections;
    }

    private static @NotNull CompoundBinaryTag readExtraData(CompoundBinaryTag levelTag) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

        CompoundBinaryTag chunkBukkitValuesTag = levelTag.getCompound("ChunkBukkitValues");
        if (!chunkBukkitValuesTag.keySet().isEmpty())
            builder.put("ChunkBukkitValues", chunkBukkitValuesTag);

        return builder.build();
    }

    private static @NotNull List<CompoundBinaryTag> getCompoundTagsList(CompoundBinaryTag compound, String block_entities) {
        return compound.getList(block_entities, BinaryTagTypes.COMPOUND).stream()
                .map(t -> (CompoundBinaryTag) t)
                .toList();
    }

    private static <T> @Nullable T applyByteArrayOrNull(CompoundBinaryTag tag, String key, Function<byte[], T> transform) {
        byte[] bytes = tag.getByteArray(key, null);
        return bytes != null && bytes.length != 0 ? transform.apply(bytes) : null;
    }

    private static boolean isEmpty(long[] array) {
        for (long item : array)
            if (item != 0L)
                return false;

        return true;
    }

    private record ChunkEntry(int offset, int paddedSize) { }
    
}
