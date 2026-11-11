package de.lardtom.justmoreparts.power;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PowerPlugTextsTest {
    private static final String DOC = "docs/battery-plug.md";
    private static final List<String> HIDDEN = List.of("keine Stecker und keine Kabel");
    private static final List<String> PAIR = List.of(
            "plug sits in",
            "sits in the drone's plug",
            "sits in the stack's plug",
            "lead sits in",
            "lead along the frame",
            "lead runs",
            "sitzt im Stecker der Drohne",
            "steckt das Akkukabel im Stecker",
            "Akkukabel steckt im Stecker",
            "Kabel läuft vom Stack",
            "Das Kabel läuft",
            "auf das Paar",
            "mit gestecktem Stecker",
            "wird nicht gesteckt gezeichnet",
            "Biegestelle",
            "bend_mm",
            "Kabelstück",
            "Die Farbe folgt der Steckerart");

    private static String flat(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> files(Path dir, String suffix) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> all = Files.walk(dir)) {
            return all.filter(p -> p.getFileName().toString().endsWith(suffix)).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> playerTexts() {
        Path root = PowerFiles.project();
        List<Path> out = new ArrayList<>();
        for (String name : List.of("README.md", "README.de.md", "CHANGELOG.md", "docs/RELEASE-NOTES.md", DOC)) {
            Path file = root.resolve(name);
            if (Files.exists(file)) {
                out.add(file);
            }
        }
        out.addAll(files(root.resolve("docs/modrinth"), ".md"));
        out.addAll(files(root.resolve("common/src/lang"), ".json"));
        out.addAll(files(PowerFiles.resources().resolve("assets/" + PowerFiles.NS + "/lang"), ".json"));
        return out;
    }

    private static List<String> pairPhrases(Path file) {
        String text = flat(file).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String phrase : PAIR) {
            if (text.contains(phrase.toLowerCase(Locale.ROOT))) {
                out.add("„" + phrase + "“");
            }
        }
        return out;
    }

    @Test
    public void batteryPlugDocDescribesHiddenPlugsAndLeads() {
        Path doc = PowerFiles.project().resolve(DOC);
        assertTrue(Files.exists(doc), DOC + " fehlt");
        String text = flat(doc);
        List<String> errors = new ArrayList<>();
        for (String phrase : HIDDEN) {
            if (!text.contains(phrase)) {
                errors.add("fehlt: „" + phrase + "“");
            }
        }
        for (String phrase : pairPhrases(doc)) {
            errors.add("beschreibt noch das gesteckte Paar mit Kabel: " + phrase);
        }
        assertTrue(errors.isEmpty(), DOC + " beschreibt nicht, dass Propwash ab 0.6.0 Stecker und Kabel des gesteckten Akkus "
                + "ausblendet:\n" + String.join("\n", errors));
    }

    @Test
    public void playerTextsDoNotPromiseAPluggedPairWithALead() {
        Path root = PowerFiles.project();
        List<Path> texts = playerTexts();
        List<String> errors = new ArrayList<>();
        for (Path file : texts) {
            List<String> found = pairPhrases(file);
            if (!found.isEmpty()) {
                errors.add(root.relativize(file).toString().replace('\\', '/') + ": " + String.join(", ", found));
            }
        }
        assertTrue(texts.size() > 5, "zu wenige Texte gefunden: " + texts.size());
        assertTrue(errors.isEmpty(), errors.size() + " Texte versprechen noch ein gestecktes Paar mit Kabel, Propwash ab 0.6.0 "
                + "blendet Stecker und Kabel aus:\n" + String.join("\n", errors));
    }

    @Test
    public void noResourceCarriesAPowerLeadEntry() {
        Path res = PowerFiles.resources();
        List<Path> json = new ArrayList<>(files(res.resolve("assets/" + PowerFiles.NS), ".json"));
        json.addAll(files(res.resolve("data/" + PowerFiles.NS), ".json"));
        List<String> errors = new ArrayList<>();
        for (Path file : json) {
            if (flat(file).contains("\"power_lead\"")) {
                errors.add(res.relativize(file).toString().replace('\\', '/'));
            }
        }
        assertTrue(json.size() > 100, "zu wenige Ressourcen gefunden: " + json.size());
        assertTrue(errors.isEmpty(), "power_lead wird von Propwash ab 0.6.0 ignoriert und gehört nicht in die Daten:\n"
                + String.join("\n", errors));
    }
}
