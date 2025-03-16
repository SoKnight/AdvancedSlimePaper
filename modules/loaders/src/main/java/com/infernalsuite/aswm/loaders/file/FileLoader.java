package com.infernalsuite.aswm.loaders.file;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class FileLoader implements SlimeLoader {

    private final Path worldDir;

    public FileLoader(Path worldDir) throws IllegalStateException, IOException {
        this.worldDir = worldDir;

        if (Files.exists(worldDir) && !Files.isDirectory(worldDir)) {
            log.warn("A file named '{}' has been deleted, as this is the name used for the worlds directory.", worldDir.getFileName());
            Files.delete(worldDir);
        }

        if (!Files.isDirectory(worldDir)) {
            Files.createDirectories(worldDir);
        }
    }

    @Override
    public byte[] readWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName))
            throw new UnknownWorldException(worldName);

        return Files.readAllBytes(worldDir.resolve(worldName + ".slime"));
    }

    @Override
    public boolean worldExists(String worldName) {
        return Files.isRegularFile(worldDir.resolve(worldName + ".slime"));
    }

    @Override
    public List<String> listWorlds() throws NotDirectoryException {
        try (Stream<Path> paths = Files.list(worldDir)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".slime"))
                    .map(name -> name.substring(0, name.length() - 6))
                    .toList();
        } catch (IOException ignored) {
            throw new NotDirectoryException(worldDir.toString());
        }
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        if (!Files.isDirectory(worldDir))
            Files.createDirectories(worldDir);

        Files.write(
                worldDir.resolve(worldName + ".slime"),
                serializedWorld,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        if (!Files.deleteIfExists(worldDir.resolve(worldName + ".slime"))) {
            throw new UnknownWorldException(worldName);
        }
    }

}
