package net.geforcemods.securitycraft.containers;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.inventory.ModuleItemInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ContainerDisguiseModule extends Container {

	private ModuleItemInventory inventory;

	public ContainerDisguiseModule(InventoryPlayer playerInventory, ModuleItemInventory moduleInventory) {
		inventory = moduleInventory;
		addSlotToContainer(new AddonSlot(inventory, 0, 79, 20));

		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));

		for(int i = 0; i < 9; i++)
			addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 142));
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot slot = inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			slotStackCopy = slotStack.copy();

			if(index < inventory.SIZE) {
				if(!mergeItemStack(slotStack, inventory.SIZE, 37, true))
					return ItemStack.EMPTY;

				slot.onSlotChange(slotStack, slotStackCopy);
			}
			else if(index >= inventory.SIZE)
				if(!mergeItemStack(slotStack, 0, inventory.SIZE, false))
					return ItemStack.EMPTY;

			if(slotStack.getCount() == 0)
				slot.putStack(ItemStack.EMPTY);
			else
				slot.onSlotChanged();

			if(slotStack.getCount() == slotStackCopy.getCount())
				return ItemStack.EMPTY;

			slot.onTake(player, slotStack);
		}

		return slotStackCopy;
	}

	@Override
	public ItemStack slotClick(int slot, int dragType, ClickType clickType, EntityPlayer player)
	{
		if(slot >= 0 && getSlot(slot) != null && (!player.getHeldItemMainhand().isEmpty() && getSlot(slot).getStack() == player.getHeldItemMainhand() && player.getHeldItemMainhand().getItem() == SCContent.disguiseModule))
			return ItemStack.EMPTY;

		return super.slotClick(slot, dragType, clickType, player);
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}


	public static class AddonSlot extends Slot {

		private ModuleItemInventory inventory;

		public AddonSlot(ModuleItemInventory inventory, int index, int xPos, int yPos) {
			super(inventory, index, xPos, yPos);
			this.inventory = inventory;
		}

		@Override
		public boolean isItemValid(ItemStack itemStack) {
			int numberOfItems = 0;
			int numberOfBlocks = 0;
			Item item = itemStack.getItem();
			boolean isStackBlock = item instanceof ItemBlock;

			for(ItemStack stack : inventory.moduleInventory)
				if(!stack.isEmpty())
					if(stack.getItem() instanceof ItemBlock)
						numberOfBlocks++;
					else if(stack.getItem() != null)
						numberOfItems++;

			return (isStackBlock && numberOfBlocks < inventory.maxNumberOfBlocks) || (!isStackBlock && numberOfItems < inventory.maxNumberOfItems);
		}

		@Override
		public int getSlotStackLimit() {
			return 1;
		}
	}

}