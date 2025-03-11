package com.infernalsuite.aswm.serialization.anvil;

import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AnvilWorldMapReader {

    private static final Pattern MAP_FILE_PATTERN = Pattern.compile("^map_([0-9]+)\\.dat$");

    static @NotNull List<CompoundBinaryTag> loadWorldMaps(@NotNull Path environmentDir) throws InvalidWorldException, IOException {
        List<CompoundBinaryTag> maps = new ArrayList<>();

        Path dataDir = environmentDir.resolve("data");
        if (!Files.exists(dataDir))
            return maps;

        if (!Files.isDirectory(dataDir))
            throw new InvalidWorldException(environmentDir);

        try (var stream = Files.newDirectoryStream(dataDir, AnvilWorldMapReader::isMapFile)) {
            for (Path path : stream) {
                log.info("Loading map file '{}'...", path.getFileName());
                CompoundBinaryTag map = loadMap(path);
                if (map != null) {
                    maps.add(map);
                }
            }
        }

        return maps;
    }

    private static CompoundBinaryTag loadMap(Path mapFile) throws IOException {
        Matcher matcher = MAP_FILE_PATTERN.matcher(mapFile.getFileName().toString());
        boolean matches = matcher.matches();
        assert matches;

        int mapId = Integer.parseInt(matcher.group(1));
        CompoundBinaryTag globalTag = BinaryTagIO.unlimitedReader().read(mapFile);
        CompoundBinaryTag dataTag = globalTag.getCompound("data");
        return dataTag.put("id", IntBinaryTag.intBinaryTag(mapId));
    }

    private static boolean isMapFile(Path filePath) {
        if (!Files.isRegularFile(filePath))
            return false;

        return MAP_FILE_PATTERN.matcher(filePath.getFileName().toString()).matches();
    }

}
