package com.infernalsuite.aswm.serialization.anvil;

import com.infernalsuite.aswm.api.SlimeInternalsBridge;
import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import com.infernalsuite.aswm.api.exceptions.MismatchedWorldVersionException;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.SlimeWorldReader;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class AnvilWorldReader implements SlimeWorldReader<AnvilImportData> {

    private static final int CURRENT_WORLD_VERSION = SlimeInternalsBridge.get().getCurrentVersion();

    public static final AnvilWorldReader INSTANCE = new AnvilWorldReader();

    @Override
    public @NotNull SlimeWorld readFromData(AnvilImportData importData) {
        Path worldDir = importData.worldDir();

        try {
            Path levelFile = worldDir.resolve("level.dat");
            if (!Files.isRegularFile(levelFile))
                throw new InvalidWorldException(worldDir);

            LevelData data = readLevelData(levelFile);
            int worldVersion = data.version();
            if (worldVersion != CURRENT_WORLD_VERSION)
                throw new MismatchedWorldVersionException(worldVersion, CURRENT_WORLD_VERSION);

            SlimePropertyMap properties = new SlimePropertyMap();
            Path environmentDir = resolveEnvironmentDir(worldDir, data, properties);

            Long2ObjectMap<SlimeChunk> chunks = AnvilWorldChunkReader.loadWorldChunks(environmentDir, worldVersion);
            if (chunks.isEmpty())
                throw new InvalidWorldException(environmentDir);

//            List<CompoundBinaryTag> worldMaps = AnvilWorldMapReader.loadWorldMaps(environmentDir);

            properties.setValue(SlimeProperties.SPAWN_X, data.spawnX);
            properties.setValue(SlimeProperties.SPAWN_Y, data.spawnY);
            properties.setValue(SlimeProperties.SPAWN_Z, data.spawnZ);

            CompoundBinaryTag.Builder extraDataBuilder = CompoundBinaryTag.builder();
            if (!data.gameRules().isEmpty()) {
                CompoundBinaryTag.Builder gameRulesBuilder = CompoundBinaryTag.builder();
                data.gameRules().forEach((rule, value) -> gameRulesBuilder.put(rule, StringBinaryTag.of(value)));
                extraDataBuilder.put("gamerules", gameRulesBuilder.build());
            }

            return new SkeletonSlimeWorld(
                    importData.newName(),
                    importData.loader(),
                    properties,
                    chunks,
                    true,
                    worldVersion,
                    extraDataBuilder.build()
            );
        } catch (IOException | InvalidWorldException | MismatchedWorldVersionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static LevelData readLevelData(Path file) throws IOException, InvalidWorldException {
        CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP);
        CompoundBinaryTag dataTag = tag.getCompound("Data");
        if (dataTag.keySet().isEmpty())
            throw new InvalidWorldException(file.getParent());

        int dataVersion = dataTag.getInt("DataVersion", -1);

        CompoundBinaryTag dimensionsTag = dataTag.getCompound("WorldGenSettings").getCompound("dimensions");
        boolean multiDimensional = dimensionsTag.keySet().size() > 1;

        Map<String, String> gameRules = new HashMap<>();
        CompoundBinaryTag gameRulesTag = dataTag.getCompound("GameRules");
        if (!gameRulesTag.keySet().isEmpty())
            gameRulesTag.forEach(entry -> {
                if (entry.getValue() instanceof StringBinaryTag cast) {
                    gameRules.put(entry.getKey(), cast.value());
                }
            });

        int spawnX = dataTag.getInt("SpawnX", 0);
        int spawnY = dataTag.getInt("SpawnY", 255);
        int spawnZ = dataTag.getInt("SpawnZ", 0);

        return new LevelData(dataVersion, multiDimensional, gameRules, spawnX, spawnY, spawnZ);
    }

    private static @NotNull Path resolveEnvironmentDir(Path worldDir, LevelData data, SlimePropertyMap properties) {
        Path netherDim = worldDir.resolve("DIM-1");
        Path theEndDim = worldDir.resolve("DIM1");

        if (!data.multiDimensional() || !Files.isDirectory(netherDim) || !Files.isDirectory(theEndDim)) {
            if (Files.isDirectory(netherDim.resolve("region"))) {
                properties.setValue(SlimeProperties.ENVIRONMENT, "nether");
                return netherDim;
            }

            if (Files.isDirectory(theEndDim.resolve("region"))) {
                properties.setValue(SlimeProperties.ENVIRONMENT, "the_end");
                return theEndDim;
            }
        }

        properties.setValue(SlimeProperties.ENVIRONMENT, "normal");
        return worldDir;
    }

    private record LevelData(
            int version,
            boolean multiDimensional,
            Map<String, String> gameRules,
            int spawnX,
            int spawnY,
            int spawnZ
    ) { }

}
