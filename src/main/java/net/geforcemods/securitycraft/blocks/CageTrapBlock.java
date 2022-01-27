package net.geforcemods.securitycraft.blocks;

import org.apache.logging.log4j.util.TriConsumer;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.blockentities.CageTrapBlockEntity;
import net.geforcemods.securitycraft.blockentities.DisguisableBlockEntity;
import net.geforcemods.securitycraft.blockentities.ReinforcedIronBarsBlockEntity;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedPaneBlock;
import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.util.ModuleUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.EntitySelectionContext;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class CageTrapBlock extends DisguisableBlock {
	public static final BooleanProperty DEACTIVATED = BooleanProperty.create("deactivated");

	public CageTrapBlock(Block.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DEACTIVATED, false));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
		TileEntity tile = world.getBlockEntity(pos);

		if (tile instanceof CageTrapBlockEntity) {
			CageTrapBlockEntity te = (CageTrapBlockEntity) tile;

			if (ctx instanceof EntitySelectionContext) {
				EntitySelectionContext esc = (EntitySelectionContext) ctx;
				Entity entity = esc.getEntity();

				if (entity instanceof PlayerEntity && (te.getOwner().isOwner((PlayerEntity) entity) || ModuleUtils.isAllowed(te, entity)))
					return getCorrectShape(state, world, pos, ctx, te);
				if (entity instanceof MobEntity && !state.getValue(DEACTIVATED))
					return te.capturesMobs() ? VoxelShapes.empty() : getCorrectShape(state, world, pos, ctx, te);
				else if (entity instanceof ItemEntity)
					return getCorrectShape(state, world, pos, ctx, te);
			}

			return state.getValue(DEACTIVATED) ? getCorrectShape(state, world, pos, ctx, te) : VoxelShapes.empty();
		}
		else
			return VoxelShapes.empty(); //shouldn't happen
	}

	private VoxelShape getCorrectShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx, DisguisableBlockEntity disguisableTe) {
		ItemStack moduleStack = disguisableTe.getModule(ModuleType.DISGUISE);

		if (!moduleStack.isEmpty() && (((ModuleItem) moduleStack.getItem()).getBlockAddon(moduleStack.getTag()) != null))
			return super.getCollisionShape(state, world, pos, ctx);
		else
			return VoxelShapes.block();
	}

	@Override
	public void entityInside(BlockState state, World world, BlockPos pos, Entity entity) {
		if (!world.isClientSide) {
			CageTrapBlockEntity tileEntity = (CageTrapBlockEntity) world.getBlockEntity(pos);
			boolean isPlayer = entity instanceof PlayerEntity;

			if (isPlayer || (entity instanceof MobEntity && tileEntity.capturesMobs())) {
				if ((isPlayer && ((IOwnable) world.getBlockEntity(pos)).getOwner().isOwner((PlayerEntity) entity)))
					return;

				if (state.getValue(DEACTIVATED))
					return;

				BlockPos topMiddle = pos.above(4);
				String ownerName = ((IOwnable) world.getBlockEntity(pos)).getOwner().getName();

				BlockModifier placer = new BlockModifier(world, new BlockPos.Mutable().set(pos), tileEntity.getOwner());

				placer.loop((w, p, o) -> {
					if (w.isEmptyBlock(p)) {
						if (p.equals(topMiddle))
							w.setBlockAndUpdate(p, SCContent.HORIZONTAL_REINFORCED_IRON_BARS.get().defaultBlockState());
						else
							w.setBlockAndUpdate(p, ((ReinforcedPaneBlock) SCContent.REINFORCED_IRON_BARS.get()).getStateForPlacement(w, p));
					}
				});
				placer.loop((w, p, o) -> {
					TileEntity te = w.getBlockEntity(p);

					if (te instanceof IOwnable)
						((IOwnable) te).setOwner(o.getUUID(), o.getName());

					if (te instanceof ReinforcedIronBarsBlockEntity)
						((ReinforcedIronBarsBlockEntity) te).setCanDrop(false);
				});
				world.setBlockAndUpdate(pos, state.setValue(DEACTIVATED, true));
				world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundCategory.BLOCKS, 3.0F, 1.0F);

				if (isPlayer && PlayerUtils.isPlayerOnline(ownerName)) {
					PlayerUtils.sendMessageToPlayer(ownerName, Utils.localize(SCContent.CAGE_TRAP.get().getDescriptionId()), Utils.localize("messages.securitycraft:cageTrap.captured", ((PlayerEntity) entity).getName(), Utils.getFormattedCoordinates(pos)), TextFormatting.BLACK);
				}
			}
		}
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		ItemStack stack = player.getItemInHand(hand);

		if (stack.getItem() == SCContent.WIRE_CUTTERS.get()) {
			if (!state.getValue(DEACTIVATED)) {
				world.setBlockAndUpdate(pos, state.setValue(DEACTIVATED, true));

				if (!player.isCreative())
					stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

				world.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return ActionResultType.SUCCESS;
			}
		}
		else if (stack.getItem() == Items.REDSTONE) {
			if (state.getValue(DEACTIVATED)) {
				world.setBlockAndUpdate(pos, state.setValue(DEACTIVATED, false));

				if (!player.isCreative())
					stack.shrink(1);

				world.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return ActionResultType.SUCCESS;
			}
		}

		return ActionResultType.PASS;
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext ctx) {
		return getStateForPlacement(ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace(), ctx.getClickLocation().x, ctx.getClickLocation().y, ctx.getClickLocation().z, ctx.getPlayer());
	}

	public BlockState getStateForPlacement(World world, BlockPos pos, Direction facing, double hitX, double hitY, double hitZ, PlayerEntity player) {
		return defaultBlockState().setValue(DEACTIVATED, false);
	}

	@Override
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(DEACTIVATED);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new CageTrapBlockEntity();
	}

	public static class BlockModifier {
		private World world;
		private BlockPos.Mutable pos;
		private BlockPos origin;
		private Owner owner;

		public BlockModifier(World world, BlockPos.Mutable origin, Owner owner) {
			this.world = world;
			pos = origin.move(-1, 1, -1);
			this.origin = origin.immutable();
			this.owner = owner;
		}

		public void loop(TriConsumer<World, BlockPos.Mutable, Owner> ifTrue) {
			for (int y = 0; y < 4; y++) {
				for (int x = 0; x < 3; x++) {
					for (int z = 0; z < 3; z++) {
						//skip the middle column above the cage trap, but not the place where the horizontal iron bars are
						if (!(x == 1 && z == 1 && y != 3))
							ifTrue.accept(world, pos, owner);

						pos.move(0, 0, 1);
					}

					pos.move(1, 0, -3);
				}

				pos.move(-3, 1, 0);
			}

			pos.set(origin); //reset the mutable block pos for the next usage
		}
	}
}
