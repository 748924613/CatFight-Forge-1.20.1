package com.catfight.client.model;

import com.catfight.client.CatFightClientPose;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

import java.util.List;

/** Adds the raised, segmented "laowu cat" stance after vanilla has animated the cat. */
public final class CatFightCatModel extends CatModel<Cat> {
    private final ModelPart archFront;
    private final ModelPart archRear;
    private final ModelPart archSpine;
    private final ModelPart pancakeHead;
    private final ModelPart pancakeBody;
    private final ModelPart pancakeLeftFrontLeg;
    private final ModelPart pancakeRightFrontLeg;
    private final ModelPart pancakeLeftHindLeg;
    private final ModelPart pancakeRightHindLeg;
    private final ModelPart pancakeTail;

    public CatFightCatModel(ModelPart root) {
        super(root);
        this.archFront = root.getChild("catfight_arch_front");
        this.archRear = root.getChild("catfight_arch_rear");
        this.archSpine = root.getChild("catfight_arch_spine");
        this.pancakeHead = root.getChild("catfight_pancake_head");
        this.pancakeBody = root.getChild("catfight_pancake_body");
        this.pancakeLeftFrontLeg = root.getChild("catfight_pancake_left_front_leg");
        this.pancakeRightFrontLeg = root.getChild("catfight_pancake_right_front_leg");
        this.pancakeLeftHindLeg = root.getChild("catfight_pancake_left_hind_leg");
        this.pancakeRightHindLeg = root.getChild("catfight_pancake_right_hind_leg");
        this.pancakeTail = root.getChild("catfight_pancake_tail");
    }

    /**
     * Keeps the vanilla body for normal cats and adds two separate upper body
     * pieces for the raised fighting pose.  The legacy spine part stays in the
     * layer for compatibility, but is deliberately hidden while fighting: a
     * free-standing cube made the crest look like a missing/incorrect patch.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = OcelotModel.createBodyMesh(CubeDeformation.NONE);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("catfight_arch_front",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 8.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("catfight_arch_rear",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 8.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("catfight_arch_spine",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.0F, -1.5F, -2.5F, 4.0F, 3.0F, 5.0F),
                PartPose.ZERO);
        // The cat pancake is intentionally a separate, one-pixel-thin top-down silhouette.
        // Its renderer selects a dedicated, opaque 64x32 texture atlas while pancaked, so
        // these intentionally broad cuboids never repeat vanilla paws or transparent padding.
        root.addOrReplaceChild("catfight_pancake_head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 22.94F, -8.0F));
        root.addOrReplaceChild("catfight_pancake_body",
                CubeListBuilder.create().texOffs(25, 0).addBox(-2.0F, 0.0F, -5.0F, 4.0F, 1.0F, 10.0F),
                PartPose.offset(0.0F, 22.94F, 0.0F));
        root.addOrReplaceChild("catfight_pancake_left_front_leg",
                CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, 0.0F, -1.0F, 5.0F, 1.0F, 2.0F),
                PartPose.offset(2.0F, 22.94F, -3.0F));
        root.addOrReplaceChild("catfight_pancake_right_front_leg",
                CubeListBuilder.create().texOffs(15, 12).addBox(-5.0F, 0.0F, -1.0F, 5.0F, 1.0F, 2.0F),
                PartPose.offset(-2.0F, 22.94F, -3.0F));
        root.addOrReplaceChild("catfight_pancake_left_hind_leg",
                CubeListBuilder.create().texOffs(30, 12).addBox(0.0F, 0.0F, -1.0F, 5.0F, 1.0F, 2.0F),
                PartPose.offset(2.0F, 22.94F, 3.0F));
        root.addOrReplaceChild("catfight_pancake_right_hind_leg",
                CubeListBuilder.create().texOffs(45, 12).addBox(-5.0F, 0.0F, -1.0F, 5.0F, 1.0F, 2.0F),
                PartPose.offset(-2.0F, 22.94F, 3.0F));
        root.addOrReplaceChild("catfight_pancake_tail",
                CubeListBuilder.create().texOffs(0, 16).addBox(-0.75F, 0.0F, 0.0F, 1.5F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 22.94F, 5.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return List.of(this.head, this.pancakeHead);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return List.of(this.body, this.leftHindLeg, this.rightHindLeg, this.leftFrontLeg, this.rightFrontLeg,
                this.tail1, this.tail2, this.archFront, this.archRear, this.archSpine,
                this.pancakeBody, this.pancakeLeftFrontLeg, this.pancakeRightFrontLeg,
                this.pancakeLeftHindLeg, this.pancakeRightHindLeg, this.pancakeTail);
    }

    @Override
    public void prepareMobModel(Cat cat, float limbSwing, float limbSwingAmount, float partialTick) {
        // Model instances are shared by every rendered cat.  Reset our edited parts before
        // vanilla prepares the next frame, otherwise position and rotation offsets accumulate.
        this.head.resetPose();
        this.body.resetPose();
        this.leftFrontLeg.resetPose();
        this.rightFrontLeg.resetPose();
        this.leftHindLeg.resetPose();
        this.rightHindLeg.resetPose();
        this.tail1.resetPose();
        this.tail2.resetPose();
        this.archFront.resetPose();
        this.archRear.resetPose();
        this.archSpine.resetPose();
        this.pancakeHead.resetPose();
        this.pancakeBody.resetPose();
        this.pancakeLeftFrontLeg.resetPose();
        this.pancakeRightFrontLeg.resetPose();
        this.pancakeLeftHindLeg.resetPose();
        this.pancakeRightHindLeg.resetPose();
        this.pancakeTail.resetPose();
        this.head.visible = true;
        this.body.visible = true;
        this.leftFrontLeg.visible = true;
        this.rightFrontLeg.visible = true;
        this.leftHindLeg.visible = true;
        this.rightHindLeg.visible = true;
        this.tail1.visible = true;
        this.tail2.visible = true;
        this.archFront.visible = false;
        this.archRear.visible = false;
        this.archSpine.visible = false;
        this.pancakeHead.visible = false;
        this.pancakeBody.visible = false;
        this.pancakeLeftFrontLeg.visible = false;
        this.pancakeRightFrontLeg.visible = false;
        this.pancakeLeftHindLeg.visible = false;
        this.pancakeRightHindLeg.visible = false;
        this.pancakeTail.visible = false;
        super.prepareMobModel(cat, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(Cat cat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
        super.setupAnim(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Replace every ordinary part with the deliberate top-down cat-pancake shape.
        // This is a fixed pose: a flattened cat cannot turn, walk or arch its back.
        if (CatFightClientPose.isPancaked(cat)) {
            this.head.visible = false;
            this.body.visible = false;
            this.leftFrontLeg.visible = false;
            this.rightFrontLeg.visible = false;
            this.leftHindLeg.visible = false;
            this.rightHindLeg.visible = false;
            this.tail1.visible = false;
            this.tail2.visible = false;
            this.archFront.visible = false;
            this.archRear.visible = false;
            this.archSpine.visible = false;
            this.pancakeHead.visible = true;
            this.pancakeBody.visible = true;
            this.pancakeLeftFrontLeg.visible = true;
            this.pancakeRightFrontLeg.visible = true;
            this.pancakeLeftHindLeg.visible = true;
            this.pancakeRightHindLeg.visible = true;
            this.pancakeTail.visible = true;
            return;
        }

        if (!CatFightClientPose.isFighting(cat)) {
            return;
        }

        float lean = CatFightClientPose.leanDirection(cat);

        // Only the upper exaggerated arched pieces are shown during a confrontation.
        this.body.visible = false;
        this.archFront.visible = true;
        this.archRear.visible = true;
        // The previous independent 4x3x5 spine cube created the odd white block
        // at the crest.  Overlap the two real back pieces instead, so their own
        // cat texture forms one continuous arch.
        this.archSpine.visible = false;
        this.archFront.setPos(0.0F, 11.2F, -7.0F);
        this.archFront.setRotation(0.18F, 0.0F, lean * 0.03F);
        this.archRear.setPos(0.0F, 9.77F, -0.20F);
        this.archRear.setRotation(-0.18F, 0.0F, lean * 0.03F);

        // The body and legs are root-level vanilla parts, so they do not inherit
        // the raised arch automatically.  Re-anchor and scale the existing legs
        // in place: their feet stay on the ground while their shoulders meet the
        // arched body, without bringing back a second low body underneath.
        placeFightLeg(this.leftFrontLeg, 1.2F, 13.90F, -5.0F, 1.02F);
        placeFightLeg(this.rightFrontLeg, -1.2F, 13.90F, -5.0F, 1.02F);
        placeFightLeg(this.leftHindLeg, 1.1F, 13.77F, 5.0F, 1.71F);
        placeFightLeg(this.rightHindLeg, -1.1F, 13.77F, 5.0F, 1.71F);

        // Hold the head high and tilted throughout a confrontation instead of letting
        // vanilla look/idle animation lower it again.  A small clamped yaw keeps it aimed
        // toward the opponent while the roll stays visibly tilted and stable for each cat.
        float fightHeadYaw = Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.35F, 0.35F);
        this.head.resetPose();
        this.head.visible = true;
        this.head.xScale = 1.0F;
        this.head.yScale = 1.0F;
        this.head.zScale = 1.0F;
        this.head.setPos(0.0F, 10.35F, -8.80F);
        this.head.setRotation(-0.48F, fightHeadYaw, -lean * 0.30F);
        this.tail1.setPos(0.0F, 11.2F, 6.80F);
        this.tail1.setRotation(1.20F, 0.0F, lean * 0.03F);
        this.tail2.setPos(0.0F, 14.1F, 14.26F);
        this.tail2.setRotation(0.80F, 0.0F, lean * 0.03F);
    }

    private static void placeFightLeg(ModelPart leg, float x, float y, float z, float yScale) {
        leg.setPos(x, y, z);
        leg.setRotation(0.0F, 0.0F, 0.0F);
        leg.xScale = 1.0F;
        leg.yScale = yScale;
        leg.zScale = 1.0F;
    }

}
