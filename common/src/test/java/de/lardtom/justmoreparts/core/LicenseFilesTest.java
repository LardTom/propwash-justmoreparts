package de.lardtom.justmoreparts.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LicenseFilesTest {
    private static final String REQUIRED_NOTICE = "Required Notice: Copyright (c) 2026 lardtom";
    private static final String SHIELD_HEADING = "# PolyForm Shield License 1.0.0";
    private static final String SHIELD_SHA256 = "67530f8e9adfcc5d2e9d72b804500cebb7472ff84c34a6729a80a2a9be901ee6";
    private static final String SPDX = "LicenseRef-PolyForm-Shield-1.0.0";

    static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "Projektwurzel nicht gefunden");
        return dir;
    }

    private static String read(String name) throws IOException {
        return Files.readString(root().resolve(name), StandardCharsets.UTF_8);
    }

    @Test
    public void polyformShieldTextIsTheOfficialPlainText() throws IOException, NoSuchAlgorithmException {
        String license = read("LICENSE");
        int start = license.indexOf(SHIELD_HEADING);
        assertTrue(start > 0, "LICENSE enthält den Text der PolyForm Shield License 1.0.0 nach dem eigenen Kopf");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(license.substring(start).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        assertEquals(SHIELD_SHA256, hex.toString(), "Lizenztext muss byte-gleich mit polyformproject.org/licenses/shield/1.0.0.txt sein");
    }

    @Test
    public void requiredNoticeLineInLicenseAndNotice() throws IOException {
        for (String name : List.of("LICENSE", "NOTICE")) {
            assertTrue(read(name).lines().anyMatch(REQUIRED_NOTICE::equals), name + " braucht die Zeile „" + REQUIRED_NOTICE + "“");
        }
    }

    @Test
    public void licenseHeadNamesTheSplitTheOldVersionsAndTheExitClause() throws IOException {
        String head = read("LICENSE");
        head = head.substring(0, head.indexOf(SHIELD_HEADING));
        for (String phrase : List.of("LICENSE-MIT", "1.0.0", "All Rights Reserved", SPDX, "Modpacks.", "Exit clause.",
                "12 consecutive months", "MIT License")) {
            assertTrue(head.contains(phrase), "Kopf von LICENSE nennt „" + phrase + "“");
        }
    }

    @Test
    public void mitPathsExistAndCoverTheDocs() throws IOException {
        List<String> paths = read("LICENSE-MIT").lines().filter(line -> line.startsWith("- "))
                .map(line -> line.substring(2).trim()).toList();
        assertTrue(paths.contains("docs/"), "LICENSE-MIT nennt die Doku");
        for (String path : paths) {
            Path resolved = root().resolve(path);
            assertTrue(path.endsWith("/") ? Files.isDirectory(resolved) : Files.isRegularFile(resolved), "Pfad aus LICENSE-MIT fehlt: " + path);
        }
    }

    @Test
    public void metadataAndJarsCarryTheLicense() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(root().resolve("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        assertEquals(SPDX, properties.getProperty("license"), "gradle.properties: license");
        assertTrue(read("fabric/src/main/resources/fabric.mod.json").contains("\"license\": \"${license}\""), "fabric.mod.json übernimmt license");
        assertTrue(read("neoforge/src/main/resources/META-INF/neoforge.mods.toml").contains("license = \"${license}\""),
                "neoforge.mods.toml übernimmt license");
        assertTrue(read("build-logic/src/main/groovy/multiloader-common.gradle").contains("rootProject.files('LICENSE', 'LICENSE-MIT', 'NOTICE')"),
                "jedes Jar bekommt LICENSE, LICENSE-MIT und NOTICE");
    }

    @Test
    public void contributionFilesAndReadmeSummary() throws IOException {
        for (String name : List.of("CONTRIBUTING.md", "CLA.md", ".github/pull_request_template.md")) {
            assertTrue(Files.isRegularFile(root().resolve(name)), name + " fehlt");
        }
        assertTrue(read(".github/pull_request_template.md").contains("CLA.md"), "Pull-Request-Vorlage fragt nach dem CLA");
        String readme = read("README.md");
        String license = readme.substring(readme.indexOf("## Lizenz"));
        for (String phrase : List.of("PolyForm Shield License 1.0.0", "LICENSE-MIT", "Modpacks", "zwölf Monate", "1.0.0 und älter")) {
            assertTrue(license.contains(phrase), "README-Abschnitt Lizenz nennt „" + phrase + "“");
        }
    }
}
