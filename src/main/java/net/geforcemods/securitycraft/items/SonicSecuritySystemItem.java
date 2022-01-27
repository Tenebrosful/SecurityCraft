package net.geforcemods.securitycraft.items;

import java.util.List;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.ILockable;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.blockentities.SonicSecuritySystemBlockEntity;
import net.geforcemods.securitycraft.blocks.DisguisableBlock;
import net.geforcemods.securitycraft.network.client.UpdateNBTTagOnClient;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.PacketDistributor;

public class SonicSecuritySystemItem extends BlockItem {
	public SonicSecuritySystemItem(Properties properties) {
		super(SCContent.SONIC_SECURITY_SYSTEM.get(), properties);
	}

	@Override
	public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext ctx) {
		return onItemUseFirst(ctx.getPlayer(), ctx.getLevel(), ctx.getClickedPos(), stack, ctx.getClickedFace(), ctx.getClickLocation().x, ctx.getClickLocation().y, ctx.getClickLocation().z);
	}

	public ActionResultType onItemUseFirst(PlayerEntity player, World world, BlockPos pos, ItemStack stack, Direction facing, double hitX, double hitY, double hitZ) {
		if (!world.isClientSide) {
			// If the player is not sneaking, add/remove positions from the item when right-clicking a lockable block
			if (!player.isShiftKeyDown()) {
				TileEntity te = world.getBlockEntity(pos);

				if (te instanceof ILockable) {
					if (te instanceof IOwnable && !((IOwnable) te).getOwner().isOwner(player)) {
						Block block = te.getBlockState().getBlock();

						if (!(block instanceof DisguisableBlock) || ((DisguisableBlock) block).getDisguisedBlockState(world, pos) == null) {
							PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.SONIC_SECURITY_SYSTEM.get().getDescriptionId()), Utils.localize("messages.securitycraft:notOwned", ((IOwnable) te).getOwner().getName(), pos), TextFormatting.GREEN);
							return ActionResultType.SUCCESS;
						}
					}
					else {
						if (stack.getTag() == null)
							stack.setTag(new CompoundNBT());

						// Remove a block from the tag if it was already linked to.
						// If not, link to it
						if (isAdded(stack.getTag(), pos)) {
							removeLinkedBlock(stack.getTag(), pos);
							PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.SONIC_SECURITY_SYSTEM.get().getDescriptionId()), Utils.localize("messages.securitycraft:sonic_security_system.blockUnlinked", Utils.localize(world.getBlockState(pos).getBlock().getDescriptionId()), pos), TextFormatting.GREEN);
							return ActionResultType.SUCCESS;
						}
						else if (addLinkedBlock(stack.getTag(), pos, player)) {
							PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.SONIC_SECURITY_SYSTEM.get().getDescriptionId()), Utils.localize("messages.securitycraft:sonic_security_system.blockLinked", Utils.localize(world.getBlockState(pos).getBlock().getDescriptionId()), pos), TextFormatting.GREEN);
							SecurityCraft.channel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new UpdateNBTTagOnClient(stack));
							return ActionResultType.SUCCESS;
						}
					}
				}
			}
		}

		//don't place down the SSS if it has at least one linked block
		//placing is handled by minecraft otherwise
		if (!stack.hasTag() || !hasLinkedBlock(stack.getTag())) {
			if (!world.isClientSide)
				PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.SONIC_SECURITY_SYSTEM.get().getDescriptionId()), Utils.localize("messages.securitycraft:sonic_security_system.notLinked"), TextFormatting.DARK_RED);

			return ActionResultType.FAIL;
		}

		return ActionResultType.PASS;
	}

	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		ActionResultType returnValue = super.useOn(ctx);

		if (returnValue.consumesAction())
			((SonicSecuritySystemBlockEntity) ctx.getLevel().getBlockEntity(ctx.getClickedPos().relative(ctx.getClickedFace()))).transferPositionsFromItem(ctx.getItemInHand().getTag());

		return returnValue;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		if (!stack.hasTag())
			return;

		// If this item is storing block positions, show the number of them in the tooltip
		int numOfLinkedBlocks = stack.getTag().getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND).size();

		if (numOfLinkedBlocks > 0)
			tooltip.add(Utils.localize("tooltip.securitycraft:sonicSecuritySystem.linkedTo", numOfLinkedBlocks).withStyle(Utils.GRAY_STYLE));
	}

	/**
	 * Adds a position to a tag
	 *
	 * @param tag The tag to add the position to
	 * @param pos The position to add to the tag
	 * @param player The player who tries to link a block
	 * @return true if the position was added, false otherwise
	 */
	public static boolean addLinkedBlock(CompoundNBT tag, BlockPos pos, PlayerEntity player) {
		// If the position was already added, return
		if (isAdded(tag, pos))
			return false;

		ListNBT list = tag.getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND);

		if (list.size() >= SonicSecuritySystemBlockEntity.MAX_LINKED_BLOCKS) {
			PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.SONIC_SECURITY_SYSTEM.get().getDescriptionId()), Utils.localize("messages.securitycraft:sonic_security_system.linkMaxReached", SonicSecuritySystemBlockEntity.MAX_LINKED_BLOCKS), TextFormatting.DARK_RED);
			return false;
		}

		CompoundNBT nbt = NBTUtil.writeBlockPos(pos);

		list.add(nbt);
		tag.put("LinkedBlocks", list);
		return true;
	}

	/**
	 * Removes a position from a tag
	 *
	 * @param tag The tag to remove the position from
	 * @param pos The position to remove from the tag
	 */
	public static void removeLinkedBlock(CompoundNBT tag, BlockPos pos) {
		if (!tag.contains("LinkedBlocks"))
			return;

		ListNBT list = tag.getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND);

		// Starting from the end of the list to prevent skipping over entries
		for (int i = list.size() - 1; i >= 0; i--) {
			BlockPos posRead = NBTUtil.readBlockPos(list.getCompound(i));

			if (pos.equals(posRead))
				list.remove(i);
		}
	}

	/**
	 * Checks whether a position is added to a tag
	 *
	 * @param tag The tag to check
	 * @param pos The position to check
	 * @return true if the position is added, false otherwise
	 */
	public static boolean isAdded(CompoundNBT tag, BlockPos pos) {
		if (!tag.contains("LinkedBlocks"))
			return false;

		ListNBT list = tag.getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < list.size(); i++) {
			BlockPos posRead = NBTUtil.readBlockPos(list.getCompound(i));

			if (pos.equals(posRead))
				return true;
		}

		return false;
	}

	/**
	 * @return true if the tag contains at least one position, false otherwise
	 */
	public static boolean hasLinkedBlock(CompoundNBT tag) {
		if (!tag.contains("LinkedBlocks"))
			return false;

		return tag.getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND).size() > 0;
	}
}
