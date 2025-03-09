package com.infernalsuite.aswm.serialization.anvil;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public record AnvilImportData(Path worldDir, String newName, @Nullable SlimeLoader loader) {

}
