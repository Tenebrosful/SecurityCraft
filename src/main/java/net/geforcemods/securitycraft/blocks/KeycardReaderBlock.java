package net.geforcemods.securitycraft.blocks;

import java.util.Random;

import net.geforcemods.securitycraft.ConfigHandler;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.blockentities.KeycardReaderBlockEntity;
import net.geforcemods.securitycraft.inventory.ItemContainer;
import net.geforcemods.securitycraft.items.CodebreakerItem;
import net.geforcemods.securitycraft.items.KeycardItem;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

public class KeycardReaderBlock extends DisguisableBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public KeycardReaderBlock(AbstractBlock.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(WATERLOGGED, false));
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		if (!world.isClientSide) {
			KeycardReaderBlockEntity te = (KeycardReaderBlockEntity) world.getBlockEntity(pos);

			if (te.isDisabled())
				player.displayClientMessage(Utils.localize("gui.securitycraft:scManual.disabled"), true);
			else if (te.isDenied(player)) {
				if (te.sendsMessages())
					PlayerUtils.sendMessageToPlayer(player, new TranslationTextComponent(getDescriptionId()), Utils.localize("messages.securitycraft:module.onDenylist"), TextFormatting.RED);
			}
			else {
				ItemStack stack = player.getItemInHand(hand);
				Item item = stack.getItem();
				boolean isCodebreaker = item == SCContent.CODEBREAKER.get();
				boolean isKeycardHolder = item == SCContent.KEYCARD_HOLDER.get();

				//either no keycard, or an unlinked keycard, or an admin tool
				if (!isKeycardHolder && (!(item instanceof KeycardItem) || !stack.hasTag() || !stack.getTag().getBoolean("linked")) && !isCodebreaker) {
					//only allow the owner and players on the allowlist to open the gui
					if (te.isOwnedBy(player) || te.isAllowed(player))
						NetworkHooks.openGui((ServerPlayerEntity) player, te, pos);
				}
				else if (item != SCContent.LIMITED_USE_KEYCARD.get()) { //limited use keycards are only crafting components now
					if (isCodebreaker) {
						double chance = ConfigHandler.SERVER.codebreakerChance.get();

						if (chance < 0.0D)
							PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.KEYCARD_READER.get().getDescriptionId()), Utils.localize("messages.securitycraft:codebreakerDisabled"), TextFormatting.RED);
						else {
							if (CodebreakerItem.wasRecentlyUsed(stack))
								return ActionResultType.PASS;

							boolean isSuccessful = player.isCreative() || SecurityCraft.RANDOM.nextDouble() < chance;
							CompoundNBT tag = stack.getOrCreateTag();

							stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
							tag.putLong(CodebreakerItem.LAST_USED_TIME, System.currentTimeMillis());
							tag.putBoolean(CodebreakerItem.WAS_SUCCESSFUL, isSuccessful);

							if (isSuccessful)
								activate(world, pos, te.getSignalLength());
							else
								PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.CODEBREAKER.get().getDescriptionId()), Utils.localize("messages.securitycraft:codebreaker.failed"), TextFormatting.RED);
						}
					}
					else {
						if (isKeycardHolder) {
							ItemContainer holderInventory = ItemContainer.keycardHolder(stack);
							IFormattableTextComponent feedback = null;

							for (int i = 0; i < holderInventory.getContainerSize(); i++) {
								ItemStack keycardStack = holderInventory.getItem(i);

								if (keycardStack.getItem() instanceof KeycardItem && keycardStack.hasTag()) {
									feedback = insertCard(world, pos, te, keycardStack, player);

									if (feedback == null)
										return ActionResultType.SUCCESS;
								}
							}

							if (feedback == null)
								PlayerUtils.sendMessageToPlayer(player, new TranslationTextComponent(getDescriptionId()), Utils.localize("messages.securitycraft:keycard_holder.no_keycards"), TextFormatting.RED);
							else
								PlayerUtils.sendMessageToPlayer(player, new TranslationTextComponent(getDescriptionId()), Utils.localize("messages.securitycraft:keycard_holder.fail"), TextFormatting.RED);
						}
						else {
							IFormattableTextComponent feedback = insertCard(world, pos, te, stack, player);

							if (feedback != null)
								PlayerUtils.sendMessageToPlayer(player, new TranslationTextComponent(getDescriptionId()), feedback, TextFormatting.RED);
						}
					}
				}
			}
		}

		return ActionResultType.SUCCESS;
	}

	public IFormattableTextComponent insertCard(World world, BlockPos pos, KeycardReaderBlockEntity te, ItemStack stack, PlayerEntity player) {
		CompoundNBT tag = stack.getTag();
		Owner keycardOwner = new Owner(tag.getString("ownerName"), tag.getString("ownerUUID"));

		//owner of this keycard reader and the keycard reader the keycard got linked to do not match
		if ((ConfigHandler.SERVER.enableTeamOwnership.get() && !PlayerUtils.areOnSameTeam(te.getOwner(), keycardOwner)) || !te.getOwner().getUUID().equals(keycardOwner.getUUID()))
			return new TranslationTextComponent("messages.securitycraft:keycardReader.differentOwner");

		//the keycard's signature does not match this keycard reader's
		if (te.getSignature() != tag.getInt("signature"))
			return new TranslationTextComponent("messages.securitycraft:keycardReader.wrongSignature");

		int level = ((KeycardItem) stack.getItem()).getLevel();

		//the keycard's level
		if (!te.getAcceptedLevels()[level]) //both are 0 indexed, so it's ok
			return new TranslationTextComponent("messages.securitycraft:keycardReader.wrongLevel", level + 1); //level is 0-indexed, so it has to be increased by one to match with the item name

		boolean powered = world.getBlockState(pos).getValue(POWERED);

		if (tag.getBoolean("limited")) {
			int uses = tag.getInt("uses");

			if (uses <= 0)
				return new TranslationTextComponent("messages.securitycraft:keycardReader.noUses");

			if (!player.isCreative() && !powered)
				tag.putInt("uses", --uses);
		}

		if (!powered) {
			activate(world, pos, te.getSignalLength());
		}

		return null;
	}

	public void activate(World world, BlockPos pos, int signalLength) {
		world.setBlockAndUpdate(pos, world.getBlockState(pos).setValue(POWERED, true));
		BlockUtils.updateIndirectNeighbors(world, pos, SCContent.KEYCARD_READER.get());
		world.getBlockTicks().scheduleTick(pos, SCContent.KEYCARD_READER.get(), signalLength);
	}

	@Override
	public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!world.isClientSide) {
			world.setBlockAndUpdate(pos, state.setValue(POWERED, false));
			BlockUtils.updateIndirectNeighbors(world, pos, SCContent.KEYCARD_READER.get());
		}
	}

	@Override
	public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			if (state.getValue(POWERED)) {
				world.updateNeighborsAt(pos, this);
				BlockUtils.updateIndirectNeighbors(world, pos, this);
			}

			super.onRemove(state, world, pos, newState, isMoving);
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState state, World world, BlockPos pos, Random rand) {
		if ((state.getValue(POWERED))) {
			double x = pos.getX() + 0.5F + (rand.nextFloat() - 0.5F) * 0.2D;
			double y = pos.getY() + 0.7F + (rand.nextFloat() - 0.5F) * 0.2D;
			double z = pos.getZ() + 0.5F + (rand.nextFloat() - 0.5F) * 0.2D;
			double magicNumber1 = 0.2199999988079071D;
			double magicNumber2 = 0.27000001072883606D;
			float f1 = 0.6F + 0.4F;
			float f2 = Math.max(0.0F, 0.7F - 0.5F);
			float f3 = Math.max(0.0F, 0.6F - 0.7F);

			world.addParticle(new RedstoneParticleData(f1, f2, f3, 1), false, x - magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
			world.addParticle(new RedstoneParticleData(f1, f2, f3, 1), false, x + magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
			world.addParticle(new RedstoneParticleData(f1, f2, f3, 1), false, x, y + magicNumber1, z - magicNumber2, 0.0D, 0.0D, 0.0D);
			world.addParticle(new RedstoneParticleData(f1, f2, f3, 1), false, x, y + magicNumber1, z + magicNumber2, 0.0D, 0.0D, 0.0D);
			world.addParticle(new RedstoneParticleData(f1, f2, f3, 1), false, x, y, z, 0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
		if ((blockState.getValue(POWERED)))
			return 15;
		else
			return 0;
	}

	@Override
	public int getDirectSignal(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext ctx) {
		return super.getStateForPlacement(ctx).setValue(FACING, ctx.getPlayer().getDirection().getOpposite()).setValue(POWERED, false);
	}

	@Override
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, WATERLOGGED);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new KeycardReaderBlockEntity();
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
