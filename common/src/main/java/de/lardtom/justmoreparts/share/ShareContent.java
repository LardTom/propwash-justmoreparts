package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ShareContent(BuildSpec build, Optional<PaintSpec> paint, Optional<TuneSpec> tune, Optional<ShareOsd> osd, Optional<String> name) {
    public ShareContent {
        Objects.requireNonNull(build, "build");
        paint = Objects.requireNonNullElse(paint, Optional.<PaintSpec>empty()).filter(p -> !p.isDefault());
        tune = Objects.requireNonNullElse(tune, Optional.<TuneSpec>empty()).map(t -> t.only(ShareTables.TUNE_GROUPS));
        osd = Objects.requireNonNullElse(osd, Optional.empty());
        name = Objects.requireNonNullElse(name, Optional.<String>empty()).map(String::strip).filter(n -> !n.isEmpty());
        if (name.isPresent() && name.get().length() > ShareTables.MAX_NAME_CHARS) {
            throw new ShareException("justmoreparts.share.error.name_length", ShareTables.MAX_NAME_CHARS);
        }
    }

    public static ShareContent of(BuildSpec build) {
        return new ShareContent(build, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ShareContent of(DroneData data, Collection<ShareLayer> layers, Optional<ShareOsd> osd) {
        Objects.requireNonNull(data, "data");
        return new ShareContent(data.build(),
                layers.contains(ShareLayer.PAINT) ? Optional.of(data.paint()) : Optional.empty(),
                layers.contains(ShareLayer.TUNE) ? Optional.of(data.tune()) : Optional.empty(),
                layers.contains(ShareLayer.OSD) ? osd : Optional.empty(),
                layers.contains(ShareLayer.NAME) ? data.name() : Optional.empty());
    }

    public Set<ShareLayer> layers() {
        EnumSet<ShareLayer> out = EnumSet.of(ShareLayer.PARTS);
        if (this.paint.isPresent()) {
            out.add(ShareLayer.PAINT);
        }
        if (this.tune.isPresent()) {
            out.add(ShareLayer.TUNE);
        }
        if (this.osd.isPresent()) {
            out.add(ShareLayer.OSD);
        }
        if (this.name.isPresent()) {
            out.add(ShareLayer.NAME);
        }
        return out;
    }

    public boolean has(ShareLayer layer) {
        return this.layers().contains(layer);
    }

    public ShareContent only(Collection<ShareLayer> layers) {
        return new ShareContent(this.build,
                layers.contains(ShareLayer.PAINT) ? this.paint : Optional.empty(),
                layers.contains(ShareLayer.TUNE) ? this.tune : Optional.empty(),
                layers.contains(ShareLayer.OSD) ? this.osd : Optional.empty(),
                layers.contains(ShareLayer.NAME) ? this.name : Optional.empty());
    }

    public ShareContent withBuild(BuildSpec value) {
        return new ShareContent(value, this.paint, this.tune, this.osd, this.name);
    }

    public DroneData toDroneData() {
        return new DroneData(this.build, this.paint.orElse(PaintSpec.DEFAULT), this.tune.orElse(TuneSpec.EMPTY), this.name);
    }
}
