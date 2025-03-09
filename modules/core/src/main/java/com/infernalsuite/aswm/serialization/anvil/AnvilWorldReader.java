package com.infernalsuite.aswm.serialization.anvil;

import com.infernalsuite.aswm.Util;
import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.SlimeWorldReader;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.nbt.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@Slf4j
public final class AnvilWorldReader implements SlimeWorldReader<AnvilImportData> {

    private static final int SECTOR_SIZE = 4096;

    public static final AnvilWorldReader INSTANCE = new AnvilWorldReader();

    @Override
    public SlimeWorld readFromData(AnvilImportData importData) {
        Path worldDir = importData.worldDir();

        try {
            Path levelFile = worldDir.resolve("level.dat");
            if (!Files.isRegularFile(levelFile))
                throw new RuntimeException(new InvalidWorldException(worldDir));

            LevelData data = readLevelData(levelFile);
            int worldVersion = data.version;

            SlimePropertyMap propertyMap = new SlimePropertyMap();

            // TODO - Really? There has to be a better way...
            Path environmentDir = worldDir.resolve("DIM-1");
            propertyMap.setValue(SlimeProperties.ENVIRONMENT, "nether");
            if (!Files.isDirectory(environmentDir)) {
                environmentDir = worldDir.resolve("DIM1");
                propertyMap.setValue(SlimeProperties.ENVIRONMENT, "the_end");
                if (!Files.isDirectory(environmentDir)) {
                    environmentDir = worldDir;
                    propertyMap.setValue(SlimeProperties.ENVIRONMENT, "normal");
                }
            }

            // Chunks
            Path regionDir = environmentDir.resolve("region");
            if (!Files.isDirectory(regionDir))
                throw new InvalidWorldException(environmentDir);

            Long2ObjectMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();
            try (var stream = Files.newDirectoryStream(regionDir, path -> path.toString().endsWith(".mca"))) {
                for (final Path path : stream) {
                    log.info("Loading region file {}...", path.getFileName());
                    chunks.putAll(loadChunks(path, worldVersion).stream().collect(Collectors.toMap(
                            chunk -> Util.chunkPosition(chunk.getX(), chunk.getZ()),
                            Function.identity()
                    )));
                }
            }

            // Entity serialization
            Path entityDir = environmentDir.resolve("entities");
            if (Files.exists(entityDir)) {
                if (!Files.isDirectory(entityDir)) throw new InvalidWorldException(environmentDir, "'entities' is not a directory!");
            }

            try (var stream = Files.newDirectoryStream(entityDir, path -> path.toString().endsWith(".mca"))) {
                for (final Path path : stream) {
                    log.info("Loading entity region file {}...", path.getFileName());
                    loadEntities(path, worldVersion, chunks);
                }
            }

            if (chunks.isEmpty()) {
                throw new InvalidWorldException(environmentDir);
            }

            // World maps
//        File dataDir = new File(worldDir, "data");
//        List<CompoundTag> maps = new ArrayList<>();
//
//        if (dataDir.exists()) {
//            if (!dataDir.isDirectory()) {
//                throw new InvalidWorldException(worldDir);
//            }
//
//            for (File mapFile : dataDir.listFiles((dir, name) -> MAP_FILE_PATTERN.matcher(name).matches())) {
//                maps.add(loadMap(mapFile));
//            }
//        }

            propertyMap.setValue(SlimeProperties.SPAWN_X, data.x);
            propertyMap.setValue(SlimeProperties.SPAWN_Y, data.y);
            propertyMap.setValue(SlimeProperties.SPAWN_Z, data.z);

            return new SkeletonSlimeWorld(importData.newName(), importData.loader(), true, chunks, new ConcurrentHashMap<>(), propertyMap, worldVersion);
        } catch (IOException | InvalidWorldException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static CompoundBinaryTag loadMap(Path mapFile) throws IOException {
        String fileName = mapFile.getFileName().toString();
        int mapId = Integer.parseInt(fileName.substring(4, fileName.length() - 4));
        CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(mapFile).getCompound("data");
        tag.put("id", IntBinaryTag.intBinaryTag(mapId));
        return tag;
    }

    private static LevelData readLevelData(Path file) throws IOException, InvalidWorldException {
        CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(file);
        CompoundBinaryTag dataTag = tag.getCompound("Data");

        if (dataTag.size() != 0) {
            int dataVersion = dataTag.getInt("DataVersion", -1);
            int spawnX = dataTag.getInt("SpawnX", 0);
            int spawnY = dataTag.getInt("SpawnY", 255);
            int spawnZ = dataTag.getInt("SpawnZ", 0);
            return new LevelData(dataVersion, spawnX, spawnY, spawnZ);
        }

        throw new InvalidWorldException(file.getParent());
    }

    private static void loadEntities(Path path, int version, Long2ObjectMap<SlimeChunk> chunkMap) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(path);
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionByteArray));
        List<ChunkEntry> chunks = new ArrayList<>(1024);

        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;

            if (entry != 0) {
                ChunkEntry chunkEntry = new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE);
                chunks.add(chunkEntry);
            }
        }

        for (ChunkEntry entry : chunks) {
            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset(), entry.paddedSize()));
                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();

                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(decompressorStream);

                readEntityChunk(tag, version, chunkMap);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static List<SlimeChunk> loadChunks(Path path, int worldVersion) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(path);
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionByteArray));
        List<ChunkEntry> chunks = new ArrayList<>(1024);

        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;

            if (entry != 0) {
                ChunkEntry chunkEntry = new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE);
                chunks.add(chunkEntry);
            }
        }

        return chunks.stream().map((entry) -> {
            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset(), entry.paddedSize()));
                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();

                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(decompressorStream);
                return readChunk(tag, worldVersion);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static void readEntityChunk(CompoundBinaryTag compound, int worldVersion, Long2ObjectMap<SlimeChunk> slimeChunkMap) {
        int[] position = compound.getIntArray("Position");
        if (position.length == 0) throw new IllegalStateException("Entity chunk is missing position data");
        int chunkX = position[0];
        int chunkZ = position[1];

        int dataVersion = compound.getInt("DataVersion", -1);
        if (dataVersion != worldVersion) {
            log.error("Cannot load entity chunk at ({},{}): data version {} does not match world version {}", chunkX, chunkZ, dataVersion, worldVersion);
            return;
        }

        SlimeChunk chunk = slimeChunkMap.get(Util.chunkPosition(chunkX, chunkZ));
        if (chunk == null) {
            log.warn("Lost entity chunk data at ({},{})", chunkX, chunkZ);
        } else {
            compound.getList("Entities", BinaryTagTypes.COMPOUND).forEach(entityTag -> chunk.getEntities().add((CompoundBinaryTag) entityTag));
        }
    }

    private static SlimeChunk readChunk(CompoundBinaryTag compound, int worldVersion) {
        int chunkX = compound.getInt("xPos");
        int chunkZ = compound.getInt("zPos");

        int dataVersion = compound.getInt("DataVersion", -1);
        if (dataVersion != worldVersion) {
            log.error("Cannot load chunk at {},{}: data version {} does not match world version {}", chunkX, chunkZ, dataVersion, worldVersion);
            return null;
        }

        String status = compound.getString("Status", "");
        if (!status.isEmpty()) {
            // TODO - Check if this is correct, looks like the status string format may have changed...
            if (!status.equals("postprocessed") && !status.startsWith("full") && !status.startsWith("minecraft:full")) {
                // It's a protochunk
                return null;
            }
        }

        CompoundBinaryTag heightMaps = compound.getCompound("Heightmaps");

        List<CompoundBinaryTag> tileEntities = compound.getList("block_entities", BinaryTagTypes.COMPOUND).stream().map(t -> (CompoundBinaryTag) t).toList();
        List<CompoundBinaryTag> entities = compound.getList("entities", BinaryTagTypes.COMPOUND).stream().map(t -> (CompoundBinaryTag) t).toList();
        ListBinaryTag sectionsTag = compound.getList("sections", BinaryTagTypes.COMPOUND);

        int minSectionY = compound.getInt("yPos");
        // TODO - look into this +1 below
        int maxSectionY = sectionsTag.stream().map(tag -> ((CompoundBinaryTag) tag).getByte("Y")).max(Byte::compareTo).orElse((byte) 0) + 1; // Add 1 to the section, as we serialize it with the 1 added.

        SlimeChunkSection[] sectionArray = new SlimeChunkSection[maxSectionY - minSectionY];

        for (final BinaryTag rawRag : sectionsTag) {
            final CompoundBinaryTag sectionTag = (CompoundBinaryTag) rawRag;
            int index = sectionTag.getByte("Y");

            CompoundBinaryTag blockStatesTag = sectionTag.getCompound("block_states");
            CompoundBinaryTag biomesTag = sectionTag.getCompound("biomes");

            // TODO - actually, the section is empty if the block_states palette only contains air so uh... yeah xD fix this :P
            // NB - maybe consider an import flag to respect the original biome even if its an empty section, or just strip and replace with the world default
            if (blockStatesTag.size() == 0 && biomesTag.size() == 0) continue; // Empty section

            NibbleArray blockLightArray = applyByteArrayOrNull(sectionTag, "BlockLight", NibbleArray::new);
            NibbleArray skyLightArray = applyByteArrayOrNull(sectionTag, "SkyLight", NibbleArray::new);

            sectionArray[index - minSectionY] = new SlimeChunkSectionSkeleton(blockStatesTag, biomesTag, blockLightArray, skyLightArray);
        }

        CompoundBinaryTag.Builder extraTagBuilder = CompoundBinaryTag.builder();
        CompoundBinaryTag chunkBukkitValues = compound.getCompound("ChunkBukkitValues");
        if (chunkBukkitValues.size() > 0) extraTagBuilder.put("ChunkBukkitValues", chunkBukkitValues);
        CompoundBinaryTag extraTag = extraTagBuilder.build();

        // Find first non-null chunk section. If all sections are null, chunk is empty so return null
        return Arrays.stream(sectionArray)
                .filter(Objects::nonNull)
                .findFirst()
                .map(x -> new SlimeChunkSkeleton(chunkX, chunkZ, sectionArray, heightMaps, tileEntities, entities, extraTag, null))
                .orElse(null);
    }

    private static <T> T applyByteArrayOrNull(final CompoundBinaryTag tag, final String key, final Function<byte[], T> transform) {
        byte[] res = tag.getByteArray(key);
        return res.length == 0 ? null : transform.apply(res);
    }

    private record ChunkEntry(int offset, int paddedSize) { }

    private record LevelData(int version, int x, int y, int z) { }

}
