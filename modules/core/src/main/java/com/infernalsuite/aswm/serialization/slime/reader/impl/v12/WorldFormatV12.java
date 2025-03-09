package com.infernalsuite.aswm.serialization.slime.reader.impl.v12;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.serialization.slime.reader.impl.SimpleWorldFormat;

public interface WorldFormatV12 {

    SimpleWorldFormat<SlimeWorld> FORMAT = new SimpleWorldFormat<>(data -> data, new SlimeWorldDeserializer());

}
