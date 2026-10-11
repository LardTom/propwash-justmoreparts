package de.lardtom.justmoreparts.share;

import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.command.JmpCommands;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShareContentTest {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        PartFiles.registerAll();
    }

    private static DroneData data() {
        return new DroneData(ShareVectors.buildA(), PaintSpec.DEFAULT.with("frame", 0x102030),
                new TuneSpec(Map.of("rates.roll.rcRate", 1.1F, "vtx.channel", 4.0F, "led.mode", 1.0F, TuneSpec.CAMERA_UPTILT, 25.0F, "hold.gain", 2.0F)),
                Optional.of("  Testdrohne  "));
    }

    @Test
    public void layersSelectWhatGoesIntoTheCode() {
        ShareContent all = ShareContent.of(data(), EnumSet.allOf(ShareLayer.class), Optional.of(new ShareOsd(1, "{}")));
        assertEquals(EnumSet.allOf(ShareLayer.class), all.layers());
        assertEquals(Map.of("rates.roll.rcRate", 1.1F, TuneSpec.CAMERA_UPTILT, 25.0F), all.tune().orElseThrow().values());
        assertEquals(Optional.of("Testdrohne"), all.name());
        ShareContent partsAndTune = ShareContent.of(data(), EnumSet.of(ShareLayer.PARTS, ShareLayer.TUNE), Optional.of(new ShareOsd(1, "{}")));
        assertEquals(EnumSet.of(ShareLayer.PARTS, ShareLayer.TUNE), partsAndTune.layers());
        assertEquals(PaintSpec.DEFAULT, partsAndTune.toDroneData().paint());
        assertEquals(Optional.empty(), partsAndTune.toDroneData().name());
        assertEquals(all.only(EnumSet.of(ShareLayer.PARTS, ShareLayer.TUNE)), partsAndTune);
        DroneData back = all.toDroneData();
        assertEquals(data().build(), back.build());
        assertEquals(data().paint(), back.paint());
    }

    @Test
    public void neverSharedGroupsNeverReachTheCode() {
        String code = ShareCodec.encode(ShareContent.of(data(), EnumSet.allOf(ShareLayer.class), Optional.empty()));
        ShareDecoded decoded = ShareCodec.decode(code);
        for (String key : decoded.require().tune().orElseThrow().values().keySet()) {
            assertFalse(key.startsWith("vtx.") || key.startsWith("led.") || key.startsWith("hold."), key);
        }
        assertEquals(25.0F, decoded.require().tune().orElseThrow().values().get(TuneSpec.CAMERA_UPTILT), "Kamerawinkel reist mit");
    }

    @Test
    public void namesAreLimited() {
        assertThrows(IllegalArgumentException.class, () -> new ShareContent(ShareVectors.buildA(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of("x".repeat(49))));
        assertThrows(IllegalArgumentException.class, () -> new ShareOsd(0, "{}"));
        assertThrows(IllegalArgumentException.class, () -> new ShareOsd(1, "x".repeat(8193)));
    }

    @Test
    public void numbersFollowTheLanguage() {
        assertEquals("1234,5", ShareFormat.number(1234.49, 1, ShareFormat.locale("de_de")));
        assertEquals("1234.5", ShareFormat.number(1234.49, 1, ShareFormat.locale("en_us")));
        assertEquals("12", ShareFormat.number(11.6, 0, Locale.ROOT));
        assertEquals("–", ShareFormat.number(Double.NaN, 1, Locale.ROOT));
    }

    @Test
    public void slotsCountEveryPiece() {
        List<ShareFormat.Slot> slots = ShareFormat.slots(ShareVectors.contentB().build());
        assertEquals(7, slots.size());
        assertEquals(PartKind.ACCESSORY, slots.getLast().kind());
        assertEquals(1 + 1 + 4 + 4 + 1 + 1 + 1, slots.stream().mapToInt(ShareFormat.Slot::count).sum());
    }

    @Test
    public void infoListsPartsChecksAndStats() {
        List<Component> lines = JmpCommands.infoLines(ShareCodec.decode(ShareVectors.A), Locale.GERMAN);
        assertTrue(lines.size() >= 8, lines.toString());
        List<Component> unknown = JmpCommands.infoLines(ShareCodec.decode(ShareVectors.D), Locale.ROOT);
        assertTrue(unknown.stream().anyMatch(line -> line.toString().contains("otheraddon:frame_x")), unknown.toString());
    }
}
