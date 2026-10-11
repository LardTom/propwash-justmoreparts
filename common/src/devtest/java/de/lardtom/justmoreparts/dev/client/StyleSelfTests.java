package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.part.FrameDefinition;
import de.lardtom.propwash.api.part.MotorArm;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StyleSelfTests {
    public static final List<PartKind> KINDS = List.of(PartKind.MOTOR, PartKind.PROP, PartKind.BATTERY, PartKind.STACK, PartKind.FRAME, PartKind.VIDEO);
    private static final List<ItemDisplayContext> CONTEXTS = List.of(ItemDisplayContext.GUI, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            ItemDisplayContext.GROUND, ItemDisplayContext.FIXED, ItemDisplayContext.HEAD, ItemDisplayContext.ON_SHELF);
    private static final double LOWER = 0.9;
    private static final double UPPER = 1.1;
    private static final double MM_PER_BLOCK = 128.0;
    private static final double HAND_FULL_MM = 180.0;
    private static final double HAND_EXPONENT = 0.6;
    private static final double HAND_MIN_FACTOR = 0.25;

    private StyleSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("stil: alle Teil-Items mit eigenem Modell statt Sonder-Renderer", 40, ctx -> modelsStep(ctx));
        SelfTest.clientStep("stil: Größe je Ansicht im Bereich der Propwash-Teile", 40, ctx -> sizesStep(ctx));
        SelfTest.clientStep("stil: Drohnen-Modell für jedes Teil, Rahmen in echter Größe", 40, ctx -> droneModelsStep(ctx));
    }

    private static StepResult droneModelsStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<String> problems = new ArrayList<>();
        ItemStackRenderState state = new ItemStackRenderState();
        int checked = 0;
        String widest = "";
        double widestMm = 0.0;
        for (PartDefinition definition : parts(JustMoreParts.MOD_ID, null)) {
            Identifier id = definition.id();
            Identifier render = Identifier.fromNamespaceAndPath(id.getNamespace(), "drone_render/" + definition.kind().key() + "/" + id.getPath());
            ItemModel model = mc.getModelManager().getItemModel(render);
            if (model instanceof MissingItemModel) {
                problems.add(id.getPath() + " ohne Drohnen-Modell");
                continue;
            }
            checked++;
            if (!(definition instanceof FrameDefinition frame)) {
                continue;
            }
            state.clear();
            model.update(state, ItemStack.EMPTY, mc.getItemModelResolver(), ItemDisplayContext.NONE, null, null, 0);
            AABB box = state.getModelBoundingBox();
            double reach = 0.0;
            for (MotorArm arm : frame.geometry().armsOrTrueX(frame.wheelbaseMm())) {
                reach = Math.max(reach, Math.max(Math.abs(arm.position().x()), Math.abs(arm.position().z())));
            }
            double halfX = Math.max(-box.minX, box.maxX) * MM_PER_BLOCK;
            double halfZ = Math.max(-box.minZ, box.maxZ) * MM_PER_BLOCK;
            double half = Math.max(halfX, halfZ);
            if (half < reach || half > reach + 60.0) {
                problems.add(String.format(Locale.ROOT, "%s Drohnen-Modell %.0f mm statt %.0f–%.0f mm vom Anbaupunkt", id.getPath(), half,
                        reach, reach + 60.0));
            }
            if (half > widestMm) {
                widestMm = half;
                widest = id.getPath();
            }
        }
        ctx.log(String.format(Locale.ROOT, "%d JMP-Teile mit Drohnen-Modell, größter Rahmen %s mit %.0f mm vom Anbaupunkt", checked,
                widest.isEmpty() ? "keiner" : widest, widestMm));
        return problems.isEmpty() ? StepResult.pass() : StepResult.fail(String.join("; ", problems.subList(0, Math.min(12, problems.size()))));
    }

    private static StepResult modelsStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (PartDefinition definition : parts(JustMoreParts.MOD_ID, null)) {
            ItemStack stack = PropwashApi.partItems().stack(definition.id());
            Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
            if (stack.isEmpty() || modelId == null) {
                problems.add(definition.id().getPath() + " ohne Item");
                continue;
            }
            ItemModel model = mc.getModelManager().getItemModel(modelId);
            if (model instanceof MissingItemModel) {
                problems.add(definition.id().getPath() + " ohne Item-Modell");
            } else if (model instanceof SpecialModelWrapper) {
                problems.add(definition.id().getPath() + " noch mit Sonder-Renderer");
            }
            checked++;
        }
        ctx.log(checked + " JMP-Teil-Items mit eigenem Item-Modell");
        if (checked != JustMoreParts.items()) {
            problems.add(checked + " geprüft, aber " + JustMoreParts.items() + " Teil-Items");
        }
        return problems.isEmpty() ? StepResult.pass() : StepResult.fail(String.join("; ", problems.subList(0, Math.min(12, problems.size()))));
    }

    private static StepResult sizesStep(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return StepResult.fail("keine Welt");
        }
        List<String> problems = new ArrayList<>();
        ItemStackRenderState state = new ItemStackRenderState();
        for (PartKind kind : KINDS) {
            Map<ItemDisplayContext, double[]> base = new EnumMap<>(ItemDisplayContext.class);
            for (PartDefinition definition : parts(JustMoreParts.PROPWASH_ID, kind)) {
                ItemStack stack = PropwashApi.partItems().stack(definition.id());
                double factor = handFactor(mc, state, definition);
                for (ItemDisplayContext context : CONTEXTS) {
                    double size = size(mc, state, stack, context) / (scaled(kind, context) ? factor : 1.0);
                    double[] range = base.computeIfAbsent(context, c -> new double[]{Double.MAX_VALUE, 0.0});
                    range[0] = Math.min(range[0], size);
                    range[1] = Math.max(range[1], size);
                }
            }
            if (base.isEmpty()) {
                problems.add("keine Propwash-Teile der Art " + kind.key());
                continue;
            }
            double worst = 1.0;
            String worstAt = "";
            int count = 0;
            for (PartDefinition definition : parts(JustMoreParts.MOD_ID, kind)) {
                ItemStack stack = PropwashApi.partItems().stack(definition.id());
                double factor = handFactor(mc, state, definition);
                count++;
                for (ItemDisplayContext context : CONTEXTS) {
                    double[] range = base.get(context);
                    double size = size(mc, state, stack, context) / (scaled(kind, context) ? factor : 1.0);
                    double ratio = size > range[1] ? size / range[1] : size < range[0] ? size / range[0] : 1.0;
                    if (Math.abs(Math.log(ratio)) > Math.abs(Math.log(worst))) {
                        worst = ratio;
                        worstAt = definition.id().getPath() + " " + context.getSerializedName();
                    }
                    if (size > range[1] * UPPER || size < range[0] * LOWER) {
                        problems.add(String.format(Locale.ROOT, "%s %s %.3f außerhalb %.3f–%.3f", definition.id().getPath(),
                                context.getSerializedName(), size, range[0], range[1]));
                    }
                }
            }
            double[] hand = base.get(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            double[] gui = base.get(ItemDisplayContext.GUI);
            ctx.log(String.format(Locale.ROOT, "%s: %d JMP-Teile, Propwash erste Person %.3f–%.3f (%s), GUI %.3f–%.3f, größte Abweichung %s (×%.2f)",
                    kind.key(), count, hand[0], hand[1], scaled(kind, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) ? "geteilt durch den Handfaktor" : "ohne Handfaktor", gui[0], gui[1], worstAt.isEmpty() ? "keine" : worstAt, worst));
        }
        return problems.isEmpty() ? StepResult.pass() : StepResult.fail(problems.size() + " Größen außerhalb der Propwash-Spanne: "
                + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));
    }

    private static boolean scaled(PartKind kind, ItemDisplayContext context) {
        boolean first = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean third = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        return third || first && kind != PartKind.BATTERY;
    }

    private static double handFactor(Minecraft mc, ItemStackRenderState state, PartDefinition definition) {
        Identifier id = definition.id();
        ItemModel model = mc.getModelManager().getItemModel(Identifier.fromNamespaceAndPath(id.getNamespace(), "drone_render/" + definition.kind().key() + "/" + id.getPath()));
        if (model instanceof MissingItemModel) {
            return 1.0;
        }
        state.clear();
        model.update(state, ItemStack.EMPTY, mc.getItemModelResolver(), ItemDisplayContext.NONE, null, null, 0);
        AABB box = state.getModelBoundingBox();
        double length = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * MM_PER_BLOCK;
        if (!(length > 0.0)) {
            return 1.0;
        }
        return Math.clamp(Math.pow(length / HAND_FULL_MM, HAND_EXPONENT), HAND_MIN_FACTOR, 1.0);
    }

    private static double size(Minecraft mc, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context) {
        mc.getItemModelResolver().updateForTopItem(state, stack, context, mc.level, null, 0);
        AABB box = state.getModelBoundingBox();
        double view = Math.max(box.getXsize(), box.getYsize());
        return context == ItemDisplayContext.GUI ? view : Math.max(view, box.getZsize());
    }

    private static List<PartDefinition> parts(String namespace, @Nullable PartKind kind) {
        List<PartDefinition> list = new ArrayList<>();
        for (PartDefinition definition : PropwashApi.parts().all()) {
            if (namespace.equals(definition.id().getNamespace()) && (kind == null || definition.kind() == kind)
                    && KINDS.contains(definition.kind()) && !PropwashApi.partItems().stack(definition.id()).isEmpty()) {
                list.add(definition);
            }
        }
        return list;
    }
}
