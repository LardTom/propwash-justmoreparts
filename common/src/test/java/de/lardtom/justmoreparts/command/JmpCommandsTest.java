package de.lardtom.justmoreparts.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JmpCommandsTest {
    static final String PW2 = "PW26Nnq88PGI91DMWZEsVC2rZlP7Bq2a9GosBZmpxkOa4zvubUYHqScfmnw6XhX43iVl";

    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        JmpCommands.register(dispatcher, null, null);
        return dispatcher;
    }

    @Test
    public void rootAliasAndCodeExistWithoutOwnSubcommands() {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(JmpCommands.ROOT);
        assertNotNull(root);
        CommandNode<CommandSourceStack> alias = dispatcher.getRoot().getChild(JmpCommands.ALIAS);
        assertNotNull(alias);
        assertEquals(root, alias.getRedirect());
        CommandNode<CommandSourceStack> code = root.getChild(JmpCommands.CODE);
        assertNotNull(code);
        assertNotNull(code.getCommand());
        assertEquals(1, root.getChildren().size());
        assertEquals(1, code.getChildren().size());
        assertNotNull(code.getChild(JmpCommands.REST));
        for (String sub : List.of("export", "import", "info", "give")) {
            assertEquals(null, code.getChild(sub), sub + " hat keine eigene Ausführung mehr");
        }
    }

    @Test
    public void everyOldFormStillParsesAndEndsInTheHint() {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        for (String input : List.of("jmp code", "jmp code export", "justmoreparts code export", "jmp code export parts,tune",
                "jmp code import " + PW2, "justmoreparts code info " + PW2, "jmp code give " + PW2 + " @a",
                "jmp code info PW1-04002-GR11D - K74SB")) {
            ParseResults<CommandSourceStack> parsed = dispatcher.parse(input, null);
            assertFalse(parsed.getReader().canRead(), input);
            assertTrue(parsed.getExceptions().isEmpty(), input);
            assertNotNull(parsed.getContext().build(input).getLastChild().getCommand(), input);
        }
    }

    @Test
    public void hintSuggestsThePropwashCommandAndShowsNoCode() {
        assertHint("export", "/propwash code export", "/propwash code export");
        assertHint("", "/propwash code ", "/propwash code");
        assertHint("export parts,tune", "/propwash code export parts,tune", "/propwash code export …");
        assertHint("import " + PW2, "/propwash code import " + PW2, "/propwash code import …");
        assertHint("  give " + PW2 + " @a ", "/propwash code give " + PW2 + " @a", "/propwash code give …");
    }

    @Test
    public void hintTextExistsInBothLanguagesWithUmlauts() throws IOException {
        Path dir = root().resolve("common/src/lang/command");
        JsonObject en = JsonParser.parseString(Files.readString(dir.resolve("en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject de = JsonParser.parseString(Files.readString(dir.resolve("de_de.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(en.keySet(), de.keySet());
        for (String key : List.of(JmpCommands.MOVED_KEY, JmpCommands.CLICK_KEY)) {
            assertTrue(en.has(key) && de.has(key), key);
        }
        assertEquals("Share codes now belong to Propwash: %s", en.get(JmpCommands.MOVED_KEY).getAsString());
        assertEquals("Sharecodes gehören jetzt zu Propwash: %s", de.get(JmpCommands.MOVED_KEY).getAsString());
        assertTrue(de.get(JmpCommands.CLICK_KEY).getAsString().contains("übernehmen"));
    }

    private static void assertHint(String arguments, String suggested, String shown) {
        MutableComponent hint = JmpCommands.hint(arguments);
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, hint.getContents());
        assertEquals(JmpCommands.MOVED_KEY, contents.getKey());
        assertEquals(1, contents.getArgs().length);
        Component link = assertInstanceOf(Component.class, contents.getArgs()[0]);
        assertEquals(shown, link.getString());
        ClickEvent click = link.getStyle().getClickEvent();
        ClickEvent.SuggestCommand suggest = assertInstanceOf(ClickEvent.SuggestCommand.class, click);
        assertEquals(suggested, suggest.command());
        assertFalse(link.getString().contains("PW1-") || link.getString().contains("PW2") || link.getString().contains("PW3-"), shown);
    }

    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir);
        return dir;
    }
}
