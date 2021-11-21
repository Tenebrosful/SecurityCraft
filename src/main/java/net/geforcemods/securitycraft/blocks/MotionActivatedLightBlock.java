package net.geforcemods.securitycraft.blocks;

import net.geforcemods.securitycraft.tileentity.MotionActivatedLightTileEntity;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class MotionActivatedLightBlock extends OwnableBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE_NORTH = VoxelShapes.or(Block.makeCuboidShape(6, 3, 13, 10, 4, 14), VoxelShapes.or(Block.makeCuboidShape(6, 6, 13, 10, 9, 14), VoxelShapes.combine(Block.makeCuboidShape(7, 3, 14, 9, 8, 16), Block.makeCuboidShape(7, 4, 15, 9, 7, 14), IBooleanFunction.ONLY_FIRST)));
	private static final VoxelShape SHAPE_EAST = VoxelShapes.or(Block.makeCuboidShape(3, 3, 6, 2, 4, 10), VoxelShapes.or(Block.makeCuboidShape(3, 6, 6, 2, 9, 10), VoxelShapes.combine(Block.makeCuboidShape(2, 3, 7, 0, 8, 9), Block.makeCuboidShape(1, 4, 7, 2, 7, 9), IBooleanFunction.ONLY_FIRST)));
	private static final VoxelShape SHAPE_SOUTH = VoxelShapes.or(Block.makeCuboidShape(6, 3, 2, 10, 4, 3), VoxelShapes.or(Block.makeCuboidShape(6, 6, 2, 10, 9, 3), VoxelShapes.combine(Block.makeCuboidShape(7, 3, 0, 9, 8, 2), Block.makeCuboidShape(7, 4, 1, 9, 7, 2), IBooleanFunction.ONLY_FIRST)));
	private static final VoxelShape SHAPE_WEST = VoxelShapes.or(Block.makeCuboidShape(13, 3, 6, 14, 4, 10), VoxelShapes.or(Block.makeCuboidShape(13, 6, 6, 14, 9, 10), VoxelShapes.combine(Block.makeCuboidShape(14, 3, 7, 16, 8, 9), Block.makeCuboidShape(15, 4, 7, 14, 7, 9), IBooleanFunction.ONLY_FIRST)));

	public MotionActivatedLightBlock(Block.Properties properties) {
		super(properties);
		setDefaultState(stateContainer.getBaseState().with(FACING, Direction.NORTH).with(LIT, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx){
		switch(state.get(FACING))
		{
			case NORTH: return SHAPE_NORTH;
			case EAST: return SHAPE_EAST;
			case SOUTH: return SHAPE_SOUTH;
			case WEST: return SHAPE_WEST;
			default: return VoxelShapes.fullCube();
		}
	}

	@Override
	public int getLightValue(BlockState state) {
		return state.get(LIT) ? 15 : 0;
	}

	@Override
	public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos){
		Direction side = state.get(FACING);

		return side != Direction.UP && side != Direction.DOWN && BlockUtils.isSideSolid(world, pos.offset(side.getOpposite()), side);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext ctx)
	{
		return getStateForPlacement(ctx.getWorld(), ctx.getPos(), ctx.getFace(), ctx.getHitVec().x, ctx.getHitVec().y, ctx.getHitVec().z, ctx.getPlayer());
	}

	public BlockState getStateForPlacement(World world, BlockPos pos, Direction facing, double hitX, double hitY, double hitZ, PlayerEntity placer)
	{
		return facing != Direction.UP && facing != Direction.DOWN && BlockUtils.isSideSolid(world, pos.offset(facing.getOpposite()), facing) ? getDefaultState().with(FACING, facing) : null;
	}

	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean flag) {
		if (!isValidPosition(state, world, pos))
			world.destroyBlock(pos, true);
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
		builder.add(LIT);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new MotionActivatedLightTileEntity();
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		return state.with(FACING, rot.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		Direction facing = state.get(FACING);

		switch(mirror)
		{
			case LEFT_RIGHT:
				if(facing.getAxis() == Axis.Z)
					return state.with(FACING, facing.getOpposite());
				break;
			case FRONT_BACK:
				if(facing.getAxis() == Axis.X)
					return state.with(FACING, facing.getOpposite());
				break;
			case NONE: break;
		}

		return state;
	}
}
