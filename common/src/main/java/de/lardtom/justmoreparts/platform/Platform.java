package de.lardtom.justmoreparts.platform;

import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

public interface Platform {
    String name();

    boolean isModLoaded(String modId);

    Optional<String> modVersion(String modId);

    boolean isDevelopment();

    Path gameDir();

    Path configDir();

    SortedMap<String, byte[]> readModResources(String modId, String folder, String suffix);

    default List<Identifier> sortedCreativeTabs() {
        return List.of();
    }
}
