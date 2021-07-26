package net.geforcemods.securitycraft.renderers;

import com.mojang.blaze3d.vertex.PoseStack;

import net.geforcemods.securitycraft.entity.IMSBombEntity;
import net.geforcemods.securitycraft.models.IMSBombModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IMSBombRenderer extends EntityRenderer<IMSBombEntity> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("securitycraft:textures/entity/ims_bomb.png");
	protected static final IMSBombModel MODEL = new IMSBombModel();

	public IMSBombRenderer(EntityRendererProvider.Context ctx){
		super(ctx);
	}

	@Override
	public void render(IMSBombEntity imsBomb, float p_225623_2_, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int p_225623_6_)
	{
		matrix.translate(-0.1D, 0, 0.1D);
		matrix.scale(1.4F, 1.4F, 1.4F);
		Minecraft.getInstance().textureManager.bindForSetup(getTextureLocation(imsBomb));
		MODEL.renderToBuffer(matrix, buffer.getBuffer(RenderType.entitySolid(getTextureLocation(imsBomb))), p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(IMSBombEntity imsBomb) {
		return TEXTURE;
	}
}