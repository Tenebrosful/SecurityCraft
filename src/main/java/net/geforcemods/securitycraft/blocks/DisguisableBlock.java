package net.geforcemods.securitycraft.blocks;

import java.util.Optional;

import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.compat.IOverlayDisplay;
import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public abstract class DisguisableBlock extends OwnableBlock implements IOverlayDisplay {
	public DisguisableBlock(Block.Properties properties) {
		super(properties);
	}

	@Override
	public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, Entity entity) {
		BlockState extendedState = getExtendedState(state, world, pos);

		if (extendedState.getBlock() != this)
			return extendedState.getSoundType(world, pos, entity);
		else
			return super.getSoundType(state, world, pos, entity);
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
		BlockState extendedState = getExtendedState(state, world, pos);

		if (extendedState.getBlock() != this)
			return extendedState.getShape(world, pos, ctx);
		else
			return super.getShape(state, world, pos, ctx);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
		BlockState extendedState = getExtendedState(state, world, pos);

		if (extendedState.getBlock() != this)
			return extendedState.getCollisionShape(world, pos, ctx);
		else
			return super.getCollisionShape(state, world, pos, ctx);
	}

	@Override
	public VoxelShape getRenderShape(BlockState state, IBlockReader world, BlockPos pos) {
		BlockState extendedState = getExtendedState(state, world, pos);

		if (extendedState.getBlock() != this)
			return extendedState.getRenderShape(world, pos);
		else
			return super.getRenderShape(state, world, pos);
	}

	@Override
	public boolean isNormalCube(BlockState state, IBlockReader world, BlockPos pos) {
		BlockState extendedState = getExtendedState(state, world, pos);

		if (extendedState.getBlock() != this)
			return extendedState.isNormalCube(world, pos);
		else
			return super.isNormalCube(state, world, pos);
	}

	@Override
	public boolean causesSuffocation(BlockState state, IBlockReader world, BlockPos pos) {
		BlockState extendedState = getExtendedState(state, world, pos);

		if (extendedState.getBlock() != this)
			return extendedState.causesSuffocation(world, pos);
		else
			return super.causesSuffocation(state, world, pos);
	}

	@Override
	public float getAmbientOcclusionLightValue(BlockState state, IBlockReader world, BlockPos pos) {
		BlockState extendedState = getExtendedState(state, world, pos);

		if (extendedState.getBlock() != this)
			return extendedState.getAmbientOcclusionLightValue(world, pos);
		else
			return super.getAmbientOcclusionLightValue(state, world, pos);
	}

	@Override
	public BlockState getExtendedState(BlockState state, IBlockReader world, BlockPos pos) {
		return getDisguisedBlockState(world, pos).orElse(state);
	}

	public Optional<BlockState> getDisguisedBlockState(IBlockReader world, BlockPos pos) {
		if (world.getTileEntity(pos) instanceof IModuleInventory) {
			IModuleInventory te = (IModuleInventory) world.getTileEntity(pos);
			ItemStack module = te.hasModule(ModuleType.DISGUISE) ? te.getModule(ModuleType.DISGUISE) : ItemStack.EMPTY;

			if (!module.isEmpty()) {
				Block block = ((ModuleItem) module.getItem()).getBlockAddon(module.getTag());

				if (block != null)
					return Optional.of(block.getDefaultState());
			}
		}

		return Optional.empty();
	}

	public ItemStack getDisguisedStack(IBlockReader world, BlockPos pos) {
		if (world != null && world.getTileEntity(pos) instanceof IModuleInventory) {
			IModuleInventory te = (IModuleInventory) world.getTileEntity(pos);
			ItemStack stack = te.hasModule(ModuleType.DISGUISE) ? te.getModule(ModuleType.DISGUISE) : ItemStack.EMPTY;

			if (!stack.isEmpty()) {
				Block block = ((ModuleItem) stack.getItem()).getBlockAddon(stack.getTag());

				if (block != null)
					return new ItemStack(block);
			}
		}

		return new ItemStack(this);
	}

	@Override
	public ItemStack getDisplayStack(World world, BlockState state, BlockPos pos) {
		return getDisguisedStack(world, pos);
	}

	@Override
	public boolean shouldShowSCInfo(World world, BlockState state, BlockPos pos) {
		return getDisguisedStack(world, pos).getItem() == asItem();
	}

	@Override
	public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
		return getDisguisedStack(world, pos);
	}
}
