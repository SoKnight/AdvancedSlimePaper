package com.infernalsuite.aswm.importer;

import com.infernalsuite.aswm.serialization.anvil.AnvilImportData;
import com.infernalsuite.aswm.serialization.anvil.AnvilWorldReader;
import com.infernalsuite.aswm.serialization.slime.SlimeSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public final class SWMImporter {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar aswm-importer.jar <path-to-world-folder> [--accept] [--print-error]");
            return;
        }

        Path worldDir = Paths.get(args[0]).toAbsolutePath();
        Path outputFile = getDestinationFile(worldDir);

        List<String> argList = Arrays.asList(args);
        boolean hasAccepted = argList.contains("--accept");
        boolean printErrors = argList.contains("--print-error");

        if (!hasAccepted) {
            System.out.println("**** WARNING ****");
            System.out.println("The Slime Format is meant to be used on tiny maps, not big survival worlds.");
            System.out.println("It is recommended to trim your world by using the Prune MCEdit tool to ensure you don't save more chunks than you want to.");
            System.out.println();
            System.out.println("NOTE: This utility will automatically ignore every chunk that doesn't contain any blocks.");
            System.out.print("Do you want to continue? [Y/N]: ");

            Scanner scanner = new Scanner(System.in);
            String response = scanner.next();
            if (!"Y".equalsIgnoreCase(response)) {
                System.out.println("Your wish is my command.");
                return;
            }
        }

        importWorld(worldDir, outputFile, printErrors);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void importWorld(Path worldDir, Path outputFile, boolean shouldPrintDebug) {
        try {
            if (!Files.isDirectory(outputFile.getParent()))
                Files.createDirectories(outputFile.getParent());

            AnvilImportData importData = new AnvilImportData(worldDir, outputFile.getFileName().toString(), null);
            byte[] serializedData = SlimeSerializer.serialize(AnvilWorldReader.INSTANCE.readFromData(importData));
            Files.write(outputFile, serializedData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IndexOutOfBoundsException ex) {
            System.err.println("Oops, it looks like the world provided is too big to be imported. Please trim it by using the MCEdit tool and try again.");
        } catch (IOException ex) {
            System.err.println("Failed to save the world file.");
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            if (shouldPrintDebug) {
                ex.printStackTrace();
            } else {
                System.err.println(ex.getMessage());
            }
        }
    }

    /**
     * Returns a destination file at which the slime file will
     * be placed when run as an executable.
     * <p>
     * This method may be used by your plugin to output slime
     * files identical to the executable.
     *
     * @param worldFolder The world directory to import
     * @return The output file destination
     */
    public static Path getDestinationFile(Path worldFolder) {
        return worldFolder.getParent().resolve(worldFolder.getFileName().toString() + ".slime");
    }

}
