package de.lardtom.justmoreparts.share;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import de.lardtom.justmoreparts.command.JmpCommands;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShareCommandTreeTest {
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
    public void rootAndAliasExist() {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(JmpCommands.ROOT);
        assertNotNull(root);
        CommandNode<CommandSourceStack> alias = dispatcher.getRoot().getChild(JmpCommands.ALIAS);
        assertNotNull(alias);
        assertEquals(root, alias.getRedirect());
        CommandNode<CommandSourceStack> code = root.getChild("code");
        assertNotNull(code);
        for (String sub : List.of("export", "import", "info", "give")) {
            assertNotNull(code.getChild(sub), sub);
        }
    }

    @Test
    public void codesParseAsOneArgument() {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        for (String input : List.of("jmp code info " + ShareVectors.B, "jmp code import " + ShareVectors.A,
                "jmp code info " + ShareVectors.A.replace("-", " - "), "justmoreparts code info " + ShareVectors.C, "jmp code export parts,tune")) {
            ParseResults<CommandSourceStack> parsed = dispatcher.parse(input, null);
            assertFalse(parsed.getReader().canRead(), input);
            assertTrue(parsed.getExceptions().isEmpty(), input);
            assertNotNull(parsed.getContext().build(input).getLastChild().getCommand(), input);
        }
    }

    @Test
    public void codesFitIntoTheChatWhenShort() {
        assertTrue(("jmp code import " + ShareVectors.A).length() <= 256);
    }
}
