package de.lardtom.justmoreparts.fabric.platform;

import de.lardtom.justmoreparts.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class FabricPlatform implements Platform {
    @Override
    public String name() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(container -> container.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public SortedMap<String, byte[]> readModResources(String modId, String folder, String suffix) {
        ModContainer container = FabricLoader.getInstance().getModContainer(modId)
                .orElseThrow(() -> new IllegalArgumentException("Mod " + modId + " ist nicht geladen"));
        SortedMap<String, byte[]> out = new TreeMap<>();
        for (Path root : container.getRootPaths()) {
            Path start = root.resolve(folder);
            if (!Files.isDirectory(start)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(start)) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    String relative = root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
                    if (relative.endsWith(suffix) && !out.containsKey(relative)) {
                        out.put(relative, Files.readAllBytes(file));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Ressourcen von " + modId + " unter " + folder + " nicht lesbar", e);
            }
        }
        return out;
    }
}
