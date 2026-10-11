package de.lardtom.justmoreparts.frame.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.guide.scene.ActorSpec;
import de.lardtom.propwash.guide.scene.Anchor;
import de.lardtom.propwash.guide.scene.BlockSpec;
import de.lardtom.propwash.guide.scene.Cue;
import de.lardtom.propwash.guide.scene.Ease;
import de.lardtom.propwash.guide.scene.Scene;
import de.lardtom.propwash.guide.scene.SceneBuilder;
import de.lardtom.propwash.guide.scene.ScenePos;

import net.minecraft.resources.Identifier;

import java.util.List;

public final class FrameShapesScene {
    public static final String ID = JustMoreParts.MOD_ID + ":frames";
    public static final String TRUE_X = "truex";
    public static final String STRETCHED = "stretched";
    public static final String SQUASHED = "squashed";
    public static final String DEADCAT = "deadcat";
    public static final String H_FRAME = "hframe";
    public static final String TAG_PREFIX = "tag_";
    public static final List<String> STEPS = List.of("lineup", STRETCHED, SQUASHED, DEADCAT, H_FRAME);
    public static final List<String> TAGS = List.of(TAG_PREFIX + TRUE_X, TAG_PREFIX + STRETCHED, TAG_PREFIX + SQUASHED, TAG_PREFIX + DEADCAT,
            TAG_PREFIX + H_FRAME);
    public static final List<String> CALLOUTS = List.of(STRETCHED, SQUASHED, DEADCAT, H_FRAME, TAGS.get(0), TAGS.get(1), TAGS.get(2),
            TAGS.get(3), TAGS.get(4));
    public static final float LINEUP_TICKS = 150.0F;
    public static final float TAGS_AT = 50.0F;
    public static final float STRETCHED_CALLOUT_AT = 20.0F;
    public static final float STRETCHED_REST_AT = 70.0F;

    private static final float YAW = 180.0F;
    private static final float FINAL_TAGS_AT = 90.0F;
    private static final float STEP_YAW = 40.0F;
    private static final float OVERVIEW_YAW = 0.0F;
    private static final float OVERVIEW_PITCH = 64.0F;
    private static final float OVERVIEW_ZOOM = 1.2F;
    private static final float HOVER = 1.0F;
    private static final float SPACING = 1.5F;
    private static final float FAST = 14.0F;
    private static final float SLOW = 22.0F;
    private static final BlockSpec GROUND = BlockSpec.of("minecraft:gray_concrete");
    private static final ScenePos OVERVIEW = new ScenePos(0.0F, 1.1F, 0.0F);

    private FrameShapesScene() {
    }

    public record Actor(String name, String frame, ScenePos position) {
        public BuildSpec build() {
            return FrameShapesScene.build(this.frame);
        }
    }

    public static List<Actor> actors() {
        return List.of(new Actor(TRUE_X, "propwash:freestyle_5", pos(-2)), new Actor(STRETCHED, "justmoreparts:race_5_stretched", pos(-1)),
                new Actor(SQUASHED, "justmoreparts:fs_5_squashed", pos(0)), new Actor(DEADCAT, "justmoreparts:deadcat_5", pos(1)),
                new Actor(H_FRAME, "justmoreparts:hframe_5", pos(2)));
    }

    public static BuildSpec build(String frame) {
        return BuildSpec.of(Identifier.parse(frame), JustMoreParts.propwash("stack_30"),
                JustMoreParts.propwash("m2207_1750"), JustMoreParts.propwash("p51_3b"), JustMoreParts.propwash("digital_hd"),
                JustMoreParts.propwash("b6s_1300"));
    }

    public static Scene create() {
        List<Actor> actors = actors();
        SceneBuilder scene = SceneBuilder.scene(ID)
                .icon(JustMoreParts.id("deadcat_5").toString())
                .view(3.9F)
                .camera(OVERVIEW, OVERVIEW_YAW, OVERVIEW_PITCH, OVERVIEW_ZOOM);
        scene.step("lineup", LINEUP_TICKS, s -> {
            s.at(0.0F).floor(-4, -2, 4, 2, 0, GROUND, 1.0F, 10.0F);
            float at = 12.0F;
            for (Actor actor : actors) {
                ActorSpec.Drone drone = PropwashClientApi.flightSchoolDrone(actor.build(), true, ActorSpec.Drone.STAGE_SCALE);
                s.at(at).spawn(actor.name(), drone, actor.position(), YAW, Cue.Appear.POP, 10.0F);
                at += 6.0F;
            }
            tags(s, actors, TAGS_AT);
        });
        scene.step(STRETCHED, 150.0F, s -> {
            ScenePos at = actors.get(1).position();
            s.at(0.0F).camera(at.add(0.0F, 0.4F, 0.0F), STEP_YAW, 22.0F, 1.5F, 20.0F);
            s.at(STRETCHED_CALLOUT_AT).callout(STRETCHED, above(STRETCHED), Cue.Side.ABOVE, 0.0F);
            fly(s, STRETCHED, 22.0F, 128.0F);
            trick(s, STRETCHED, at, 30.0F, false, FAST);
            trick(s, STRETCHED, at, 80.0F, true, SLOW);
        });
        scene.step(SQUASHED, 150.0F, s -> {
            ScenePos at = actors.get(2).position();
            s.at(0.0F).camera(at.add(0.0F, 0.4F, 0.0F), STEP_YAW, 22.0F, 1.5F, 20.0F);
            s.at(20.0F).callout(SQUASHED, above(SQUASHED), Cue.Side.ABOVE, 0.0F);
            fly(s, SQUASHED, 22.0F, 128.0F);
            trick(s, SQUASHED, at, 30.0F, true, FAST);
            trick(s, SQUASHED, at, 80.0F, false, SLOW);
        });
        scene.step(DEADCAT, 120.0F, s -> {
            ScenePos at = actors.get(3).position();
            s.at(0.0F).camera(at.add(0.0F, 0.3F, 0.0F), STEP_YAW, 40.0F, 1.7F, 24.0F);
            s.at(26.0F).callout(DEADCAT, above(DEADCAT), Cue.Side.ABOVE, 0.0F);
        });
        scene.step(H_FRAME, 120.0F, s -> {
            ScenePos at = actors.get(4).position();
            s.at(0.0F).camera(at.add(0.0F, 0.3F, 0.0F), STEP_YAW, 40.0F, 1.7F, 24.0F);
            s.at(26.0F).callout(H_FRAME, above(H_FRAME), Cue.Side.ABOVE, FINAL_TAGS_AT - 2.0F - 26.0F);
            s.at(66.0F).camera(OVERVIEW, OVERVIEW_YAW, OVERVIEW_PITCH, OVERVIEW_ZOOM, 30.0F);
            tags(s, actors, FINAL_TAGS_AT);
        });
        return scene.build();
    }

    private static void tags(SceneBuilder.StepBuilder s, List<Actor> actors, float at) {
        for (int i = 0; i < actors.size(); i++) {
            String actor = actors.get(i).name();
            s.at(at + 2.0F * i).callout(TAG_PREFIX + actor, Anchor.on(actor, 0.0F, 0.2F, -0.3F), Cue.Side.ABOVE, 0.0F);
        }
    }

    private static void fly(SceneBuilder.StepBuilder s, String actor, float start, float end) {
        s.at(start).arm(actor, true);
        s.at(start).throttle(actor, 0.3F, 8.0F);
        s.at(end).throttle(actor, 0.0F, 8.0F);
        s.at(end + 10.0F).arm(actor, false);
    }

    private static void trick(SceneBuilder.StepBuilder s, String actor, ScenePos at, float start, boolean flip, float length) {
        ScenePos up = at.add(0.0F, 0.6F, 0.0F);
        s.at(start).move(actor, 10.0F, Ease.OUT, up);
        s.at(start + 10.0F).turn(actor, YAW, flip ? 360.0F : 0.0F, flip ? 0.0F : 360.0F, length, Ease.IN_OUT);
        s.at(start + 10.0F + length).turn(actor, YAW, 0.0F, 0.0F, 0.0F);
        s.at(start + 12.0F + length).move(actor, 10.0F, Ease.IN_OUT, at);
    }

    private static Anchor above(String actor) {
        return Anchor.on(actor, 0.0F, 0.25F, 0.0F);
    }

    private static ScenePos pos(int slot) {
        return new ScenePos(slot * SPACING, HOVER, 0.0F);
    }
}
