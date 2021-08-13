package net.geforcemods.securitycraft.blocks.reinforced;

import net.geforcemods.securitycraft.api.OwnableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ReinforcedCobwebBlock extends BaseReinforcedBlock
{
	public ReinforcedCobwebBlock(Properties properties, Block vB)
	{
		super(properties, vB);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx)
	{
		return Shapes.empty();
	}

	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity)
	{
		if(entity instanceof Player player)
		{
			BlockEntity tile = world.getBlockEntity(pos);

			if(tile instanceof OwnableBlockEntity te)
			{
				if(te.getOwner().isOwner(player))
					return;
			}
		}

		entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
	}
}
