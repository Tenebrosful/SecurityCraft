package net.geforcemods.securitycraft.inventory;

import net.geforcemods.securitycraft.util.BlockUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

public class GenericMenu extends Container {
	private TileEntity te;

	public GenericMenu() {}

	public GenericMenu(TileEntity te) {
		this.te = te;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		//this is also used for items (e.g. Briefcase), so the te can be null
		return te == null || BlockUtils.isWithinUsableDistance(te.getWorld(), te.getPos(), player, te.getBlockType());
	}
}
