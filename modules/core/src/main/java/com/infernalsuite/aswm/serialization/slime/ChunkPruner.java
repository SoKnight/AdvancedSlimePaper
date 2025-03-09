package com.infernalsuite.aswm.serialization.slime;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChunkPruner {

    public static boolean canBePruned(SlimeWorld world, SlimeChunk chunk) {
        SlimePropertyMap propertyMap = world.getPropertyMap();
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

        String pruningSetting = world.getPropertyMap().getValue(SlimeProperties.CHUNK_PRUNING);
        if (pruningSetting.equals("aggressive"))
            return chunk.getTileEntities().isEmpty() && chunk.getEntities().isEmpty() && areSectionsEmpty(chunk.getSections());

        return false;
    }

    private static boolean areSectionsEmpty(SlimeChunkSection[] sections) {
        for (SlimeChunkSection chunkSection : sections) {
            try {
                List<CompoundTag> palettes = chunkSection.getBlockStatesTag().getAsListTag("palette")
                        .orElseThrow().getAsCompoundTagList()
                        .orElseThrow().getValue();

                if (palettes.size() > 1)
                    return false; // If there is more than one palette, the section is not empty

                if (!palettes.get(0).getStringValue("Name").orElseThrow().equals("minecraft:air")) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

            // The section is empty, continue to the next one
        }

        // All sections are empty, we can omit this chunk
        return true;
    }

}
