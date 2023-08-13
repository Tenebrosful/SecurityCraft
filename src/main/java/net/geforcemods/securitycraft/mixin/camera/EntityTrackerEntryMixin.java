package net.geforcemods.securitycraft.mixin.camera;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.geforcemods.securitycraft.entity.camera.SecurityCamera;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Allows security cameras as well as entities outside the player's range to be sent to the player when they are viewing a
 * camera
 */
@Mixin(value = EntityTrackerEntry.class, priority = 1100)
public class EntityTrackerEntryMixin {
	@Shadow
	@Final
	private int range;
	@Shadow
	private int maxRange;
	@Shadow
	private long encodedPosX;
	@Shadow
	private long encodedPosZ;

	@Redirect(method = "updatePlayerEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityTrackerEntry;isVisibleTo(Lnet/minecraft/entity/player/EntityPlayerMP;)Z"))
	private boolean securitycraft$shouldUpdate(EntityTrackerEntry entry, EntityPlayerMP player) {
		if (entry.getTrackedEntity() instanceof SecurityCamera)
			return true;

		if (PlayerUtils.isPlayerMountedOnCamera(player)) {
			SecurityCamera cam = (SecurityCamera) player.getSpectatingEntity();
			double relativeX = cam.posX - encodedPosX / 4096.0D;
			double relativeZ = cam.posZ - encodedPosZ / 4096.0D;
			int adjustedRange = Math.min(range, maxRange);

			return relativeX >= -adjustedRange && relativeX <= adjustedRange && relativeZ >= -adjustedRange && relativeZ <= adjustedRange;
		}

		return entry.isVisibleTo(player);
	}
}
