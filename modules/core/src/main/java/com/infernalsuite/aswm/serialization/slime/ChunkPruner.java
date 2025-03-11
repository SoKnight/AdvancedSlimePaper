package com.infernalsuite.aswm.serialization.slime;

import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChunkPruner {

    public static boolean canBePruned(SlimeWorld world, SlimeChunk chunk) {
        SlimePropertyMap propertyMap = world.getProperties();
        if (propertyMap.getValue(SlimeProperties.SHOULD_LIMIT_SAVE)) {
            int minX = propertyMap.getValue(SlimeProperties.SAVE_MIN_X);
            int maxX = propertyMap.getValue(SlimeProperties.SAVE_MAX_X);

            int minZ = propertyMap.getValue(SlimeProperties.SAVE_MIN_Z);
            int maxZ = propertyMap.getValue(SlimeProperties.SAVE_MAX_Z);

            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();

            if (chunkX < minX || chunkX > maxX)
                return true;

            if (chunkZ < minZ || chunkZ > maxZ) {
                return true;
            }
        }

        String pruningSetting = world.getProperties().getValue(SlimeProperties.CHUNK_PRUNING);
        if ("aggressive".equalsIgnoreCase(pruningSetting))
            return chunk.getTileEntities().isEmpty() && chunk.getEntities().isEmpty() && areSectionsEmpty(chunk.getSections());

        return false;
    }

    private static boolean areSectionsEmpty(SlimeChunkSection[] sections) {
        for (SlimeChunkSection chunkSection : sections) {
            try {
                ListBinaryTag blockPalette = chunkSection.getBlockPalette();
                if (blockPalette.elementType() != BinaryTagTypes.COMPOUND)
                    continue; // If the element type isn't a compound tag, consider the section empty

                if (blockPalette.size() > 1)
                    return false; // If there is more than one palette, the section is not empty

                CompoundBinaryTag firstItem = blockPalette.getCompound(0);
                if ("minecraft:air".equals(firstItem.getString("Name", "")))
                    return false; // If the only palette entry is not air, the section is not empty
            } catch (final Exception e) {
                return false;
            }
            // The section is empty, continue to the next one
        }
        // All sections are empty, we can omit this chunk
        return true;
    }

}
