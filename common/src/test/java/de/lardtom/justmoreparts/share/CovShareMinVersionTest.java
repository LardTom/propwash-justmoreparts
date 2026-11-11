package de.lardtom.justmoreparts.share;

import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.share.ShareOptions;
import de.lardtom.propwash.api.share.ShareResult;
import de.lardtom.propwash.platform.ModMeta;
import de.lardtom.propwash.share.AddonDirectory;
import de.lardtom.propwash.share.ShareTableJson;
import de.lardtom.propwash.share.ShareTables;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CovShareMinVersionTest {
    static final String RELEASE = "1.2.0";
    static final List<String> NEW_MOTORS = List.of("m0702_30000", "m0702_35000", "m0802_27200");

    static final class JmpInstalled implements ShareTables.Loader {
        @Override
        public Map<String, byte[]> tables(String modId) {
            if (!PartFiles.NAMESPACE.equals(modId)) {
                return Map.of();
            }
            String path = ShareTableJson.tablePath(modId);
            try (InputStream in = CovShareMinVersionTest.class.getClassLoader().getResourceAsStream(path)) {
                assertNotNull(in, path + " fehlt");
                return Map.of(path, in.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Optional<String> version(String modId) {
            return PartFiles.NAMESPACE.equals(modId) ? Optional.of(RELEASE) : Optional.of("0.6.0");
        }

        @Override
        public Optional<ModMeta> meta(String modId) {
            return Optional.empty();
        }

        @Override
        public AddonDirectory directory() {
            return AddonDirectory.builtin();
        }
    }

    static ShareTables installJmp() {
        PartFiles.registerAll();
        ShareTables tables = ShareTables.load(List.of(ShareTables.PROPWASH, PartFiles.NAMESPACE), new JmpInstalled(), ShareTables.REGISTERED,
                ShareTables.registeredParts());
        ShareTables.install(tables);
        return tables;
    }

    static ShareResult.NamespaceInfo jmp(DroneData data, List<String> failures, String label) {
        String code = PropwashApi.shareCodes().encode(data, ShareOptions.defaults());
        ShareResult read = PropwashApi.shareCodes().decode(code);
        assertTrue(read.ok(), label + ": " + read.status() + " " + code);
        assertEquals(ShareResult.Format.PW3, read.format().orElseThrow(), label);
        assertTrue(read.unknownParts().isEmpty(), label + ": unbekannte Teile " + read.unknownParts());
        assertEquals(data.build(), read.data().orElseThrow().build(), label + ": anderer Bauplan nach dem Lesen");
        for (ShareResult.NamespaceInfo info : read.namespaces()) {
            if (PartFiles.NAMESPACE.equals(info.namespace())) {
                return info;
            }
        }
        failures.add(label + ": kein Eintrag für " + PartFiles.NAMESPACE + " in " + read.namespaces());
        return null;
    }

    static void expect(DroneData data, String label, String minVersion, List<String> failures) {
        ShareResult.NamespaceInfo info = jmp(data, failures, label);
        if (info == null) {
            return;
        }
        System.out.println(label + ": " + PartFiles.NAMESPACE + " ab " + info.minVersion().orElse("?") + ", Prüfung " + info.check());
        if (!info.minVersion().equals(Optional.of(minVersion))) {
            failures.add(label + ": Mindestversion " + info.minVersion() + " statt " + minVersion);
        }
        if (info.check() != ShareResult.Check.MATCH) {
            failures.add(label + ": Prüfung " + info.check() + " statt MATCH mit installiertem " + RELEASE);
        }
        if (info.shortId().orElse(-1) != 2) {
            failures.add(label + ": Kurzkennung " + info.shortId() + " statt 2");
        }
    }

    @Test
    public void codeWithANewWhoopMotorNeedsJmp120AndOnlyOldPartsNeed100() {
        ShareTables tables = installJmp();
        try {
            List<String> problems = new ArrayList<>();
            for (String problem : tables.problems()) {
                if (problem.contains(PartFiles.NAMESPACE)) {
                    problems.add(problem);
                }
            }
            assertTrue(problems.isEmpty(), "Tabellen-Befunde für " + PartFiles.NAMESPACE + ": " + problems);
            List<String> failures = new ArrayList<>();
            DroneData race = JmpPresets.byPath("whoop65_race_1s").data();
            assertEquals(Identifier.fromNamespaceAndPath(PartFiles.NAMESPACE, "m0702_35000"), race.build().motor());
            expect(race, "Preset whoop65_race_1s", RELEASE, failures);
            BuildSpec base = JmpPresets.byPath("whoop75_1s").build();
            for (String motor : NEW_MOTORS) {
                BuildSpec spec = base.withPart(PartKind.MOTOR, Identifier.fromNamespaceAndPath(PartFiles.NAMESPACE, motor));
                expect(DroneData.of(spec), "whoop75_1s mit " + motor, RELEASE, failures);
            }
            expect(JmpPresets.byPath("bash5_6s").data(), "Preset bash5_6s", "1.0.0", failures);
            expect(DroneData.of(JmpPresets.byPath("bash5_6s").build()), "Bash nur mit Teilen", "1.0.0", failures);
            expect(JmpPresets.byPath("whoop75_1s").data(), "Preset whoop75_1s", "1.0.0", failures);
            assertTrue(failures.isEmpty(), String.join("\n", failures));
        } finally {
            ShareTables.install(ShareTables.builtin());
        }
    }
}
