package com.infernalsuite.aswm.serialization.slime.reader;

import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.utils.SlimeFormat;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v12.WorldFormatV12;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SlimeWorldReaderRegistry {

    private static final Map<Byte, SlimeWorldReader<SlimeWorld>> FORMATS = new HashMap<>();

    static {
        register(WorldFormatV12.FORMAT, 12);
    }

    @SuppressWarnings("SameParameterValue")
    private static void register(SlimeWorldReader<SlimeWorld> format, int... bytes) {
        for (int value : bytes) {
            FORMATS.put((byte) value, format);
        }
    }

    public static SlimeWorld readWorld(
            SlimeLoader loader,
            String worldName,
            byte[] serializedWorld,
            SlimePropertyMap propertyMap,
            boolean readOnly
    ) throws IOException, CorruptedWorldException, NewerFormatException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(serializedWorld));
        byte[] fileHeader = new byte[SlimeFormat.SLIME_HEADER.length];
        //noinspection ResultOfMethodCallIgnored
        dataStream.read(fileHeader);

        if (!Arrays.equals(SlimeFormat.SLIME_HEADER, fileHeader))
            throw new CorruptedWorldException(worldName);

        // File version
        byte version = dataStream.readByte();
        if (version > SlimeFormat.SLIME_VERSION)
            throw new NewerFormatException(version);

        SlimeWorldReader<SlimeWorld> reader = FORMATS.get(version);
        return reader.deserializeWorld(version, loader, worldName, dataStream, propertyMap, readOnly);
    }

}
