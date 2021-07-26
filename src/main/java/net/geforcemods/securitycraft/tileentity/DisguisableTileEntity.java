package net.geforcemods.securitycraft.tileentity;

import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.CustomizableTileEntity;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.models.DisguisableDynamicBakedModel;
import net.geforcemods.securitycraft.network.client.RefreshDisguisableModel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

public class DisguisableTileEntity extends CustomizableTileEntity
{
	public DisguisableTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	@Override
	public void onModuleInserted(ItemStack stack, ModuleType module)
	{
		super.onModuleInserted(stack, module);

		if(!level.isClientSide && module == ModuleType.DISGUISE)
			SecurityCraft.channel.send(PacketDistributor.ALL.noArg(), new RefreshDisguisableModel(worldPosition, true, stack));
	}

	@Override
	public void onModuleRemoved(ItemStack stack, ModuleType module)
	{
		super.onModuleRemoved(stack, module);

		if(!level.isClientSide && module == ModuleType.DISGUISE)
			SecurityCraft.channel.send(PacketDistributor.ALL.noArg(), new RefreshDisguisableModel(worldPosition, false, stack));
	}

	@Override
	public ModuleType[] acceptedModules()
	{
		return new ModuleType[]{ModuleType.DISGUISE};
	}

	@Override
	public Option<?>[] customOptions()
	{
		return null;
	}

	@Override
	public IModelData getModelData()
	{
		return new ModelDataMap.Builder().withInitial(DisguisableDynamicBakedModel.DISGUISED_BLOCK_RL, getBlockState().getBlock().getRegistryName()).build();
	}

	@Override
	public void onLoad()
	{
		super.onLoad();

		if(level != null && level.isClientSide)
			refreshModel();
	}

	public void refreshModel()
	{
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			ModelDataManager.requestModelDataRefresh(this);
			Minecraft.getInstance().levelRenderer.setBlocksDirty(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		});
	}
}