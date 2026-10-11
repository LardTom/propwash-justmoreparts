package de.lardtom.justmoreparts.share.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.share.ShareCodec;
import de.lardtom.justmoreparts.share.ShareContent;
import de.lardtom.justmoreparts.share.ShareDecoded;
import de.lardtom.justmoreparts.share.ShareStatus;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.DroneData;

import java.util.Objects;
import java.util.Optional;

public final class ShareImportState {
    private String text = "";
    private ShareDecoded decoded = ShareDecoded.failed(ShareStatus.EMPTY);
    private Optional<BuildCheck> check = Optional.empty();
    private Optional<BuildStats> stats = Optional.empty();
    private int revision;

    public boolean update(String input) {
        String value = Objects.requireNonNullElse(input, "");
        if (value.equals(this.text) && this.revision > 0) {
            return false;
        }
        this.text = value;
        this.revision++;
        this.decoded = ShareCodec.decode(value);
        this.check = Optional.empty();
        this.stats = Optional.empty();
        if (this.decoded.partsKnown()) {
            try {
                this.check = Optional.of(PropwashApi.builds().check(this.decoded.require().build()));
                this.stats = PropwashApi.builds().analyze(this.decoded.require().build());
            } catch (RuntimeException e) {
                JustMoreParts.LOG.debug("Sharecode-Vorschau: Prüfung fehlgeschlagen", e);
            }
        }
        return true;
    }

    public String text() {
        return this.text;
    }

    public ShareDecoded decoded() {
        return this.decoded;
    }

    public Optional<ShareContent> content() {
        return this.decoded.content();
    }

    public Optional<BuildCheck> check() {
        return this.check;
    }

    public Optional<BuildStats> stats() {
        return this.stats;
    }

    public int revision() {
        return this.revision;
    }

    public boolean buildable() {
        return this.check.map(BuildCheck::buildable).orElse(false);
    }

    public Optional<DroneData> preview() {
        if (!this.decoded.partsKnown()) {
            return Optional.empty();
        }
        return this.decoded.content().map(ShareContent::toDroneData);
    }
}
