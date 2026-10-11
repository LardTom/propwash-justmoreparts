package de.lardtom.justmoreparts.share;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShareExceptionTest {
    private static final List<String> KEYS = List.of(ShareException.REJECTED, "justmoreparts.share.error.accessory_slot",
            "justmoreparts.share.error.part_id", "justmoreparts.share.error.namespace", "justmoreparts.share.error.paint_slot",
            "justmoreparts.share.error.name_length", "justmoreparts.share.error.osd_version", "justmoreparts.share.error.osd_length");
    private static final Pattern ARG = Pattern.compile("%(\\d+\\$)?s");

    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        PartFiles.registerAll();
    }

    @Test
    public void rejectedContentCarriesATranslationKeyInsteadOfGermanText() {
        ShareException name = assertThrows(ShareException.class, () -> new ShareContent(ShareVectors.buildA(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of("x".repeat(ShareTables.MAX_NAME_CHARS + 1))));
        assertEquals("justmoreparts.share.error.name_length", name.key());
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, name.reason().getContents());
        assertEquals(name.key(), contents.getKey());
        ShareException osd = assertThrows(ShareException.class, () -> new ShareOsd(0, "{}"));
        assertEquals("justmoreparts.share.error.osd_version", osd.key());
        Component other = ShareException.reason(new IllegalArgumentException("Unbekannter Rahmen"));
        assertEquals(ShareException.REJECTED, assertInstanceOf(TranslatableContents.class, other.getContents()).getKey());
    }

    @Test
    public void everyErrorKeyIsTranslatedWithTheSameArguments() throws IOException {
        Path resources = PartFiles.root().getParent().getParent().getParent();
        JsonObject en = read(resources.resolve("assets/justmoreparts/lang/en_us.json"));
        JsonObject de = read(resources.resolve("assets/justmoreparts/lang/de_de.json"));
        for (String key : KEYS) {
            assertTrue(en.has(key) && de.has(key), key);
            assertEquals(arguments(en.get(key).getAsString()), arguments(de.get(key).getAsString()), key);
            assertFalse(en.get(key).getAsString().matches(".*[äöüß].*"), key);
        }
    }

    private static int arguments(String text) {
        Matcher matcher = ARG.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
