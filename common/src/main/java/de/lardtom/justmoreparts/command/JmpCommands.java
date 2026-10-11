package de.lardtom.justmoreparts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.lardtom.justmoreparts.share.PartAliases;
import de.lardtom.justmoreparts.share.ShareCodec;
import de.lardtom.justmoreparts.share.ShareContent;
import de.lardtom.justmoreparts.share.ShareDecoded;
import de.lardtom.justmoreparts.share.ShareException;
import de.lardtom.justmoreparts.share.ShareFormat;
import de.lardtom.justmoreparts.share.ShareLayer;
import de.lardtom.justmoreparts.share.ShareServer;
import de.lardtom.justmoreparts.share.ShareTables;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.ImportResult;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class JmpCommands {
    public static final String ROOT = "jmp";
    public static final String ALIAS = "justmoreparts";
    public static final int SUCCESS = 1;
    public static final int FAILURE = 0;

    private static final SuggestionProvider<CommandSourceStack> LAYER_SUGGESTIONS = (context, builder) -> SharedSuggestionProvider.suggest(
            List.of("parts,paint,tune,name", "parts,tune", "parts,paint", "parts", "parts,paint,tune"), builder);

    private JmpCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal(ROOT)
                .then(Commands.literal("code")
                        .then(Commands.literal("export")
                                .executes(c -> export(c.getSource(), ShareLayer.DEFAULT_EXPORT))
                                .then(Commands.argument("layers", StringArgumentType.greedyString())
                                        .suggests(LAYER_SUGGESTIONS)
                                        .executes(c -> exportWith(c, StringArgumentType.getString(c, "layers")))))
                        .then(Commands.literal("import")
                                .then(Commands.argument("code", StringArgumentType.greedyString())
                                        .executes(c -> importCode(c.getSource(), StringArgumentType.getString(c, "code")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("code", StringArgumentType.greedyString())
                                        .executes(c -> info(c.getSource(), StringArgumentType.getString(c, "code")))))
                        .then(Commands.literal("give")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("code", StringArgumentType.word())
                                        .executes(c -> give(c.getSource(), StringArgumentType.getString(c, "code"),
                                                List.of(c.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(c -> give(c.getSource(), StringArgumentType.getString(c, "code"),
                                                        EntityArgument.getPlayers(c, "targets"))))))));
        dispatcher.register(Commands.literal(ALIAS).redirect(root));
    }

    static Locale locale(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null ? Locale.ROOT : ShareFormat.locale(player.clientInformation().language());
    }

    private static int exportWith(CommandContext<CommandSourceStack> context, String text) throws CommandSyntaxException {
        EnumSet<ShareLayer> layers = EnumSet.of(ShareLayer.PARTS);
        for (String part : text.split("[,;\\s]+")) {
            if (part.isBlank()) {
                continue;
            }
            Optional<ShareLayer> layer = ShareLayer.byKey(part);
            if (layer.isEmpty() || layer.get() == ShareLayer.OSD) {
                context.getSource().sendFailure(Component.translatable("justmoreparts.command.code.bad_layer", Component.literal(part)));
                return FAILURE;
            }
            layers.add(layer.get());
        }
        return export(context.getSource(), layers);
    }

    private static int export(CommandSourceStack source, Collection<ShareLayer> layers) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<DroneData> held = ShareServer.held(player);
        if (held.isEmpty()) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.no_drone"));
            return FAILURE;
        }
        EnumSet<ShareLayer> chosen = EnumSet.copyOf(layers);
        chosen.remove(ShareLayer.OSD);
        ShareContent content = ShareContent.of(held.get(), chosen, Optional.empty());
        String code;
        try {
            code = ShareCodec.encode(content);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.not_shareable", ShareException.reason(e)));
            return FAILURE;
        }
        if (!ShareCodec.fits(code)) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.too_long", code.length(), ShareTables.MAX_INPUT_CHARS));
            return FAILURE;
        }
        MutableComponent header = Component.translatable("justmoreparts.command.code.export", code.length(), ShareFormat.layerList(content.layers()));
        source.sendSuccess(() -> header, false);
        source.sendSuccess(() -> codeLine(code), false);
        return SUCCESS;
    }

    public static MutableComponent codeLine(String code) {
        return Component.literal(code).withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(code))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("justmoreparts.command.code.click_to_copy"))));
    }

    private static Optional<ShareDecoded> decodeOrReport(CommandSourceStack source, String code) {
        ShareDecoded decoded = ShareCodec.decode(code);
        if (!decoded.ok()) {
            source.sendFailure(Component.translatable(decoded.status().translationKey()));
            return Optional.empty();
        }
        return Optional.of(decoded);
    }

    private static boolean reportUnknown(CommandSourceStack source, ShareDecoded decoded) {
        if (!decoded.unknownParts().isEmpty()) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.unknown_parts", ShareFormat.idList(decoded.unknownParts())));
            return true;
        }
        if (!decoded.wrongKind().isEmpty()) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.wrong_kind", ShareFormat.idList(decoded.wrongKind())));
            return true;
        }
        return false;
    }

    private static int importCode(CommandSourceStack source, String code) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<ShareDecoded> result = decodeOrReport(source, code);
        if (result.isEmpty()) {
            return FAILURE;
        }
        ShareDecoded decoded = result.get();
        ShareContent content = decoded.require();
        if (player.isSpectator()) {
            source.sendFailure(Component.translatable("propwash.api.import.not_allowed"));
            return FAILURE;
        }
        if (player.hasInfiniteMaterials()) {
            if (reportUnknown(source, decoded)) {
                return FAILURE;
            }
            return createFor(source, content, List.of(player));
        }
        if (ShareServer.heldLayers(content).isEmpty()) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.parts_at_workbench"));
            return FAILURE;
        }
        if (ShareServer.held(player).isEmpty()) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.no_drone"));
            return FAILURE;
        }
        ImportResult applied = ShareServer.applyToHeld(player, content);
        MutableComponent line = Component.translatable("justmoreparts.command.code.held", Component.translatable(applied.status().translationKey()));
        if (applied.status().success()) {
            source.sendSuccess(() -> line, false);
            source.sendSuccess(() -> Component.translatable("justmoreparts.command.code.parts_at_workbench").withStyle(ChatFormatting.GRAY), false);
            return SUCCESS;
        }
        source.sendFailure(line);
        if (!applied.missingDyes().isEmpty()) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.missing_dyes", dyes(applied.missingDyes())));
        }
        return FAILURE;
    }

    static MutableComponent dyes(Map<String, Integer> dyes) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : dyes.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(entry.getValue() + "× ").append(ShareFormat.dyeName(entry.getKey()));
            first = false;
        }
        return out;
    }

    private static int give(CommandSourceStack source, String code, Collection<ServerPlayer> targets) {
        Optional<ShareDecoded> result = decodeOrReport(source, code);
        if (result.isEmpty() || reportUnknown(source, result.get())) {
            return FAILURE;
        }
        return createFor(source, result.get().require(), targets);
    }

    private static int createFor(CommandSourceStack source, ShareContent content, Collection<ServerPlayer> targets) {
        BuildCheck check = PropwashApi.builds().check(content.build());
        if (!check.buildable()) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.not_buildable", issues(check.problems())));
            return FAILURE;
        }
        ItemStack drone;
        try {
            drone = ShareServer.createDrone(content);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("justmoreparts.command.code.not_shareable", ShareException.reason(e)));
            return FAILURE;
        }
        for (ServerPlayer target : targets) {
            ShareServer.give(target, drone.copy());
        }
        Component name = content.name().<Component>map(Component::literal).orElseGet(() -> ShareFormat.partName(content.build().frame()));
        if (targets.size() == 1) {
            Component who = targets.iterator().next().getDisplayName();
            source.sendSuccess(() -> Component.translatable("justmoreparts.command.code.created", name, who), true);
        } else {
            source.sendSuccess(() -> Component.translatable("justmoreparts.command.code.created_many", name, targets.size()), true);
        }
        return targets.size();
    }

    static MutableComponent issues(List<String> keys) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (String key : keys) {
            if (!first) {
                out.append("; ");
            }
            out.append(Component.translatable(BuildCheck.translationKey(key)));
            first = false;
        }
        return out;
    }

    static MutableComponent warnings(List<String> keys) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (String key : keys) {
            if (!first) {
                out.append("; ");
            }
            out.append(Component.translatable(BuildStats.warningTranslationKey(key)));
            first = false;
        }
        return out;
    }

    private static int info(CommandSourceStack source, String code) {
        Optional<ShareDecoded> result = decodeOrReport(source, code);
        if (result.isEmpty()) {
            return FAILURE;
        }
        List<Component> lines = infoLines(result.get(), locale(source));
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return SUCCESS;
    }

    public static List<Component> infoLines(ShareDecoded decoded, Locale locale) {
        List<Component> lines = new ArrayList<>();
        ShareContent content = decoded.require();
        lines.add(Component.translatable("justmoreparts.command.code.info.layers", ShareFormat.layerList(decoded.layers()))
                .withStyle(ChatFormatting.GOLD));
        content.name().ifPresent(name -> lines.add(Component.translatable("justmoreparts.command.code.info.name", Component.literal(name))));
        for (ShareFormat.Slot slot : ShareFormat.slots(content.build())) {
            MutableComponent part = ShareFormat.partName(slot.id());
            if (decoded.unknownParts().contains(slot.id())) {
                part = Component.literal(ShareTables.display(slot.id())).append(" ")
                        .append(Component.translatable("justmoreparts.share.part.unknown")).withStyle(ChatFormatting.RED);
            } else if (decoded.wrongKind().contains(slot.id())) {
                part = part.append(" ").append(Component.translatable("justmoreparts.share.part.wrong_kind")).withStyle(ChatFormatting.YELLOW);
            }
            lines.add(ShareFormat.partLine(slot, part, "justmoreparts.command.code.info.part"));
        }
        content.paint().ifPresent(paint -> lines.add(Component.translatable("justmoreparts.command.code.info.paint", paint.colors().size())));
        content.tune().ifPresent(tune -> lines.add(Component.translatable("justmoreparts.command.code.info.tune", tune.values().size())));
        content.osd().ifPresent(osd -> lines.add(Component.translatable("justmoreparts.command.code.info.osd", osd.version(), osd.json().length())));
        for (PartAliases.Rename rename : decoded.renamed()) {
            lines.add(Component.translatable("justmoreparts.command.code.info.renamed", Component.literal(rename.from().toString()),
                    Component.literal(rename.to().toString())));
        }
        if (!decoded.skipped().isEmpty()) {
            lines.add(Component.translatable("justmoreparts.command.code.info.skipped", Component.literal(String.join(", ", decoded.skipped())))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!decoded.partsKnown()) {
            lines.add(Component.translatable("justmoreparts.command.code.info.not_checked").withStyle(ChatFormatting.RED));
            return lines;
        }
        BuildCheck check = PropwashApi.builds().check(content.build());
        if (!check.problems().isEmpty()) {
            lines.add(Component.translatable("justmoreparts.command.code.info.problems", issues(check.problems())).withStyle(ChatFormatting.RED));
        } else {
            lines.add(Component.translatable("justmoreparts.command.code.info.buildable").withStyle(ChatFormatting.GREEN));
        }
        if (!check.warnings().isEmpty()) {
            lines.add(Component.translatable("justmoreparts.command.code.info.warnings", issues(check.warnings())).withStyle(ChatFormatting.YELLOW));
        }
        PropwashApi.builds().analyze(content.build()).ifPresent(stats -> {
            lines.add(Component.translatable("justmoreparts.command.code.info.stats",
                    ShareFormat.number(stats.massGrams(), 0, locale),
                    ShareFormat.number(stats.thrustToWeight(), 1, locale),
                    ShareFormat.number(stats.mixedFlightTimeMin(), 1, locale),
                    ShareFormat.number(stats.topSpeedKmh(), 0, locale)));
            if (!stats.warnings().isEmpty()) {
                lines.add(Component.translatable("justmoreparts.command.code.info.analysis_warnings", warnings(stats.warnings()))
                        .withStyle(ChatFormatting.YELLOW));
            }
        });
        return lines;
    }
}
