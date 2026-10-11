package de.lardtom.justmoreparts.bench;

import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.part.CellChemistry;
import de.lardtom.propwash.api.part.PartDefinition;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PartFilesTest {
    @Test
    public void everyPartFileParsesAndRegisters() {
        Map<Identifier, Object> parts = PartFiles.registerAll();
        for (Identifier id : parts.keySet()) {
            assertTrue(PropwashApi.parts().get(id).isPresent() || PropwashApi.parts().chemistry(id).isPresent(), id + " nicht angemeldet");
        }
        assertEquals(parts.size(), PropwashApi.parts().all().stream().filter(d -> PartFiles.NAMESPACE.equals(d.id().getNamespace())).count()
                + PropwashApi.parts().chemistries().stream().filter(c -> PartFiles.NAMESPACE.equals(c.id().getNamespace())).count());
    }

    @Test
    public void allRegisteredPartsUseTheAddonNamespace() {
        Map<Identifier, Object> parts = PartFiles.registerAll();
        for (Map.Entry<Identifier, Object> entry : parts.entrySet()) {
            if (entry.getValue() instanceof PartDefinition definition) {
                assertEquals(PartFiles.NAMESPACE, definition.id().getNamespace());
            }
        }
    }

    @Test
    public void builtinReferenceAnalysisStillMatches() {
        BuildSpec freestyle = Bench.spec("propwash:freestyle_5", "propwash:stack_30", "propwash:m2207_1750", "propwash:p51_3b",
                "propwash:digital_hd", "propwash:b6s_1300");
        BuildStats stats = Bench.stats(freestyle);
        System.out.println(Bench.header());
        System.out.println(Bench.row("Referenz 5\" propwash (eingebaut)", freestyle));
        assertEquals(525.2, stats.massGrams(), 0.05);
        assertEquals(10.64, stats.thrustToWeight(), 0.05);
    }

    @Test
    public void mapperReadsPropwashBuiltinFilesExactlyLikePropwash() {
        int compared = 0;
        for (PartDefinition definition : PropwashApi.parts().all()) {
            if (!PropwashApi.parts().isBuiltin(definition.id())) {
                continue;
            }
            Object parsed = PartFiles.parse(definition.kind().key(), definition.id(), read(definition.kind().key(), definition.id()));
            assertEquals(definition, parsed, "Abweichung bei " + definition.id());
            compared++;
        }
        for (CellChemistry chemistry : PropwashApi.parts().chemistries()) {
            if ("propwash".equals(chemistry.id().getNamespace())) {
                assertEquals(chemistry, PartFiles.parse("chemistry", chemistry.id(), read("chemistry", chemistry.id())));
                compared++;
            }
        }
        assertTrue(compared >= 30, "nur " + compared + " eingebaute Teile verglichen");
    }

    private static JsonObject read(String category, Identifier id) {
        String path = "data/propwash/propwash_parts/" + category + "/" + id.getPath() + ".json";
        try (InputStream in = PartFilesTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException(path + " fehlt im Propwash-Jar");
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
