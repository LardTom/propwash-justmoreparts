package de.lardtom.justmoreparts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class JmpCommands {
    public static final String ROOT = "jmp";
    public static final String ALIAS = "justmoreparts";
    public static final String CODE = "code";
    public static final String REST = "arguments";
    public static final String TARGET = "/propwash code ";
    public static final String MOVED_KEY = "justmoreparts.command.code.moved";
    public static final String CLICK_KEY = "justmoreparts.command.code.moved.click";
    public static final int SUCCESS = 1;

    private JmpCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal(ROOT)
                .then(Commands.literal(CODE)
                        .executes(c -> moved(c.getSource(), ""))
                        .then(Commands.argument(REST, StringArgumentType.greedyString())
                                .executes(c -> moved(c.getSource(), StringArgumentType.getString(c, REST))))));
        dispatcher.register(Commands.literal(ALIAS).redirect(root));
    }

    public static String suggestion(String arguments) {
        return TARGET + arguments.strip();
    }

    public static String shown(String arguments) {
        String rest = arguments.strip();
        int space = rest.indexOf(' ');
        return space < 0 ? (TARGET + rest).strip() : TARGET + rest.substring(0, space) + " …";
    }

    public static MutableComponent hint(String arguments) {
        String command = suggestion(arguments);
        MutableComponent link = Component.literal(shown(arguments)).withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.SuggestCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable(CLICK_KEY))));
        return Component.translatable(MOVED_KEY, link).withStyle(ChatFormatting.YELLOW);
    }

    private static int moved(CommandSourceStack source, String arguments) {
        source.sendSuccess(() -> hint(arguments), false);
        return SUCCESS;
    }
}
