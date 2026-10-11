package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.core.JmpTab;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.platform.Services;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.registry.ExtensionTab;
import de.lardtom.propwash.api.registry.TabSection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CoreSelfTests {
    private CoreSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("jmp: JMP geladen und Propwash-API verfügbar", 40, ctx -> {
            List<String> errors = new ArrayList<>();
            if (!Services.PLATFORM.isModLoaded(JustMoreParts.PROPWASH_ID)) {
                errors.add("Propwash nicht geladen");
            }
            if (!Services.PLATFORM.isModLoaded(JustMoreParts.MOD_ID)) {
                errors.add("JMP nicht geladen");
            }
            if (!PropwashApi.isCompatible(JustMoreParts.REQUIRED_API)) {
                errors.add("Propwash-API " + PropwashApi.API_VERSION + " erfüllt " + JustMoreParts.REQUIRED_API + " nicht");
            }
            if (!PropwashApi.parts().isFrozen()) {
                errors.add("Teileregister nicht eingefroren");
            }
            if (PropwashApi.parts().get(JustMoreParts.propwash("freestyle_5")).isEmpty()) {
                errors.add("eingebauter Rahmen propwash:freestyle_5 fehlt");
            }
            if (JustMoreParts.bundledParts() < 0 || JustMoreParts.items() < 0) {
                errors.add("JustMoreParts.init() lief nicht");
            }
            long own = 0;
            for (PartDefinition definition : PropwashApi.parts().all()) {
                if (JustMoreParts.MOD_ID.equals(definition.id().getNamespace())) {
                    own++;
                    if (PropwashApi.partItems().item(definition.id()).isEmpty()) {
                        errors.add("Teil ohne Item: " + definition.id());
                    }
                }
            }
            if (own != JustMoreParts.items()) {
                errors.add(own + " JMP-Teile, aber " + JustMoreParts.items() + " Items");
            }
            Optional<DronePreset> freestyle = PropwashApi.builds().presets().stream()
                    .filter(p -> p.id().equals(JustMoreParts.propwash("freestyle"))).findFirst();
            if (freestyle.isEmpty()) {
                errors.add("Preset propwash:freestyle fehlt");
            } else if (PropwashApi.builds().analyze(freestyle.get().data().build()).isEmpty()) {
                errors.add("Analyse des Presets propwash:freestyle leer");
            }
            ctx.log("Propwash " + Services.PLATFORM.modVersion(JustMoreParts.PROPWASH_ID).orElse("?") + ", API " + PropwashApi.API_VERSION
                    + ", " + own + " JMP-Teile, " + PropwashApi.parts().all().size() + " Teile gesamt");
            return errors.isEmpty() ? SelfTest.StepResult.pass() : SelfTest.StepResult.fail(String.join("; ", errors));
        });
        SelfTest.clientStep("jmp: eigener Kreativ-Tab „Propwash: Just More Parts“ hinter dem Propwash-Tab", 40, ctx -> tabStep());
    }

    private static SelfTest.StepResult tabStep() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return SelfTest.StepResult.fail("keine Welt");
        }
        Optional<ExtensionTab> declared = JmpTab.tab();
        if (declared.isEmpty()) {
            return SelfTest.StepResult.fail("JmpTab.init() lief nicht");
        }
        ExtensionTab tab = declared.get();
        List<String> errors = new ArrayList<>();
        Identifier mainId = JustMoreParts.propwash("main");
        Identifier ownId = JustMoreParts.propwash("main-" + JustMoreParts.MOD_ID);
        if (PropwashApi.extensionTab(JustMoreParts.MOD_ID) != tab || !tab.tabId().equals(ownId) || !tab.registered()) {
            errors.add("JMP-Tab falsch angemeldet: " + tab);
        }
        CreativeModeTab own = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(ownId);
        CreativeModeTab main = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(mainId);
        if (own == null || main == null) {
            return SelfTest.StepResult.fail("Tab fehlt im Register: " + (own == null ? ownId : mainId));
        }
        Language language = Language.getInstance();
        String title = own.getDisplayName().getString();
        if (!language.has(JmpTab.NAME_KEY) || !title.equals("Propwash: " + language.getOrDefault(JmpTab.NAME_KEY))
                || !title.equals("Propwash: Just More Parts")) {
            errors.add("Titel „" + title + "“ statt „Propwash: Just More Parts“");
        }
        Optional<Item> icon = PropwashApi.partItems().item(JmpTab.ICON);
        if (icon.isEmpty() || !own.getIconItem().is(icon.get())) {
            errors.add("Symbol ist nicht " + JmpTab.ICON + ": " + own.getIconItem());
        }
        if (!tab.sections().subList(0, JmpTab.ORDER.size()).equals(JmpTab.ORDER)) {
            errors.add("Abschnitte " + tab.sections());
        }
        CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
        List<ItemStack> shown = List.copyOf(own.getDisplayItems());
        List<ItemStack> base = List.copyOf(main.getDisplayItems());
        int last = -1;
        int drones = 0;
        for (ItemStack stack : shown) {
            int section = section(tab, stack);
            if (section < 0) {
                errors.add("fremder Eintrag im JMP-Tab: " + stack);
            } else if (section < last) {
                errors.add(stack + " steht hinter Abschnitt " + tab.sections().get(last));
            } else {
                last = section;
            }
            if (PropwashApi.builds().read(stack).isPresent()) {
                drones++;
            }
        }
        long presets = PropwashApi.builds().presets().stream()
                .filter(preset -> preset.creativeListed() && JustMoreParts.MOD_ID.equals(preset.id().getNamespace())).count();
        if (drones != presets || (presets > 0 && (shown.isEmpty() || PropwashApi.builds().read(shown.getFirst()).isEmpty()))) {
            errors.add(drones + " Presets im JMP-Tab statt " + presets + " vorneweg");
        }
        for (PartDefinition definition : PropwashApi.parts().all()) {
            Optional<Item> item = PropwashApi.partItems().item(definition.id());
            if (!JustMoreParts.MOD_ID.equals(definition.id().getNamespace()) || item.isEmpty()) {
                continue;
            }
            if (definition.basics().creativeListed() && shown.stream().noneMatch(stack -> stack.is(item.get()))) {
                errors.add(definition.id() + " fehlt im JMP-Tab");
            }
            if (base.stream().anyMatch(stack -> stack.is(item.get()))) {
                errors.add(definition.id() + " steht im Propwash-Tab");
            }
        }
        for (ItemStack stack : base) {
            Optional<DroneData> data = PropwashApi.builds().read(stack);
            if (data.isPresent() && JustMoreParts.MOD_ID.equals(data.get().build().frame().getNamespace())) {
                errors.add("JMP-Preset im Propwash-Tab: " + data.get().build().frame());
            }
        }
        List<Identifier> order = new ArrayList<>(Services.PLATFORM.sortedCreativeTabs());
        if (order.isEmpty()) {
            for (Identifier id : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
                if (!Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
                    order.add(id);
                }
            }
            order.sort(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath));
        }
        int mainIndex = order.indexOf(mainId);
        int ownIndex = order.indexOf(ownId);
        if (mainIndex < 0 || ownIndex <= mainIndex || !order.subList(mainIndex + 1, ownIndex).stream()
                .allMatch(id -> JustMoreParts.PROPWASH_ID.equals(id.getNamespace()) && id.getPath().startsWith("main-"))) {
            errors.add("JMP-Tab steht nicht direkt hinter dem Propwash-Tab: " + order);
        }
        return errors.isEmpty() ? SelfTest.StepResult.pass() : SelfTest.StepResult.fail(String.join("; ", errors));
    }

    private static int section(ExtensionTab tab, ItemStack stack) {
        if (PropwashApi.builds().read(stack).isPresent()) {
            return tab.sections().indexOf(TabSection.PRESETS);
        }
        Optional<TabSection> part = PropwashApi.partItems().partOf(stack).flatMap(id -> PropwashApi.parts().get(id))
                .filter(definition -> JustMoreParts.MOD_ID.equals(definition.id().getNamespace()))
                .flatMap(definition -> TabSection.of(definition.kind()));
        return part.map(tab.sections()::indexOf).orElse(-1);
    }
}
