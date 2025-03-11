package com.infernalsuite.aswm.serialization.slime;

import com.github.luben.zstd.Zstd;
import com.infernalsuite.aswm.api.utils.SlimeFormat;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.nbt.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlimeSerializer {

    public static byte[] serialize(SlimeWorld world) {
        CompoundBinaryTag extraData = world.getExtraData();

        // Store world properties
        SlimePropertyMap properties = world.getProperties();
        CompoundBinaryTag propertiesTag = extraData.getCompound("properties").put(properties.toCompound());
        extraData = propertiesTag.size() != 0
                ? extraData.put("properties", propertiesTag)
                : extraData.remove("properties");

        // Store world maps
        List<CompoundBinaryTag> worldMaps = world.getWorldMaps();
        ListBinaryTag worldMapsTag = ListBinaryTag.from(worldMaps);
        extraData = worldMapsTag.size() != 0
                ? extraData.put("worldMaps", worldMapsTag)
                : extraData.remove("worldMaps");

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {
            // File Header and Slime version
            outStream.write(SlimeFormat.SLIME_HEADER);
            outStream.writeByte(SlimeFormat.SLIME_VERSION);

            // World version
            outStream.writeInt(world.getDataVersion());

            // Chunks
            byte[] chunkData = serializeChunks(world, world.getChunkStorage());
            byte[] compressedChunkData = Zstd.compress(chunkData);
            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);
            
            // Extra Tag
            byte[] extra = serializeCompoundTag(extraData);
            byte[] compressedExtra = Zstd.compress(extra);
            outStream.writeInt(compressedExtra.length);
            outStream.writeInt(extra.length);
            outStream.write(compressedExtra);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outByteStream.toByteArray();
    }

    static byte[] serializeChunks(SlimeWorld world, Collection<SlimeChunk> chunks) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        // Prune chunks
        List<SlimeChunk> chunksToSave = chunks.stream()
                .filter(chunk -> !ChunkPruner.canBePruned(world, chunk))
                .toList();

        outStream.writeInt(chunksToSave.size());
        for (SlimeChunk chunk : chunksToSave) {
            outStream.writeInt(chunk.getX());
            outStream.writeInt(chunk.getZ());

            // Chunk Sections
            serializeChunkSections(chunk, outStream);

            // Height Maps
            byte[] heightMaps = serializeCompoundTag(chunk.getHeightMaps());
            outStream.writeInt(heightMaps.length);
            outStream.write(heightMaps);

            // Biomes & Tile Entities & Entities & Extra Data
            serializeChunkBiomes(chunk, outStream);
            serializeChunkCompoundTag(chunk.getTileEntities(), "tileEntities", outStream);
            serializeChunkCompoundTag(chunk.getEntities(), "entities", outStream);
            serializeChunkExtraData(world, chunk, outStream);
        }

        return outByteStream.toByteArray();
    }

    private static void serializeChunkSections(SlimeChunk chunk, DataOutputStream outStream) throws IOException {
        SlimeChunkSection[] sections = Arrays.stream(chunk.getSections())
                .filter(Objects::nonNull)
                .toArray(SlimeChunkSection[]::new);

        outStream.writeInt(sections.length);
        for (SlimeChunkSection section : sections) {
            // Block Light
            boolean hasBlockLight = section.getBlockLight() != null;
            outStream.writeBoolean(hasBlockLight);
            if (hasBlockLight)
                outStream.write(section.getBlockLight().getBacking());

            // Sky Light
            boolean hasSkyLight = section.getSkyLight() != null;
            outStream.writeBoolean(hasSkyLight);
            if (hasSkyLight)
                outStream.write(section.getSkyLight().getBacking());

            // Block Palette
            serializeChunkSectionBlockPalette(outStream, section);

            // Block States
            long[] blockStates = section.getBlockStates();
            if (blockStates.length != 0) {
                outStream.writeInt(blockStates.length);
                outStream.write(serializeLongArray(blockStates));
            } else {
                outStream.writeInt(0);
            }
        }
    }

    private static void serializeChunkBiomes(SlimeChunk chunk, DataOutputStream outStream) throws IOException {
        int[] biomes = chunk.getBiomes();
        if (biomes != null) {
            outStream.writeInt(biomes.length);
            outStream.write(serializeIntArray(biomes));
        } else {
            outStream.writeInt(0);
        }
    }

    private static void serializeChunkCompoundTag(List<CompoundBinaryTag> chunk, String tagName, DataOutputStream outStream) throws IOException {
        ListBinaryTag nbtList = ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(chunk));
        CompoundBinaryTag tag = CompoundBinaryTag.builder().put(tagName, nbtList).build();
        byte[] serializedData = serializeCompoundTag(tag);
        outStream.writeInt(serializedData.length);
        outStream.write(serializedData);
    }

    private static void serializeChunkSectionBlockPalette(DataOutputStream outStream, SlimeChunkSection section) throws IOException {
        ListBinaryTag blockPalette = section.getBlockPalette();
        if (blockPalette.size() != 0) {
            outStream.writeInt(blockPalette.size());
            for (BinaryTag tag : blockPalette) {
                if (tag instanceof CompoundBinaryTag cast) {
                    byte[] serializedData = serializeCompoundTag(cast);
                    outStream.writeInt(serializedData.length);
                    outStream.write(serializedData);
                } else {
                    throw new RuntimeException("Unexpected block palette child tag type: " + tag.type());
                }
            }
        } else {
            outStream.writeInt(0);
        }
    }

    private static void serializeChunkExtraData(SlimeWorld world, SlimeChunk chunk, DataOutputStream outStream) throws IOException {
        if (chunk.getExtraData() == null)
            log.warn(
                    "Chunk at ({},{}) from world '{}' has no extraData! When deserialized, this chunk will have an empty extraData tag!",
                    chunk.getX(), chunk.getZ(), world.getName()
            );

        byte[] extra = serializeCompoundTag(chunk.getExtraData());
        outStream.writeInt(extra.length);
        outStream.write(extra);
    }

    private static byte[] serializeCompoundTag(CompoundBinaryTag tag) throws IOException {
        if (tag == null || tag.size() == 0)
            return new byte[0];

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(tag, outByteStream);
        return outByteStream.toByteArray();
    }

    private static byte[] serializeIntArray(int[] array) {
        if (array == null || array.length == 0)
            return new byte[0];

        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * (Integer.SIZE / Byte.SIZE));
        byteBuffer.asIntBuffer().put(array);
        return byteBuffer.array();
    }

    private static byte[] serializeLongArray(long[] array) {
        if (array == null || array.length == 0)
            return new byte[0];

        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * (Long.SIZE / Byte.SIZE));
        byteBuffer.asLongBuffer().put(array);
        return byteBuffer.array();
    }

    @SuppressWarnings("unchecked")
    private static List<BinaryTag> yayGenerics(final List<? extends BinaryTag> tags) {
        return (List<BinaryTag>) tags;
    }

}
