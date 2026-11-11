package de.lardtom.justmoreparts.command.client;

import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.share.ShareOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class CovCodeChatSelfTests {
    public static final String GROUP = "jmp: Abdeckung cov-jmp – ";
    public static final String HINT_STEP = GROUP + "/jmp code export und /justmoreparts code export zeigen den Hinweis (Englisch und Deutsch)";
    public static final String INFO_STEP = GROUP + "/propwash code info nennt Just More Parts ab 1.2.0 bzw. ab 1.0.0 (Deutsch und Englisch)";
    static final String EN = "en_us";
    static final String DE = "de_de";
    static final String SUGGESTED = "/propwash code export";
    static final Pattern CODE = Pattern.compile("PW1-|PW2[0-9A-Za-z]{6,}|PW3-");
    static final int WAIT_TICKS = 80;
    static final int SETTLE_TICKS = 10;

    private static String savedLanguage;

    private CovCodeChatSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep(HINT_STEP, 2400, CovCodeChatSelfTests::hintStep);
        SelfTest.clientStep(INFO_STEP, 2400, CovCodeChatSelfTests::infoStep);
    }

    record Probe(String language, String command, String expected) {
    }

    static List<GuiMessage> messages() {
        try {
            Field field = ChatComponent.class.getDeclaredField("allMessages");
            field.setAccessible(true);
            List<GuiMessage> out = new ArrayList<>();
            for (Object message : (List<?>) field.get(Minecraft.getInstance().gui.hud.getChat())) {
                if (message instanceof GuiMessage gui) {
                    out.add(gui);
                }
            }
            return out;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Chat nicht lesbar: " + e, e);
        }
    }

    static Set<GuiMessage> snapshot() {
        Set<GuiMessage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.addAll(messages());
        return seen;
    }

    static List<Component> since(Set<GuiMessage> seen) {
        List<Component> out = new ArrayList<>();
        List<GuiMessage> all = messages();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (!seen.contains(all.get(i))) {
                out.add(all.get(i).content());
            }
        }
        return out;
    }

    static void suggestions(Component component, List<String> out) {
        ClickEvent click = component.getStyle().getClickEvent();
        if (click instanceof ClickEvent.SuggestCommand suggest) {
            out.add(suggest.command());
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    suggestions(inner, out);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            suggestions(sibling, out);
        }
    }

    static StepResult language(SelfTest.StepContext ctx, String key, String code) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        if (!state.containsKey(key)) {
            String current = mc.getLanguageManager().getSelected();
            if (savedLanguage == null) {
                savedLanguage = current;
            }
            if (code.equals(current)) {
                state.put(key, CompletableFuture.completedFuture(null));
                return StepResult.pass();
            }
            mc.gui.setScreen(null);
            mc.getLanguageManager().setSelected(code);
            mc.options.languageCode = code;
            state.put(key, mc.reloadResourcePacks());
            return StepResult.pending();
        }
        CompletableFuture<?> reload = (CompletableFuture<?>) state.get(key);
        if (!reload.isDone() || mc.gui.overlay() != null) {
            return StepResult.pending();
        }
        if (reload.isCompletedExceptionally()) {
            return StepResult.fail("Sprache " + code + ": " + reload.exceptionNow());
        }
        return StepResult.pass();
    }

    static StepResult send(SelfTest.StepContext ctx, String key, String command, int lines) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        if (mc.player == null || mc.getConnection() == null) {
            return StepResult.fail("kein Spieler verbunden");
        }
        if (!state.containsKey(key + ".seen")) {
            state.put(key + ".seen", snapshot());
            state.put(key + ".tick", ctx.tick());
            mc.getConnection().sendCommand(command);
            return StepResult.pending();
        }
        @SuppressWarnings("unchecked")
        Set<GuiMessage> seen = (Set<GuiMessage>) state.get(key + ".seen");
        int waited = ctx.tick() - (int) state.get(key + ".tick");
        List<Component> fresh = since(seen);
        if (fresh.size() >= lines && waited >= SETTLE_TICKS) {
            state.put(key + ".lines", fresh);
            return StepResult.pass();
        }
        if (waited > WAIT_TICKS) {
            return StepResult.fail("/" + command + ": " + fresh.size() + " statt mindestens " + lines + " neue Chatzeilen " + text(fresh));
        }
        return StepResult.pending();
    }

    static List<String> text(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component line : lines) {
            out.add(line.getString());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<Component> lines(SelfTest.StepContext ctx, String key) {
        return (List<Component>) ctx.state().get(key + ".lines");
    }

    static StepResult hold(SelfTest.StepContext ctx) {
        Map<String, Object> state = ctx.state();
        if (!state.containsKey("held")) {
            DroneData data = JmpPresets.byPath("bash5_6s").data();
            state.put("held", ctx.onServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND, PropwashApi.builds().createDrone(data));
                player.inventoryMenu.sendAllDataToRemote();
            }));
            return StepResult.pending();
        }
        CompletableFuture<?> held = (CompletableFuture<?>) state.get("held");
        if (held.isCompletedExceptionally()) {
            return StepResult.fail("JMP-Drohne nicht in die Hand gelegt: " + held.exceptionNow());
        }
        Minecraft mc = Minecraft.getInstance();
        boolean inHand = mc.player != null && PropwashApi.builds().isDrone(mc.player.getMainHandItem());
        return held.isDone() && inHand ? StepResult.pass() : ctx.tick() > 200 ? StepResult.fail("JMP-Drohne nicht in der Hand") : StepResult.pending();
    }

    static void clearHand(SelfTest.StepContext ctx) {
        ctx.onServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().clearContent();
            player.inventoryMenu.sendAllDataToRemote();
        });
    }

    static StepResult restore(SelfTest.StepContext ctx, StepResult result) {
        Map<String, Object> state = ctx.state();
        state.putIfAbsent("result", result);
        String saved = savedLanguage == null ? EN : savedLanguage;
        StepResult back = language(ctx, "restore", saved);
        if (back.status() == StepResult.Status.PENDING) {
            return back;
        }
        savedLanguage = null;
        StepResult first = (StepResult) state.get("result");
        return first.status() == StepResult.Status.FAIL || back.status() == StepResult.Status.PASS ? first : back;
    }

    static StepResult run(SelfTest.StepContext ctx, List<Probe> probes, boolean clean) {
        Map<String, Object> state = ctx.state();
        if (state.containsKey("result")) {
            return finish(ctx, (StepResult) state.get("result"), clean);
        }
        int index = (int) state.getOrDefault("index", 0);
        if (index >= probes.size()) {
            return clean ? restore(ctx, StepResult.pass()) : StepResult.pass();
        }
        Probe probe = probes.get(index);
        StepResult lang = language(ctx, "lang." + index, probe.language());
        if (lang.status() != StepResult.Status.PASS) {
            return lang.status() == StepResult.Status.FAIL ? finish(ctx, lang, clean) : lang;
        }
        String key = "probe." + index;
        StepResult sent = send(ctx, key, probe.command(), 1);
        if (sent.status() != StepResult.Status.PASS) {
            return sent.status() == StepResult.Status.FAIL ? finish(ctx, sent, clean) : sent;
        }
        StepResult checked = check(ctx, probe, lines(ctx, key));
        if (checked.status() == StepResult.Status.FAIL) {
            return finish(ctx, checked, clean);
        }
        state.put("index", index + 1);
        return StepResult.pending();
    }

    static StepResult finish(SelfTest.StepContext ctx, StepResult result, boolean clean) {
        ctx.state().putIfAbsent("result", result);
        return clean || result.status() == StepResult.Status.FAIL ? restore(ctx, result) : result;
    }

    static StepResult check(SelfTest.StepContext ctx, Probe probe, List<Component> lines) {
        List<String> shown = text(lines);
        if (probe.command().startsWith("propwash code info")) {
            for (String line : shown) {
                if (line.contains(probe.expected())) {
                    ctx.log("/propwash code info (" + probe.language() + "): " + line);
                    return StepResult.pass();
                }
            }
            return StepResult.fail("/propwash code info (" + probe.language() + "): keine Zeile mit „" + probe.expected() + "“ in " + shown);
        }
        for (String line : shown) {
            if (CODE.matcher(line).find()) {
                return StepResult.fail("/" + probe.command() + " (" + probe.language() + "): Sharecode im Chat: " + line);
            }
        }
        if (lines.size() != 1 || !shown.getFirst().equals(probe.expected())) {
            return StepResult.fail("/" + probe.command() + " (" + probe.language() + "): " + shown + " statt [" + probe.expected() + "]");
        }
        List<String> suggested = new ArrayList<>();
        suggestions(lines.getFirst(), suggested);
        if (!suggested.equals(List.of(SUGGESTED))) {
            return StepResult.fail("/" + probe.command() + " (" + probe.language() + "): Vorschläge " + suggested + " statt [" + SUGGESTED + "]");
        }
        ctx.log("/" + probe.command() + " (" + probe.language() + "): „" + shown.getFirst() + "“, Klick schlägt „" + suggested.getFirst() + "“ vor");
        return StepResult.pass();
    }

    private static StepResult hintStep(SelfTest.StepContext ctx) {
        Map<String, Object> state = ctx.state();
        if (!state.containsKey("ready")) {
            StepResult held = hold(ctx);
            if (held.status() != StepResult.Status.PASS) {
                return held;
            }
            state.put("ready", true);
        }
        String en = "Share codes now belong to Propwash: " + SUGGESTED;
        String de = "Sharecodes gehören jetzt zu Propwash: " + SUGGESTED;
        StepResult result = run(ctx, List.of(new Probe(EN, "jmp code export", en), new Probe(EN, "justmoreparts code export", en),
                new Probe(DE, "jmp code export", de), new Probe(DE, "justmoreparts code export", de)), false);
        if (result.status() != StepResult.Status.PENDING) {
            clearHand(ctx);
        }
        return result;
    }

    private static StepResult infoStep(SelfTest.StepContext ctx) {
        Map<String, Object> state = ctx.state();
        if (!state.containsKey("codes")) {
            String race = PropwashApi.shareCodes().encode(JmpPresets.byPath("whoop65_race_1s").data(), ShareOptions.defaults());
            String bash = PropwashApi.shareCodes().encode(DroneData.of(JmpPresets.byPath("bash5_6s").build()), ShareOptions.defaults());
            ctx.log("Codes: Renn-Whoop " + race + ", Bash " + bash);
            state.put("codes", List.of(race, bash));
        }
        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) state.get("codes");
        return run(ctx, List.of(new Probe(DE, "propwash code info " + codes.get(0), "Just More Parts ab 1.2.0"),
                new Probe(DE, "propwash code info " + codes.get(1), "Just More Parts ab 1.0.0"),
                new Probe(EN, "propwash code info " + codes.get(0), "Just More Parts 1.2.0 or newer"),
                new Probe(EN, "propwash code info " + codes.get(1), "Just More Parts 1.0.0 or newer")), true);
    }
}
