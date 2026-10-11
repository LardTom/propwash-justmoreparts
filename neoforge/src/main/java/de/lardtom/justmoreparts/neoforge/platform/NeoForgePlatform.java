package de.lardtom.justmoreparts.neoforge.platform;

import de.lardtom.justmoreparts.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public final class NeoForgePlatform implements Platform {
    @Override
    public String name() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public List<Identifier> sortedCreativeTabs() {
        List<Identifier> ids = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    @Override
    public boolean isDevelopment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public SortedMap<String, byte[]> readModResources(String modId, String folder, String suffix) {
        IModFileInfo info = ModList.get().getModFileById(modId);
        if (info == null) {
            throw new IllegalArgumentException("Mod " + modId + " ist nicht geladen");
        }
        SortedMap<String, byte[]> out = new TreeMap<>();
        info.getFile().getContents().visitContent(folder, (path, resource) -> {
            if (path.endsWith(suffix) && !out.containsKey(path)) {
                try {
                    out.put(path, resource.readAllBytes());
                } catch (IOException e) {
                    throw new UncheckedIOException("Ressource " + path + " von " + modId + " nicht lesbar", e);
                }
            }
        });
        return out;
    }
}
