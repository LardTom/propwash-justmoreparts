package de.lardtom.justmoreparts.share;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ShareDecoded(ShareStatus status, Optional<ShareContent> content, List<ShareLayer> layers, List<Identifier> unknownParts,
                           List<Identifier> wrongKind, List<PartAliases.Rename> renamed, List<String> skipped) {
    public ShareDecoded {
        Objects.requireNonNull(status, "status");
        content = Objects.requireNonNullElse(content, Optional.empty());
        layers = List.copyOf(Objects.requireNonNullElse(layers, List.of()));
        unknownParts = List.copyOf(Objects.requireNonNullElse(unknownParts, List.of()));
        wrongKind = List.copyOf(Objects.requireNonNullElse(wrongKind, List.of()));
        renamed = List.copyOf(Objects.requireNonNullElse(renamed, List.of()));
        skipped = List.copyOf(Objects.requireNonNullElse(skipped, List.of()));
        if (status.ok() != content.isPresent()) {
            throw new IllegalArgumentException("Inhalt genau dann, wenn der Status OK ist");
        }
    }

    public static ShareDecoded failed(ShareStatus status) {
        return new ShareDecoded(status, Optional.empty(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public boolean ok() {
        return this.status.ok();
    }

    public boolean partsKnown() {
        return this.ok() && this.unknownParts.isEmpty() && this.wrongKind.isEmpty();
    }

    public ShareContent require() {
        return this.content.orElseThrow(() -> new IllegalStateException("Sharecode nicht lesbar: " + this.status));
    }
}
