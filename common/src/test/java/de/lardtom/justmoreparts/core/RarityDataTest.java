package de.lardtom.justmoreparts.core;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.frame.FrameModule;
import de.lardtom.justmoreparts.motor.MotorModule;
import de.lardtom.justmoreparts.power.PowerModule;
import de.lardtom.justmoreparts.prop.PropModule;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PropDefinition;
import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RarityDataTest {
    @BeforeAll
    public static void register() {
        PartFiles.registerAll();
    }

    static Rarity rule(PartDefinition definition) {
        String path = definition.id().getPath();
        if (definition instanceof MotorDefinition motor) {
            return MotorModule.rarity(motor);
        }
        if (definition instanceof PropDefinition prop) {
            return PropModule.rarityOf(prop);
        }
        if (FrameModule.X_CLASS.contains(path)) {
            return Rarity.RARE;
        }
        return PowerModule.rarities().getOrDefault(path, Rarity.COMMON);
    }

    @Test
    public void jsonRarityFollowsTheDesignRules() {
        int special = 0;
        for (PartDefinition definition : PropwashApi.parts().all()) {
            if (!JustMoreParts.MOD_ID.equals(definition.id().getNamespace())) {
                continue;
            }
            Rarity expected = rule(definition);
            assertEquals(expected, definition.basics().rarity().orElse(Rarity.COMMON), definition.id().toString());
            if (expected != Rarity.COMMON) {
                assertTrue(definition.basics().rarity().isPresent(), definition.id() + ": \"rarity\" fehlt in der Teile-Datei");
                special++;
            } else {
                assertTrue(definition.basics().rarity().isEmpty(), definition.id() + ": \"common\" nicht eigens eintragen");
            }
        }
        assertEquals(24, special);
    }
}
