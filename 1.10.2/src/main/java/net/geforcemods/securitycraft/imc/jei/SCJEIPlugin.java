package net.geforcemods.securitycraft.imc.jei;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import net.geforcemods.securitycraft.blocks.reinforced.IReinforcedBlock;
import net.geforcemods.securitycraft.main.mod_SecurityCraft;
import net.minecraft.item.ItemStack;

@JEIPlugin
public class SCJEIPlugin implements IModPlugin
{
	@Override
	public void register(IModRegistry registry)
	{
		registry.addAdvancedGuiHandlers(new SlotMover());
		registry.addDescription(new ItemStack(mod_SecurityCraft.adminTool), "gui.scManual.recipe.adminTool");
		registry.addDescription(new ItemStack(mod_SecurityCraft.keypad), "gui.scManual.recipe.keypad");
		registry.addDescription(new ItemStack(mod_SecurityCraft.scManual), "gui.scManual.recipe.scManual");
		IReinforcedBlock.BLOCKS.forEach((rb) -> {
			IReinforcedBlock reinforcedBlock = (IReinforcedBlock)rb;

			reinforcedBlock.getVanillaBlocks().forEach((vanillaBlock) -> {
				if(reinforcedBlock.getVanillaBlocks().size() == reinforcedBlock.getAmount())
					registry.addDescription(new ItemStack(rb, 1, reinforcedBlock.getVanillaBlocks().indexOf(vanillaBlock)), "jei.reinforcedBlock.info", "", vanillaBlock.getUnlocalizedName() + ".name");
				else
				{
					for(int i = 0; i < reinforcedBlock.getAmount(); i++)
					{
						registry.addDescription(new ItemStack(rb, 1, i), "jei.reinforcedBlock.info", "", new ItemStack(vanillaBlock, 1, i).getUnlocalizedName() + ".name");
					}
				}
			});
		});
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime){}

	@Override
	public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {}

	@Override
	public void registerIngredients(IModIngredientRegistration registry) {}
}
