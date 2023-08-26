package net.geforcemods.securitycraft.blocks.reinforced;

import java.util.function.Supplier;

import net.geforcemods.securitycraft.api.IReinforcedBlock;
import net.geforcemods.securitycraft.api.OwnableBlockEntity;
import net.geforcemods.securitycraft.misc.OwnershipEvent;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class ReinforcedWallBlock extends WallBlock implements IReinforcedBlock {
	private final Supplier<Block> vanillaBlockSupplier;

	public ReinforcedWallBlock(AbstractBlock.Properties properties, Block vanillaBlock) {
		super(properties);

		this.vanillaBlockSupplier = () -> vanillaBlock;
	}

	@Override
	public Block getVanillaBlock() {
		return vanillaBlockSupplier.get();
	}

	@Override
	public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (placer instanceof PlayerEntity)
			MinecraftForge.EVENT_BUS.post(new OwnershipEvent(world, pos, (PlayerEntity) placer));
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new OwnableBlockEntity();
	}
}
