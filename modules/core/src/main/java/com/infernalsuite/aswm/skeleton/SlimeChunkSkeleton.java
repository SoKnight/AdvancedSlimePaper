package com.infernalsuite.aswm.skeleton;

import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Getter
public final class SlimeChunkSkeleton implements SlimeChunk {

    public static final SlimeChunkSection[] EMPTY_SECTIONS_ARRAY = {};

    private final int x;
    private final int z;
    private final SlimeChunkSection[] sections;
    private final CompoundBinaryTag heightMaps;
    private final int[] biomes;
    private final List<CompoundBinaryTag> tileEntities;
    private final List<CompoundBinaryTag> entities;
    private @NotNull CompoundBinaryTag extraData;

    public SlimeChunkSkeleton(
            int x, int z,
            SlimeChunkSection[] sections,
            CompoundBinaryTag heightMaps,
            int[] biomes,
            List<CompoundBinaryTag> tileEntities,
            List<CompoundBinaryTag> entities,
            @NotNull CompoundBinaryTag extraData
    ) {
        this.x = x;
        this.z = z;
        this.sections = sections;
        this.heightMaps = heightMaps;
        this.biomes = biomes;
        this.tileEntities = tileEntities;
        this.entities = entities;
        putExtraData(extraData);
    }

    @Override
    public @NotNull SlimeChunkSection[] getSections() {
        return sections != null ? sections : EMPTY_SECTIONS_ARRAY;
    }

    @Override
    public @NotNull CompoundBinaryTag getHeightMaps() {
        return heightMaps != null ? heightMaps : CompoundBinaryTag.empty();
    }

    @Override
    public @NotNull @Unmodifiable List<CompoundBinaryTag> getTileEntities() {
        return tileEntities != null ? Collections.unmodifiableList(tileEntities) : Collections.emptyList();
    }

    @Override
    public @NotNull @Unmodifiable List<CompoundBinaryTag> getEntities() {
        return entities != null ? Collections.unmodifiableList(entities) : Collections.emptyList();
    }

    @Override
    public @NotNull CompoundBinaryTag putExtraData(@Nullable CompoundBinaryTag extraData) {
        this.extraData = extraData != null ? extraData : CompoundBinaryTag.empty();
        return this.extraData;
    }

    @Override
    public @NotNull CompoundBinaryTag updateExtraData(@NotNull Consumer<CompoundBinaryTag.Builder> compoundCustomizer) {
        Objects.requireNonNull(compoundCustomizer, "compoundCustomizer cannot be null");
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

        if (extraData != null)
            builder.put(extraData);

        compoundCustomizer.accept(builder);
        this.extraData = builder.build();
        return extraData;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SlimeChunkSkeleton that = (SlimeChunkSkeleton) o;
        return x == that.x && z == that.z
                && Objects.deepEquals(sections, that.sections)
                && Objects.equals(heightMaps, that.heightMaps)
                && Objects.deepEquals(biomes, that.biomes)
                && Objects.equals(tileEntities, that.tileEntities)
                && Objects.equals(entities, that.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, Arrays.hashCode(sections), heightMaps, Arrays.hashCode(biomes), tileEntities, entities);
    }

    @Override
    public String toString() {
        return "SlimeChunkSkeleton{" +
                "biomes=" + Arrays.toString(biomes) +
                ", x=" + x +
                ", z=" + z +
                ", sections=" + Arrays.toString(sections) +
                ", heightMaps=" + heightMaps +
                ", tileEntities=" + tileEntities +
                ", entities=" + entities +
                ", extraData=" + extraData +
                '}';
    }

}
