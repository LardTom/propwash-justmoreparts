package de.lardtom.justmoreparts.motor;

import de.lardtom.propwash.api.part.MotorDefinition;
import net.minecraft.world.item.Rarity;

public final class MotorModule {
    public static final double UNCOMMON_STATOR_MM = 30.0;
    public static final double RARE_STATOR_MM = 40.0;

    private MotorModule() {
    }

    public static Rarity rarity(MotorDefinition motor) {
        if (motor.statorDiameterMm() >= RARE_STATOR_MM) {
            return Rarity.RARE;
        }
        if (motor.statorDiameterMm() >= UNCOMMON_STATOR_MM) {
            return Rarity.UNCOMMON;
        }
        return Rarity.COMMON;
    }
}
