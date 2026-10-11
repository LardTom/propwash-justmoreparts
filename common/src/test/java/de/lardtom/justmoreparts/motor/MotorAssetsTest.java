package de.lardtom.justmoreparts.motor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.propwash.api.part.MotorDefinition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MotorAssetsTest {
    private static final String ASSETS = "assets/justmoreparts/";

    @Test
    public void everyMotorHasModelTextureRenderDefinitionAndPaintVariant() throws IOException {
        for (MotorDefinition motor : MotorDataTest.motors()) {
            String path = motor.id().getPath();
            for (String suffix : new String[]{"", "_paint"}) {
                JsonObject model = json(ASSETS + "models/part/motor/" + path + suffix + ".json");
                String texture = model.getAsJsonObject("textures").get("0").getAsString();
                assertEquals("justmoreparts:item/part/motor/" + path + suffix, texture);
                BufferedImage image = image(ASSETS + "textures/item/part/motor/" + path + suffix + ".png");
                assertEquals(model.getAsJsonArray("texture_size").get(0).getAsInt(), image.getWidth(), path + suffix);
                assertEquals(model.getAsJsonArray("texture_size").get(1).getAsInt(), image.getHeight(), path + suffix);
                boolean tinted = false;
                double top = 0.0;
                for (JsonElement e : model.getAsJsonArray("elements")) {
                    JsonObject element = e.getAsJsonObject();
                    for (String key : new String[]{"from", "to"}) {
                        for (JsonElement v : element.getAsJsonArray(key)) {
                            assertTrue(v.getAsDouble() >= -16.0 && v.getAsDouble() <= 32.0, path + suffix + ": Element außerhalb −16…32");
                        }
                    }
                    top = Math.max(top, element.getAsJsonArray("to").get(1).getAsDouble());
                    for (String face : element.getAsJsonObject("faces").keySet()) {
                        tinted |= element.getAsJsonObject("faces").getAsJsonObject(face).has("tintindex");
                    }
                }
                assertEquals(!suffix.isEmpty(), tinted, path + suffix + ": tintindex nur in der Lack-Variante");
                double heightMm = (top - 8.0) * 8.0;
                assertTrue(heightMm > motor.statorHeightMm() && heightMm < motor.statorDiameterMm() * 1.4 + 6.0, path + ": Bauhöhe " + heightMm);
                JsonObject render = json(ASSETS + "items/drone_render/motor/" + path + suffix + ".json").getAsJsonObject("model");
                assertEquals("justmoreparts:part/motor/" + path + suffix, render.get("model").getAsString());
                assertEquals(!suffix.isEmpty(), render.has("tints"), path + suffix);
            }
            JsonObject icon = json(ASSETS + "items/" + path + ".json").getAsJsonObject("model");
            assertFalse(icon.entrySet().isEmpty(), path + ": Item-Symbol leer");
        }
    }

    private static JsonObject json(String path) {
        try (InputStream in = MotorAssetsTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "fehlt: " + path);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedImage image(String path) throws IOException {
        try (InputStream in = MotorAssetsTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "fehlt: " + path);
            BufferedImage image = ImageIO.read(in);
            assertNotNull(image, "keine PNG: " + path);
            return image;
        }
    }
}
