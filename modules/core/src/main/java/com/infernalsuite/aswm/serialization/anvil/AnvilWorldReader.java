package com.infernalsuite.aswm.serialization.anvil;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public final class AnvilWorldReader implements SlimeWorldReader<AnvilImportData> {

    public static final int V1_16 = 2566;
    public static final int V1_16_5 = 2586;
    public static final int V1_17_1 = 2730;
    public static final int V1_19_2 = 3120;

    private static final Pattern MAP_FILE_PATTERN = Pattern.compile("^map_([0-9]*).dat$");
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
            try (Stream<Path> paths = Files.list(regionDir)) {
                Iterator<Path> iterator = paths.filter(path -> path.endsWith(".mca")).iterator();
                while (iterator.hasNext()) {
                    chunks.putAll(loadChunks(iterator.next(), worldVersion).stream().collect(Collectors.toMap(
                            chunk -> Util.chunkPosition(chunk.getX(), chunk.getZ()),
                            Function.identity()
                    )));
                }
            }

            // Entity serialization
            {
                Path entityRegion = environmentDir.resolve("entities");
                if (Files.isDirectory(entityRegion)) {
                    try (Stream<Path> paths = Files.list(regionDir)) {
                        Iterator<Path> iterator = paths.filter(path -> path.endsWith(".mca")).iterator();
                        while (iterator.hasNext()) {
                            loadEntities(iterator.next(), worldVersion, chunks);
                        }
                    }
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

            // Extra Data
            CompoundMap extraData = new CompoundMap();
            propertyMap.setValue(SlimeProperties.SPAWN_X, data.x);
            propertyMap.setValue(SlimeProperties.SPAWN_Y, data.y);
            propertyMap.setValue(SlimeProperties.SPAWN_Z, data.z);
            return new SkeletonSlimeWorld(importData.newName(), importData.loader(), true, chunks, new CompoundTag("", extraData), propertyMap, worldVersion);
        } catch (IOException | InvalidWorldException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static CompoundTag loadMap(Path mapFile) throws IOException {
        String fileName = mapFile.getFileName().toString();
        int mapId = Integer.parseInt(fileName.substring(4, fileName.length() - 4));

        CompoundTag tag;
        try (var nbtStream = new NBTInputStream(Files.newInputStream(mapFile), NBTInputStream.GZIP_COMPRESSION, ByteOrder.BIG_ENDIAN)) {
            tag = nbtStream.readTag().getAsCompoundTag().flatMap(t -> t.getAsCompoundTag("data")).orElseThrow();
        }

        tag.getValue().put("id", new IntTag("id", mapId));
        return tag;
    }

    private static LevelData readLevelData(Path file) throws IOException, InvalidWorldException {
        Optional<CompoundTag> tag;
        try (var nbtStream = new NBTInputStream(Files.newInputStream(file))) {
            tag = nbtStream.readTag().getAsCompoundTag();
        }

        if (tag.isPresent()) {
            Optional<CompoundTag> dataTag = tag.get().getAsCompoundTag("Data");
            if (dataTag.isPresent()) {
                // Data version
                int dataVersion = dataTag.get().getIntValue("DataVersion").orElse(-1);

                int spawnX = dataTag.get().getIntValue("SpawnX").orElse(0);
                int spawnY = dataTag.get().getIntValue("SpawnY").orElse(255);
                int spawnZ = dataTag.get().getIntValue("SpawnZ").orElse(0);

                return new LevelData(dataVersion, spawnX, spawnY, spawnZ);
            }
        }

        throw new InvalidWorldException(file.getParent());
    }

    private static void loadEntities(Path file, int version, Long2ObjectMap<SlimeChunk> chunkMap) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(file);
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
                NBTInputStream nbtStream = new NBTInputStream(decompressorStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
                CompoundTag globalCompound = (CompoundTag) nbtStream.readTag();
                CompoundMap globalMap = globalCompound.getValue();

                readEntityChunk(new CompoundTag("entityChunk", globalMap), version, chunkMap);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    private static List<SlimeChunk> loadChunks(Path file, int worldVersion) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(file);
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
                NBTInputStream nbtStream = new NBTInputStream(decompressorStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
                CompoundTag globalCompound = (CompoundTag) nbtStream.readTag();
                CompoundMap globalMap = globalCompound.getValue();

                CompoundTag levelDataTag = new CompoundTag("Level", globalMap);
                if (globalMap.containsKey("Level"))
                    levelDataTag = (CompoundTag) globalMap.get("Level");

                return readChunk(levelDataTag, worldVersion);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static void readEntityChunk(CompoundTag compound, int worldVersion, Long2ObjectMap<SlimeChunk> slimeChunkMap) {
        int[] position = compound.getAsIntArrayTag("Position").orElseThrow().getValue();
        int chunkX = position[0];
        int chunkZ = position[1];

        int dataVersion = compound.getAsIntTag("DataVersion").map(IntTag::getValue).orElse(-1);
        if (dataVersion != worldVersion) {
            System.err.println("Cannot load entity chunk at " + chunkX + "," + chunkZ + ": data version " + dataVersion + " does not match world version " + worldVersion);
            return;
        }

        SlimeChunk chunk = slimeChunkMap.get(Util.chunkPosition(chunkX, chunkZ));
        if (chunk == null) {
            System.out.println("Lost entity chunk data at: " + chunkX + " " + chunkZ);
        } else {
            chunk.getEntities().addAll((List<CompoundTag>) compound.getAsListTag("Entities").orElseThrow().getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static SlimeChunk readChunk(CompoundTag compound, int worldVersion) {
        int chunkX = compound.getAsIntTag("xPos").orElseThrow().getValue();
        int chunkZ = compound.getAsIntTag("zPos").orElseThrow().getValue();

        if (worldVersion >= V1_19_2) { // 1.18 chunks should have a DataVersion tag, we can check if the chunk has been converted to match the world
            int dataVersion = compound.getAsIntTag("DataVersion").map(IntTag::getValue).orElse(-1);
            if (dataVersion != worldVersion) {
                System.err.printf("Cannot load chunk at (%d,%d): data version %d does not match world version %d%n", chunkX, chunkZ, dataVersion, worldVersion);
                return null;
            }
        }

        Optional<String> status = compound.getStringValue("Status");
        if (status.isPresent() && !status.get().equals("postprocessed") && !status.get().startsWith("full") && !status.get().startsWith("minecraft:full")) {
            // It's a protochunk
            return null;
        }

//        int[] biomes;
//        Tag biomesTag = compound.getValue().get("Biomes");
//
//        if (biomesTag instanceof IntArrayTag) {
//            biomes = ((IntArrayTag) biomesTag).getValue();
//        } else if (biomesTag instanceof ByteArrayTag) {
//            byte[] byteBiomes = ((ByteArrayTag) biomesTag).getValue();
//            biomes = toIntArray(byteBiomes);
//        } else {
//            biomes = null;
//        }

        Optional<CompoundTag> optionalHeightMaps = compound.getAsCompoundTag("Heightmaps");
        CompoundTag heightMapsCompound = optionalHeightMaps.orElse(new CompoundTag("", new CompoundMap()));

        List<CompoundTag> tileEntities;
        List<CompoundTag> entities;
        ListTag<CompoundTag> sectionsTag;

        int minSectionY = 0;
        int maxSectionY = 16;

        if (worldVersion < V1_19_2) {
            tileEntities = ((ListTag<CompoundTag>) compound.getAsListTag("TileEntities").orElse(new ListTag<>("TileEntities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            entities = ((ListTag<CompoundTag>) compound.getAsListTag("Entities").orElse(new ListTag<>("Entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            sectionsTag = (ListTag<CompoundTag>) compound.getAsListTag("Sections").orElseThrow();
        } else {
            tileEntities = ((ListTag<CompoundTag>) compound.getAsListTag("block_entities").orElse(new ListTag<>("block_entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            entities = ((ListTag<CompoundTag>) compound.getAsListTag("entities").orElse(new ListTag<>("entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            sectionsTag = (ListTag<CompoundTag>) compound.getAsListTag("sections").orElseThrow();

            Class<?> type = compound.getValue().get("yPos").getValue().getClass();
            if (type == Byte.class) {
                minSectionY = compound.getByteValue("yPos").orElseThrow();
            } else {
                minSectionY = compound.getIntValue("yPos").orElseThrow();
            }

            maxSectionY = sectionsTag.getValue().stream().map(c -> c.getByteValue("Y").orElseThrow()).max(Byte::compareTo).orElse((byte) 0) + 1; // Add 1 to the section, as we serialize it with the 1 added.
        }

        SlimeChunkSection[] sectionArray = new SlimeChunkSection[maxSectionY - minSectionY];
        for (CompoundTag sectionTag : sectionsTag.getValue()) {
            int index = sectionTag.getByteValue("Y").orElseThrow();
            if (worldVersion < V1_17_1 && index < 0)
                // For some reason MC 1.14 worlds contain an empty section with Y = -1, however 1.17+ worlds can use these sections
                continue;

            ListTag<CompoundTag> paletteTag;
            long[] blockStatesArray;

            CompoundTag blockStatesTag = null;
            CompoundTag biomeTag = null;
            if (worldVersion < V1_19_2) {
                paletteTag = (ListTag<CompoundTag>) sectionTag.getAsListTag("Palette").orElse(null);
                blockStatesArray = sectionTag.getLongArrayValue("BlockStates").orElse(null);
                if (paletteTag == null || blockStatesArray == null || isEmpty(blockStatesArray)) { // Skip it
                    continue;
                }
            } else {
                if (sectionTag.getAsCompoundTag("block_states").isEmpty() && sectionTag.getAsCompoundTag("biomes").isEmpty())
                    continue; // empty section

                blockStatesTag = sectionTag.getAsCompoundTag("block_states").orElseThrow();
                biomeTag = sectionTag.getAsCompoundTag("biomes").orElseThrow();
            }

            NibbleArray blockLightArray = sectionTag.getByteArrayValue("BlockLight").map(NibbleArray::new).orElse(null);
            NibbleArray skyLightArray = sectionTag.getByteArrayValue("SkyLight").map(NibbleArray::new).orElse(null);

            // There is no need to do any custom processing here.
            sectionArray[index - minSectionY] = new SlimeChunkSectionSkeleton(/*paletteTag, blockStatesArray,*/ blockStatesTag, biomeTag, blockLightArray, skyLightArray);
        }

        CompoundTag extraTag = new CompoundTag("", new CompoundMap());
        compound.getAsCompoundTag("ChunkBukkitValues").ifPresent(chunkBukkitValues -> extraTag.getValue().put(chunkBukkitValues));

        for (SlimeChunkSection section : sectionArray)
            if (section != null) // Chunk isn't empty
                return new SlimeChunkSkeleton(chunkX, chunkZ, sectionArray, heightMapsCompound, tileEntities, entities, extraTag, null);

        // Chunk is empty
        return null;
    }

    private static int[] toIntArray(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        int[] ret = new int[buf.length / 4];
        buffer.asIntBuffer().get(ret);
        return ret;
    }

    private static boolean isEmpty(byte[] array) {
        for (byte b : array)
            if (b != 0)
                return false;

        return true;
    }

    private static boolean isEmpty(long[] array) {
        for (long b : array)
            if (b != 0L)
                return false;

        return true;
    }


    private record ChunkEntry(int offset, int paddedSize) { }

    private record LevelData(int version, int x, int y, int z) { }

}
