package net.geforcemods.securitycraft.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.geforcemods.securitycraft.entity.SecurityCameraEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * SecurityCamera - Geforce
 * Created using Tabula 4.1.1
 */
@OnlyIn(Dist.CLIENT)
public class SecurityCameraModel extends EntityModel<SecurityCameraEntity> {
	public ModelPart shape1;
	public ModelPart shape2;
	public ModelPart cameraRotationPoint;
	public ModelPart shape3;
	public ModelPart cameraBody;
	public ModelPart cameraLensRight;
	public ModelPart cameraLensLeft;
	public ModelPart cameraLensTop;

	public SecurityCameraModel() {
		texWidth = 128;
		texHeight = 64;
		cameraRotationPoint = new ModelPart(this, 0, 25);
		cameraRotationPoint.setPos(0.0F, 14.0F, 3.0F);
		cameraRotationPoint.addBox(0.0F, 0.0F, 0.0F, 1, 1, 1);
		setRotateAngle(cameraRotationPoint, 0.2617993877991494F, 0.0F, 0.0F);
		cameraLensRight = new ModelPart(this, 10, 40);
		cameraLensRight.setPos(3.0F, 0.0F, -3.0F);
		cameraLensRight.addBox(-2.0F, 0.0F, 0.0F, 1, 3, 1);
		shape3 = new ModelPart(this, 1, 12);
		shape3.setPos(0.0F, 1.0F, 0.0F);
		shape3.addBox(0.0F, 0.0F, 0.0F, 2, 1, 7);
		cameraLensLeft = new ModelPart(this, 0, 40);
		cameraLensLeft.setPos(-2.0F, 0.0F, -3.0F);
		cameraLensLeft.addBox(0.0F, 0.0F, 0.0F, 1, 3, 1);
		cameraBody = new ModelPart(this, 0, 25);
		cameraBody.setPos(0.0F, 0.0F, -5.0F);
		cameraBody.addBox(-2.0F, 0.0F, -2.0F, 4, 3, 8);
		setRotateAngle(cameraBody, 0.2617993877991494F, 0.0F, 0.0F);
		shape1 = new ModelPart(this, 0, 0);
		shape1.setPos(-3.0F, 13.0F, 7.0F);
		shape1.addBox(0.0F, 0.0F, 0.0F, 6, 6, 1);
		cameraLensTop = new ModelPart(this, 20, 40);
		cameraLensTop.setPos(-1.0F, 0.0F, -3.0F);
		cameraLensTop.addBox(0.0F, 0.0F, 0.0F, 2, 1, 1);
		shape2 = new ModelPart(this, 2, 12);
		shape2.setPos(-1.0F, 13.75F, 2.25F);
		shape2.addBox(0.0F, 0.0F, 0.0F, 2, 1, 6);
		setRotateAngle(shape2, -0.5235987755982988F, 0.0F, 0.0F);
		cameraBody.addChild(cameraLensRight);
		shape2.addChild(shape3);
		cameraBody.addChild(cameraLensLeft);
		cameraRotationPoint.addChild(cameraBody);
		cameraBody.addChild(cameraLensTop);
	}

	@Override
	public void renderToBuffer(PoseStack matrix, VertexConsumer builder, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
	{
		cameraRotationPoint.render(matrix, builder, packedLight, packedOverlay, red, green, blue, alpha);
		shape1.render(matrix, builder, packedLight, packedOverlay, red, green, blue, alpha);
		shape2.render(matrix, builder, packedLight, packedOverlay, red, green, blue, alpha);
	}

	/**
	 * This is a helper function from Tabula to set the rotation of model parts
	 */
	public void setRotateAngle(ModelPart modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}

	@Override
	public void setupAnim(SecurityCameraEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}
}