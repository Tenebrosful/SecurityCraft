package net.geforcemods.securitycraft.blocks.reinforced;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IExtractionBlock;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.blockentities.ReinforcedHopperBlockEntity;
import net.geforcemods.securitycraft.misc.OwnershipEvent;
import net.geforcemods.securitycraft.util.ModuleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;

public class ReinforcedHopperBlock extends HopperBlock implements IReinforcedBlock {
	public ReinforcedHopperBlock(Block.Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (placer instanceof Player)
			MinecraftForge.EVENT_BUS.post(new OwnershipEvent(level, pos, (Player) placer));

		if (stack.hasCustomHoverName()) {
			if (level.getBlockEntity(pos) instanceof ReinforcedHopperBlockEntity be)
				be.setCustomName(stack.getHoverName());
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide) {
			if (level.getBlockEntity(pos) instanceof ReinforcedHopperBlockEntity be) {
				//only allow the owner or players on the allowlist to access a reinforced hopper
				if (be.getOwner().isOwner(player) || ModuleUtils.isAllowed(be, player))
					player.openMenu(be);
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			if (level.getBlockEntity(pos) instanceof ReinforcedHopperBlockEntity be) {
				Containers.dropContents(level, pos, be);
				level.updateNeighbourForOutputSignal(pos, this);
			}

			super.onRemove(state, level, pos, newState, isMoving);
		}
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.getBlockEntity(pos) instanceof ReinforcedHopperBlockEntity be)
			ReinforcedHopperBlockEntity.entityInside(level, pos, state, entity, be);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ReinforcedHopperBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, SCContent.beTypeReinforcedHopper, ReinforcedHopperBlockEntity::pushItemsTick);
	}

	@Override
	public Block getVanillaBlock() {
		return Blocks.HOPPER;
	}

	@Override
	public BlockState getConvertedState(BlockState vanillaState) {
		return defaultBlockState().setValue(ENABLED, vanillaState.getValue(ENABLED)).setValue(FACING, vanillaState.getValue(FACING));
	}

	public static class ExtractionBlock implements IExtractionBlock {
		@Override
		public boolean canExtract(IOwnable be, Level level, BlockPos pos, BlockState state) {
			ReinforcedHopperBlockEntity hopperBe = (ReinforcedHopperBlockEntity) level.getBlockEntity(pos);

			if (!be.getOwner().owns(hopperBe)) {
				if (be instanceof IModuleInventory inv) {
					//hoppers can extract out of e.g. chests if the hopper's owner is on the chest's allowlist module
					if (ModuleUtils.isAllowed(inv, hopperBe.getOwner().getName()))
						return true;
					//hoppers can extract out of e.g. chests whose owner is on the hopper's allowlist module
					else if (ModuleUtils.isAllowed(hopperBe, be.getOwner().getName()))
						return true;
				}

				return false;
			}
			else
				return true;
		}

		@Override
		public Block getBlock() {
			return SCContent.REINFORCED_HOPPER.get();
		}
	}
}
