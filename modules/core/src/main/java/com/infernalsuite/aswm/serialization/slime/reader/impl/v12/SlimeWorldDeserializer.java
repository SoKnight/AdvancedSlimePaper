package com.infernalsuite.aswm.serialization.slime.reader.impl.v12;

import com.github.luben.zstd.Zstd;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.slime.reader.SlimeWorldReader;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class SlimeWorldDeserializer implements SlimeWorldReader<SlimeWorld> {

    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    public static final int NIBBLE_ARRAY_SIZE = 2048;

    @Override
    public SlimeWorld deserializeWorld(
            byte version,
            @Nullable SlimeLoader loader,
            String worldName,
            DataInputStream dataStream,
            SlimePropertyMap propertyMap,
            boolean readOnly
    ) throws IOException {
        int worldVersion = dataStream.readInt();

        byte[] chunkBytes = readCompressed(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, chunkBytes);

        byte[] extraTagBytes = readCompressed(dataStream);
        CompoundBinaryTag extraData = readCompound(extraTagBytes);
        if (extraData == null)
            extraData = CompoundBinaryTag.empty();

        SlimePropertyMap properties = propertyMap;
        CompoundBinaryTag propertiesTag = extraData.getCompound("properties");
        if (!propertiesTag.keySet().isEmpty()) {
            properties = SlimePropertyMap.fromCompound(propertiesTag);
            properties.merge(propertyMap);
        }

//        List<CompoundBinaryTag> worldMaps = extraData.getList("worldMaps", BinaryTagTypes.COMPOUND).stream()
//                .map(t -> (CompoundBinaryTag) t)
//                .collect(Collectors.toCollection(ArrayList::new));

        return new SkeletonSlimeWorld(worldName, loader, properties, chunks, readOnly, worldVersion, extraData);
    }

    private static @NotNull Long2ObjectMap<SlimeChunk> readChunks(SlimePropertyMap slimePropertyMap, byte[] chunkBytes) throws IOException {
        Long2ObjectMap<SlimeChunk> chunkMap = new Long2ObjectOpenHashMap<>();
        DataInputStream chunkData = new DataInputStream(new ByteArrayInputStream(chunkBytes));

        int chunkAmount = chunkData.readInt();
        for (int i = 0; i < chunkAmount; i++) {
            int chunkX = chunkData.readInt();
            int chunkZ = chunkData.readInt();

            // Chunk Sections
            SlimeChunkSection[] chunkSections = readChunkSections(chunkData);

            // HeightMaps
            byte[] heightMapData = new byte[chunkData.readInt()];
            chunkData.read(heightMapData);
            CompoundBinaryTag heightMaps = readCompound(heightMapData);

            // Biomes & Tile Entities & Entities & Extra Data
            int[] biomes = readChunkBiomes(chunkData);
            List<CompoundBinaryTag> tileEntities = readChunkCompoundTag(chunkData, "tileEntities");
            List<CompoundBinaryTag> entities = readChunkCompoundTag(chunkData, "entities");
            CompoundBinaryTag extraData = readChunkExtraData(chunkData);

            SlimeChunkSkeleton chunk = new SlimeChunkSkeleton(
                    chunkX, chunkZ,
                    chunkSections,
                    heightMaps,
                    biomes,
                    tileEntities,
                    entities,
                    extraData
            );

            chunkMap.put(SlimeWorld.chunkPosition(chunkX, chunkZ), chunk);
        }

        return chunkMap;
    }

    private static @NotNull SlimeChunkSection[] readChunkSections(DataInputStream chunkData) throws IOException {
        byte[] sectionBitmask = new byte[2];
        chunkData.read(sectionBitmask);
        BitSet sectionBitset = BitSet.valueOf(sectionBitmask);

        SlimeChunkSection[] chunkSections = new SlimeChunkSection[16];
        for (int sectionId = 0; sectionId < 16; sectionId++) {
            if (!sectionBitset.get(sectionId))
                continue;

            // Block/Sky Light Nibble Array
            NibbleArray blockLightArray = readChunkSectionNibbleArray(chunkData);
            NibbleArray skyLightArray = readChunkSectionNibbleArray(chunkData);

            // Block Palette
            int blockPaletteLength = chunkData.readInt();
            List<CompoundBinaryTag> blockPaletteTags = new ArrayList<>(blockPaletteLength);
            for (int index = 0; index < blockPaletteLength; index++) {
                int serializedDataLength = chunkData.readInt();
                byte[] serializedData = new byte[serializedDataLength];
                chunkData.read(serializedData);
                CompoundBinaryTag element = readCompound(serializedData);
                if (element != null) {
                    blockPaletteTags.add(element);
                }
            }
            ListBinaryTag blockPalette = ListBinaryTag.from(blockPaletteTags);

            // Block states
            int blockStatesLength = chunkData.readInt();
            long[] blockStates = blockStatesLength > 0 ? new long[blockStatesLength] : EMPTY_LONG_ARRAY;
            for (int i = 0; i < blockStatesLength; i++)
                blockStates[i] = chunkData.readLong();

            chunkSections[sectionId] = new SlimeChunkSectionSkeleton(blockPalette, blockStates, blockLightArray, skyLightArray);
        }

        return chunkSections;
    }

    private static @Nullable NibbleArray readChunkSectionNibbleArray(DataInputStream chunkData) throws IOException {
        if (!chunkData.readBoolean())
            return null;

        byte[] backing = new byte[NIBBLE_ARRAY_SIZE];
        chunkData.read(backing);
        return new NibbleArray(backing);
    }

    private static int[] readChunkBiomes(DataInputStream chunkData) throws IOException {
        int biomesSize = chunkData.readInt();
        if (biomesSize == 0)
            return null;

        int[] biomes = new int[biomesSize];
        for (int i = 0; i < biomesSize; i++)
            biomes[i] = chunkData.readInt();

        return biomes;
    }

    private static @NotNull List<CompoundBinaryTag> readChunkCompoundTag(DataInputStream chunkData, String tagName) throws IOException {
        byte[] serializedData = read(chunkData);

        CompoundBinaryTag compoundTag = readCompound(serializedData);
        if (compoundTag == null)
            return Collections.emptyList();

        return compoundTag.getList(tagName, BinaryTagTypes.COMPOUND).stream()
                .map(tag -> (CompoundBinaryTag) tag)
                .toList();
    }

    private static @NotNull CompoundBinaryTag readChunkExtraData(DataInputStream chunkData) throws IOException {
        byte[] serializedData = read(chunkData);
        CompoundBinaryTag extraData = readCompound(serializedData);
        return extraData != null ? extraData : CompoundBinaryTag.empty();
    }

    private static byte[] readCompressed(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        int decompressedLength = stream.readInt();
        byte[] compressedData = new byte[compressedLength];
        byte[] decompressedData = new byte[decompressedLength];

        stream.read(compressedData);
        Zstd.decompress(decompressedData, compressedData);
        return decompressedData;
    }

    private static byte[] read(DataInputStream stream) throws IOException {
        int length = stream.readInt();
        byte[] data = new byte[length];
        stream.read(data);
        return data;
    }

    private static CompoundBinaryTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0)
            return null;

        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(tagBytes));
    }

}
