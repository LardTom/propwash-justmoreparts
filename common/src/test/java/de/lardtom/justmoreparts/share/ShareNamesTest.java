package de.lardtom.justmoreparts.share;

import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.SharedConstants;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShareNamesTest {
    private static final String FORMAT = "justmoreparts.share.import.part";
    private static final Map<String, Pattern> PROP_NAMES = Map.of(
            "en_us", Pattern.compile("^(\\d+ mm|\\d+(\\.\\d)?″) Propeller \\(\\d-blade, \\d+\\.\\d″ pitch, [^()]+\\)$"),
            "de_de", Pattern.compile("^(\\d+-mm|\\d+(,\\d)?-Zoll)-Propeller \\(\\d Blätter, \\d+,\\d″ Steigung, [^()]+\\)$"));

    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        PartFiles.registerAll();
    }

    @AfterEach
    public void restoreLanguage() {
        Language.inject(Language.DEFAULT_INSTANCE);
    }

    @Test
    public void partLinesNameTheKindOnlyOnce() throws IOException {
        for (String language : PROP_NAMES.keySet()) {
            Language.inject(language(language));
            List<String> problems = new ArrayList<>();
            for (PartDefinition part : PropwashApi.parts().all()) {
                int count = part.kind() == PartKind.MOTOR || part.kind() == PartKind.PROP ? 4 : 1;
                ShareFormat.Slot slot = new ShareFormat.Slot(part.kind(), Optional.empty(), part.id(), count);
                String line = ShareFormat.partLine(slot, ShareFormat.partName(part.id()), FORMAT).getString();
                String kind = ShareFormat.kindName(part.kind()).getString().toLowerCase(Locale.ROOT);
                int seen = occurrences(line.toLowerCase(Locale.ROOT), kind);
                if (seen != 1) {
                    problems.add(language + " " + part.id() + ": „" + line + "“ nennt „" + kind + "“ " + seen + "×");
                }
                if (count > 1 && !line.startsWith(count + "× ")) {
                    problems.add(language + " " + part.id() + ": Stückzahl fehlt in „" + line + "“");
                }
            }
            assertTrue(problems.isEmpty(), String.join("\n", problems));
        }
    }

    @Test
    public void propNamesFollowTheNamingScheme() throws IOException {
        for (Map.Entry<String, Pattern> entry : PROP_NAMES.entrySet()) {
            Map<String, String> table = table(entry.getKey());
            List<String> problems = new ArrayList<>();
            int props = 0;
            for (PartDefinition part : PropwashApi.parts().all(PartKind.PROP)) {
                if (!part.id().getNamespace().equals(PartFiles.NAMESPACE)) {
                    continue;
                }
                props++;
                String name = table.get("part." + part.id().getNamespace() + "." + part.id().getPath());
                if (name == null || !entry.getValue().matcher(name).matches()) {
                    problems.add(entry.getKey() + " " + part.id() + ": „" + name + "“");
                }
            }
            assertEquals(38, props);
            assertTrue(problems.isEmpty(), String.join("\n", problems));
        }
    }

    @Test
    public void codeWrapsAtGroupsWithoutExtraSpaces() {
        String code = ShareVectors.A;
        for (int width : new int[]{36, 90, 150, 400}) {
            List<String> rows = ShareFormat.wrapCode(code, width, text -> 6 * text.length());
            assertEquals(code, String.join("", rows));
            for (String row : rows) {
                assertFalse(row.contains(" "), row);
                assertTrue(6 * row.length() <= width || !row.substring(0, row.length() - 1).contains("-"), width + ": " + row);
            }
            for (int i = 0; i < rows.size() - 1; i++) {
                assertTrue(rows.get(i).endsWith("-"), rows.get(i));
            }
        }
        assertEquals(List.of(code), ShareFormat.wrapCode(code, 10_000, String::length));
    }

    @Test
    public void pw2CodeWrapsByCharacterToTheBoxWidth() {
        String code = ShareVectors.P2_H;
        for (int width : new int[]{36, 90, 150, 400}) {
            List<String> rows = ShareFormat.wrapCode(code, width, text -> 6 * text.length());
            assertEquals(code, String.join("", rows));
            for (String row : rows) {
                assertTrue(6 * row.length() <= width, width + ": " + row);
            }
            for (int i = 0; i < rows.size() - 1; i++) {
                assertEquals(width / 6, rows.get(i).length(), width + ": " + rows.get(i));
            }
        }
        assertEquals(List.of(code), ShareFormat.wrapCode(code, 10_000, String::length));
    }

    private static int occurrences(String text, String word) {
        int count = 0;
        for (int at = text.indexOf(word); at >= 0; at = text.indexOf(word, at + word.length())) {
            if (!text.startsWith("h-", at - 2)) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, String> table(String language) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (InputStream base = ShareNamesTest.class.getResourceAsStream("/assets/propwash/lang/" + language + ".json")) {
            assertNotNull(base, "Propwash-Sprachdatei " + language + " fehlt auf dem Klassenpfad");
            Language.loadFromJson(base, map::put);
        }
        Path own = PartFiles.root().getParent().getParent().getParent().resolve("assets/" + PartFiles.NAMESPACE + "/lang/" + language + ".json");
        try (InputStream in = Files.newInputStream(own)) {
            Language.loadFromJson(in, map::put);
        }
        return map;
    }

    private static Language language(String code) throws IOException {
        Map<String, String> table = table(code);
        return new Language() {
            @Override
            public String getOrDefault(String elementId, String defaultValue) {
                return table.getOrDefault(elementId, defaultValue);
            }

            @Override
            public boolean has(String elementId) {
                return table.containsKey(elementId);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText logicalOrderText) {
                return FormattedCharSequence.EMPTY;
            }
        };
    }
}
