package net.geforcemods.securitycraft.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.ICustomizable;
import net.geforcemods.securitycraft.api.IExplosive;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasswordProtected;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.SecurityCraftTileEntity;
import net.geforcemods.securitycraft.items.SCManualItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.SCManualPage;
import net.geforcemods.securitycraft.screen.components.HoverChecker;
import net.geforcemods.securitycraft.screen.components.IdButton;
import net.geforcemods.securitycraft.screen.components.IngredientDisplay;
import net.geforcemods.securitycraft.screen.components.StringHoverChecker;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SCManualScreen extends Screen {

	private ResourceLocation infoBookTexture = new ResourceLocation("securitycraft:textures/gui/info_book_texture.png");
	private ResourceLocation infoBookTextureSpecial = new ResourceLocation("securitycraft:textures/gui/info_book_texture_special.png"); //for items without a recipe
	private ResourceLocation infoBookTitlePage = new ResourceLocation("securitycraft:textures/gui/info_book_title_page.png");
	private ResourceLocation infoBookIcons = new ResourceLocation("securitycraft:textures/gui/info_book_icons.png");
	private static ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
	private List<HoverChecker> hoverCheckers = new ArrayList<>();
	private static int lastPage = -1;
	private int currentPage = lastPage;
	private NonNullList<Ingredient> recipe;
	private IngredientDisplay[] displays = new IngredientDisplay[9];
	private int startX = -1;
	private boolean update = false;
	private List<String> subpages = new ArrayList<>();
	private int currentSubpage = 0;
	private final int subpageLength = 1285;
	private final String intro1 = Utils.localize("gui.securitycraft:scManual.intro.1").getFormattedText();
	private final String intro2 = Utils.localize("gui.securitycraft:scManual.intro.2").getFormattedText();

	public SCManualScreen() {
		super(new TranslationTextComponent(SCContent.SC_MANUAL.get().getTranslationKey()));
	}

	@Override
	public void init(){
		byte startY = 2;

		if((width - 256) / 2 != startX && startX != -1)
			update = true;

		startX = (width - 256) / 2;
		minecraft.keyboardListener.enableRepeatEvents(true);

		addButton(new SCManualScreen.ChangePageButton(1, startX + 210, startY + 158, true, this::actionPerformed)); //next page
		addButton(new SCManualScreen.ChangePageButton(2, startX + 16, startY + 158, false, this::actionPerformed)); //previous page
		addButton(new SCManualScreen.ChangePageButton(3, startX + 180, startY + 97, true, this::actionPerformed)); //next subpage
		addButton(new SCManualScreen.ChangePageButton(4, startX + 155, startY + 97, false, this::actionPerformed)); //previous subpage

		for(int i = 0; i < 3; i++)
		{
			for(int j = 0; j < 3; j++)
			{
				displays[(i * 3) + j] = new IngredientDisplay((startX + 100) + (j * 20), 144 + (i * 20));
			}
		}

		updateRecipeAndIcons();
		SCManualItem.PAGES.sort((page1, page2) -> {
			String key1 = Utils.localize(page1.getItem().getTranslationKey()).getFormattedText();
			String key2 = Utils.localize(page2.getItem().getTranslationKey()).getFormattedText();

			return key1.compareTo(key2);
		});
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks){
		renderBackground();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		if(update)
		{
			updateRecipeAndIcons();
			update = false;
		}

		if(currentPage == -1)
			minecraft.getTextureManager().bindTexture(infoBookTitlePage);
		else if(recipe != null && recipe.size() > 0)
			minecraft.getTextureManager().bindTexture(infoBookTexture);
		else
			minecraft.getTextureManager().bindTexture(infoBookTextureSpecial);

		this.blit(startX, 5, 0, 0, 256, 250);

		if(currentPage > -1){
			if(SCManualItem.PAGES.get(currentPage).getHelpInfo().equals("help.securitycraft:reinforced.info"))
				font.drawString(Utils.localize("gui.securitycraft:scManual.reinforced").getFormattedText(), startX + 39, 27, 0);
			else
				font.drawString(Utils.localize(SCManualItem.PAGES.get(currentPage).getItem().getTranslationKey()).getFormattedText(), startX + 39, 27, 0);

			font.drawSplitString(subpages.get(currentSubpage), startX + 18, 45, 225, 0);

			String designedBy = SCManualItem.PAGES.get(currentPage).getDesignedBy();

			if(designedBy != null && !designedBy.isEmpty())
				font.drawSplitString(Utils.localize("gui.securitycraft:scManual.designedBy", designedBy).getFormattedText(), startX + 18, 180, 75, 0);
		}else{
			font.drawString(intro1, width / 2 - font.getStringWidth(intro1) / 2, 22, 0);
			font.drawString(intro2, width / 2 - font.getStringWidth(intro2) / 2, 142, 0);


			if(I18n.hasKey("gui.securitycraft:scManual.author"))
			{
				String text = Utils.localize("gui.securitycraft:scManual.author").getFormattedText();

				font.drawSplitString(text, width / 2 - 175 / 2, 155, 175, 0);
			}
		}

		for(int i = 0; i < buttons.size(); i++)
			buttons.get(i).render(mouseX, mouseY, partialTicks);

		if(currentPage != -1)
		{
			if(subpages.size() > 1)
				font.drawString((currentSubpage + 1) + "/" + subpages.size(), startX + 205, 102, 0x8E8270);

			font.drawString((currentPage + 1) + "/" + SCManualItem.PAGES.size(), startX + 195, 192, 0x8E8270);
		}

		if(currentPage > -1){
			Item item = SCManualItem.PAGES.get(currentPage).getItem();

			minecraft.getItemRenderer().renderItemAndEffectIntoGUI(new ItemStack(item), startX + 19, 22);
			minecraft.getTextureManager().bindTexture(infoBookIcons);

			if(item instanceof BlockItem){
				Block block = ((BlockItem) item).getBlock();
				TileEntity te = block.hasTileEntity(block.getDefaultState()) ? block.createTileEntity(block.getDefaultState(), Minecraft.getInstance().world) : null;

				if(block instanceof IExplosive)
					this.blit(startX + 107, 117, 54, 1, 18, 18);

				if(te instanceof IOwnable)
					this.blit(startX + 29, 118, 1, 1, 16, 16);

				if(te instanceof IPasswordProtected)
					this.blit(startX + 55, 118, 18, 1, 17, 16);

				if(te instanceof SecurityCraftTileEntity && ((SecurityCraftTileEntity) te).isActivatedByView())
					this.blit(startX + 81, 118, 36, 1, 17, 16);

				if(te instanceof ICustomizable)
				{
					ICustomizable scte = (ICustomizable)te;

					this.blit(startX + 213, 118, 72, 1, 16, 16);

					if(scte.customOptions() != null && scte.customOptions().length > 0)
						this.blit(startX + 136, 118, 88, 1, 16, 16);
				}

				if(te instanceof IModuleInventory)
				{
					if(((IModuleInventory)te).acceptedModules() != null && ((IModuleInventory)te).acceptedModules().length > 0)
						this.blit(startX + 163, 118, 105, 1, 16, 16);
				}
			}

			for(IngredientDisplay display : displays)
			{
				display.render(minecraft, partialTicks);
			}

			for(int i = 0; i < hoverCheckers.size(); i++)
			{
				HoverChecker chc = hoverCheckers.get(i);

				if(chc != null && chc.checkHover(mouseX, mouseY))
				{
					if(chc instanceof StringHoverChecker && ((StringHoverChecker)chc).getName() != null)
						renderTooltip(((StringHoverChecker)chc).getLines(), mouseX, mouseY);
					else if(i < displays.length && !displays[i].getCurrentStack().isEmpty())
						renderTooltip(displays[i].getCurrentStack(), mouseX, mouseY);
				}
			}
		}
	}

	@Override
	public void onClose(){
		super.onClose();
		lastPage = currentPage;
		minecraft.keyboardListener.enableRepeatEvents(false);
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode){
		if(keyCode == GLFW.GLFW_KEY_LEFT)
			previousSubpage();
		else if(keyCode == GLFW.GLFW_KEY_RIGHT)
			nextSubpage();

		return super.charTyped(typedChar, keyCode);
	}

	protected void actionPerformed(IdButton button){
		if(button.id == 1)
			nextPage();
		else if(button.id == 2)
			previousPage();
		else if(button.id == 3)
			nextSubpage();
		else if(button.id == 4)
			previousSubpage();

		//hide subpage buttons on main page
		buttons.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttons.get(3).visible = currentPage != -1 && subpages.size() > 1;
	}

	@Override
	public boolean mouseScrolled(double aDouble, double p_mouseScrolled_3_, double p_mouseScrolled_5_)
	{
		super.mouseScrolled(aDouble, p_mouseScrolled_3_, p_mouseScrolled_5_);

		switch((int)Math.signum(p_mouseScrolled_5_))
		{
			case -1: nextPage(); break;
			case 1: previousPage(); break;
		}

		//hide subpage buttons on main page
		buttons.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttons.get(3).visible = currentPage != -1 && subpages.size() > 1;
		return true;
	}

	private void nextPage()
	{
		currentPage++;

		if(currentPage > SCManualItem.PAGES.size() - 1)
			currentPage = -1;

		updateRecipeAndIcons();
	}

	private void previousPage()
	{
		currentPage--;

		if(currentPage < -1)
			currentPage = SCManualItem.PAGES.size() - 1;

		updateRecipeAndIcons();
	}

	private void nextSubpage()
	{
		currentSubpage++;

		if(currentSubpage == subpages.size())
			currentSubpage = 0;
	}

	private void previousSubpage()
	{
		currentSubpage--;

		if(currentSubpage == -1)
			currentSubpage = subpages.size() - 1;
	}

	private void updateRecipeAndIcons(){
		currentSubpage = 0;
		hoverCheckers.clear();

		if(currentPage < 0){
			recipe = null;
			buttons.get(2).visible = false;
			buttons.get(3).visible = false;
			return;
		}

		SCManualPage page = SCManualItem.PAGES.get(currentPage);

		for(IRecipe<?> object : Minecraft.getInstance().world.getRecipeManager().getRecipes())
		{
			if(object instanceof ShapedRecipe){
				ShapedRecipe recipe = (ShapedRecipe) object;

				if(!recipe.getRecipeOutput().isEmpty() && recipe.getRecipeOutput().getItem() == page.getItem()){
					NonNullList<Ingredient> ingredients = recipe.getIngredients();
					NonNullList<Ingredient> recipeItems = NonNullList.<Ingredient>withSize(9, Ingredient.EMPTY);

					for(int i = 0; i < ingredients.size(); i++)
					{
						recipeItems.set(getCraftMatrixPosition(i, recipe.getWidth(), recipe.getHeight()), ingredients.get(i));
					}

					this.recipe = recipeItems;
					break;
				}
			}else if(object instanceof ShapelessRecipe){
				ShapelessRecipe recipe = (ShapelessRecipe) object;

				if(!recipe.getRecipeOutput().isEmpty() && recipe.getRecipeOutput().getItem() == page.getItem()){
					//don't show keycard reset recipes
					if(recipe.getId().getPath().endsWith("_reset"))
						continue;

					NonNullList<Ingredient> recipeItems = NonNullList.<Ingredient>withSize(recipe.getIngredients().size(), Ingredient.EMPTY);

					for(int i = 0; i < recipeItems.size(); i++)
						recipeItems.set(i, recipe.getIngredients().get(i));

					this.recipe = recipeItems;
					break;
				}
			}

			recipe = null;
		}

		String helpInfo = page.getHelpInfo();
		boolean reinforcedPage = helpInfo.equals("help.securitycraft:reinforced.info") || helpInfo.contains("reinforced_hopper");

		if(page.hasRecipeDescription())
		{
			String name = page.getItem().getRegistryName().getPath();

			hoverCheckers.add(new StringHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.recipe." + name).getFormattedText()));
		}
		else if(reinforcedPage)
		{
			recipe = null;
			hoverCheckers.add(new StringHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.recipe.reinforced").getFormattedText()));
		}
		else if(recipe != null)
		{
			for(int i = 0; i < 3; i++)
			{
				for(int j = 0; j < 3; j++)
				{
					hoverCheckers.add(new HoverChecker(144 + (i * 20), 144 + (i * 20) + 16, (startX + 100) + (j * 20), (startX + 100) + (j * 20) + 16));
				}
			}
		}
		else
			hoverCheckers.add(new StringHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.disabled").getFormattedText()));

		Item item = page.getItem();

		if(item instanceof BlockItem){
			Block block = ((BlockItem) item).getBlock();
			TileEntity te = block.hasTileEntity(block.getDefaultState()) ? block.createTileEntity(block.getDefaultState(), Minecraft.getInstance().world) : null;

			if(block instanceof IExplosive)
				hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 107, (startX + 107) + 16, Utils.localize("gui.securitycraft:scManual.explosiveBlock").getFormattedText()));

			if(te != null){
				if(te instanceof IOwnable)
					hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 29, (startX + 29) + 16, Utils.localize("gui.securitycraft:scManual.ownableBlock").getFormattedText()));

				if(te instanceof IPasswordProtected)
					hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 55, (startX + 55) + 16, Utils.localize("gui.securitycraft:scManual.passwordProtectedBlock").getFormattedText()));

				if(te instanceof SecurityCraftTileEntity && ((SecurityCraftTileEntity) te).isActivatedByView())
					hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 81, (startX + 81) + 16, Utils.localize("gui.securitycraft:scManual.viewActivatedBlock").getFormattedText()));

				if(te instanceof ICustomizable)
				{
					ICustomizable scte = (ICustomizable)te;

					hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 213, (startX + 213) + 16, Utils.localize("gui.securitycraft:scManual.customizableBlock").getFormattedText()));

					if(scte.customOptions() != null && scte.customOptions().length > 0)
					{
						List<String> display = new ArrayList<>();

						display.add(Utils.localize("gui.securitycraft:scManual.options").getFormattedText());
						display.add("---");

						for(Option<?> option : scte.customOptions())
						{
							display.add("- " + Utils.localize("option" + block.getTranslationKey().substring(5) + "." + option.getName() + ".description").getFormattedText());
							display.add("");
						}

						display.remove(display.size() - 1);
						hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 136, (startX + 136) + 16, display));
					}
				}

				if(te instanceof IModuleInventory)
				{
					IModuleInventory moduleInv = (IModuleInventory)te;

					if(moduleInv.acceptedModules() != null && moduleInv.acceptedModules().length > 0)
					{
						List<String> display = new ArrayList<>();

						display.add(Utils.localize("gui.securitycraft:scManual.modules").getFormattedText());
						display.add("---");

						for(ModuleType module : moduleInv.acceptedModules())
						{
							display.add("- " + Utils.localize("module" + block.getTranslationKey().substring(5) + "." + module.getItem().getTranslationKey().substring(5).replace("securitycraft.", "") + ".description").getFormattedText());
							display.add("");
						}

						display.remove(display.size() - 1);
						hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 163, (startX + 163) + 16, display));
					}
				}
			}
		}

		if(recipe != null && recipe.size() > 0)
		{
			for(int i = 0; i < 3; i++)
			{
				for(int j = 0; j < 3; j++)
				{
					int index = (i * 3) + j;

					if(index >= recipe.size())
						displays[index].setIngredient(Ingredient.EMPTY);
					else
						displays[index].setIngredient(recipe.get(index));
				}
			}
		}
		else
		{
			for(IngredientDisplay display : displays)
			{
				display.setIngredient(Ingredient.EMPTY);
			}
		}

		//set up subpages
		helpInfo = Utils.localize(page.getHelpInfo()).getFormattedText();
		subpages.clear();

		while(font.getStringWidth(helpInfo) > subpageLength)
		{
			String trimmed = font.trimStringToWidth(helpInfo, 1285);
			int lastIndex = trimmed.lastIndexOf(' ');

			if(lastIndex > 0)
				trimmed = trimmed.trim().substring(0, lastIndex); //remove last word to remove the possibility to break it up onto multiple pages

			trimmed = trimmed.trim();
			subpages.add(trimmed);
			helpInfo = helpInfo.replace(trimmed, "").trim();
		}

		subpages.add(helpInfo);
		buttons.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttons.get(3).visible = currentPage != -1 && subpages.size() > 1;
	}

	static class ChangePageButton extends IdButton {
		private final boolean isForward;

		public ChangePageButton(int index, int xPos, int yPos, boolean forward, Consumer<IdButton> onClick){
			super(index, xPos, yPos, 23, 13, "", onClick);
			isForward = forward;
		}

		/**
		 * Draws this button to the screen.
		 */
		@Override
		public void render(int mouseX, int mouseY, float partialTicks){
			if(visible){
				boolean isHovering = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				Minecraft.getInstance().getTextureManager().bindTexture(bookGuiTextures);
				int textureX = 0;
				int textureY = 192;

				if(isHovering)
					textureX += 23;

				if(!isForward)
					textureY += 13;

				this.blit(x, y, textureX, textureY, 23, 13);
			}
		}
	}

	//from JEI
	private int getCraftMatrixPosition(int i, int width, int height)
	{
		int index;

		if(width == 1)
		{
			if(height == 3)
				index = (i * 3) + 1;
			else if(height == 2)
				index = (i * 3) + 1;
			else
				index = 4;

		}
		else if(height == 1)
			index = i + 3;
		else if(width == 2)
		{
			index = i;

			if(i > 1)
			{
				index++;

				if(i > 3)
					index++;
			}
		}
		else if(height == 2)
			index = i + 3;
		else
			index = i;

		return index;
	}
}