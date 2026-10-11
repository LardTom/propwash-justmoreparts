package de.lardtom.justmoreparts.share;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;

public final class PartAliases {
    private static final int MAX_CHAIN = 8;
    private static final Map<Identifier, Identifier> ALIASES = Map.of();

    private PartAliases() {
    }

    public static Map<Identifier, Identifier> all() {
        return ALIASES;
    }

    public static Identifier resolve(Identifier id) {
        return resolve(id, ALIASES);
    }

    static Identifier resolve(Identifier id, Map<Identifier, Identifier> aliases) {
        Identifier current = Objects.requireNonNull(id, "id");
        for (int i = 0; i < MAX_CHAIN; i++) {
            Identifier next = aliases.get(current);
            if (next == null || next.equals(current)) {
                return current;
            }
            current = next;
        }
        return current;
    }

    public record Rename(Identifier from, Identifier to) {
        public Rename {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
    }
}
