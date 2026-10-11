package de.lardtom.justmoreparts.prop;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.part.PropDefinition;
import de.lardtom.propwash.api.part.PropMaterial;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

public final class PropModule {
    public static final int X_CLASS_MIN_MM = 254;

    private PropModule() {
    }

    public static List<PropDefinition> ownProps() {
        List<PropDefinition> out = new ArrayList<>();
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.PROP)) {
            if (definition instanceof PropDefinition prop && JustMoreParts.MOD_ID.equals(prop.id().getNamespace())) {
                out.add(prop);
            }
        }
        return out;
    }

    public static Rarity rarityOf(PropDefinition prop) {
        if (prop.diameterMm() < X_CLASS_MIN_MM) {
            return Rarity.COMMON;
        }
        return prop.material() == PropMaterial.CARBON ? Rarity.RARE : Rarity.UNCOMMON;
    }
}
