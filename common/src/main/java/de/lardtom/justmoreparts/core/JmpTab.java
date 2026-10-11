package de.lardtom.justmoreparts.core;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.registry.ExtensionTab;
import de.lardtom.propwash.api.registry.TabSection;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public final class JmpTab {
    public static final Identifier ICON = JustMoreParts.id("deadcat_5");
    public static final List<TabSection> ORDER = List.of(TabSection.PRESETS, TabSection.FRAMES, TabSection.MOTORS, TabSection.PROPS,
            TabSection.STACKS, TabSection.BATTERIES, TabSection.VIDEO);
    public static final String NAME_KEY = "itemGroup." + JustMoreParts.MOD_ID;

    private static ExtensionTab tab;

    private JmpTab() {
    }

    public static synchronized ExtensionTab init() {
        if (tab == null) {
            tab = PropwashApi.extensionTab(JustMoreParts.MOD_ID).icon(ICON).order(ORDER.toArray(TabSection[]::new));
        }
        return tab;
    }

    public static synchronized Optional<ExtensionTab> tab() {
        return Optional.ofNullable(tab);
    }
}
