package net.geforcemods.securitycraft.api;

import java.util.List;

import net.geforcemods.securitycraft.util.EntityUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * Allows a tile entity to check if an entity is looking at it
 */
public interface IViewActivated {
	/**
	 * Performs checks to determine whether an entity is looking at the tile entity
	 *
	 * @param world The level of the tile entity
	 * @param pos The position of the tile entity
	 */
	default void checkView(World world, BlockPos pos) {
		if (!world.isClientSide) {
			if (getViewCooldown() > 0) {
				setViewCooldown(getViewCooldown() - 1);
				return;
			}

			double maximumDistance = getMaximumDistance();
			List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(pos).inflate(maximumDistance), e -> !e.isSpectator() && !EntityUtils.isInvisible(e) && (!activatedOnlyByPlayer() || e instanceof PlayerEntity));

			for (LivingEntity entity : entities) {
				double eyeHeight = entity.getEyeHeight();
				Vector3d lookVec = new Vector3d(entity.getX() + (entity.getLookAngle().x * maximumDistance), (eyeHeight + entity.getY()) + (entity.getLookAngle().y * maximumDistance), entity.getZ() + (entity.getLookAngle().z * maximumDistance));
				BlockRayTraceResult rtr = world.clip(new RayTraceContext(new Vector3d(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ()), lookVec, BlockMode.COLLIDER, FluidMode.NONE, entity));

				if (rtr != null && rtr.getBlockPos().getX() == pos.getX() && rtr.getBlockPos().getY() == pos.getY() && rtr.getBlockPos().getZ() == pos.getZ() && onEntityViewed(entity, rtr))
					setViewCooldown(getDefaultViewCooldown());
			}
		}
	}

	/**
	 * @return The default amount of ticks to pass between two view checks
	 */
	public default int getDefaultViewCooldown() {
		return 30;
	}

	/**
	 * @return The amount of ticks left before the next view check is performed
	 */
	public int getViewCooldown();

	/**
	 * Sets the ticks left before the next view check
	 *
	 * @param viewCooldown The amount of ticks left before the next view check
	 */
	public void setViewCooldown(int viewCooldown);

	/**
	 * Called when a view check is successful, aka when an entity is looking at this block entity
	 *
	 * @param entity The entity that is looking at this block entity
	 * @param rayTraceResult The context with which the entity is looking at this block entity
	 * @return true if the block entity's view cooldown should be updated
	 */
	public boolean onEntityViewed(LivingEntity entity, BlockRayTraceResult rayTraceResult);

	/**
	 * @return true if the view check should only pass if a player is looking at this block entity, false otherwise
	 */
	public default boolean activatedOnlyByPlayer() {
		return true;
	}

	/**
	 * Returns the maximum distance from which a view check is performed. If an entity is further away than this distance, this
	 * block entity cannot be activated
	 *
	 * @return The maximum distance in blocks from which a view check is performed
	 */
	public double getMaximumDistance();
}
