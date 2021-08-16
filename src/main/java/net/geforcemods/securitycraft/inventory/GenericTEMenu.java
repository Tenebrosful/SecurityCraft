package net.geforcemods.securitycraft.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GenericTEMenu extends AbstractContainerMenu {
	public final BlockEntity te;
	private ContainerLevelAccess worldPosCallable;

	public GenericTEMenu(MenuType<GenericTEMenu> type, int windowId, Level world, BlockPos pos)
	{
		super(type, windowId);

		te = world.getBlockEntity(pos);
		worldPosCallable = ContainerLevelAccess.create(world, pos);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(worldPosCallable, player, te.getBlockState().getBlock());
	}
}