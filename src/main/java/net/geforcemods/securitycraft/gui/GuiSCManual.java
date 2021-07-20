package net.geforcemods.securitycraft.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.geforcemods.securitycraft.api.ICustomizable;
import net.geforcemods.securitycraft.api.IExplosive;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasswordProtected;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.TileEntitySCTE;
import net.geforcemods.securitycraft.gui.components.HoverChecker;
import net.geforcemods.securitycraft.gui.components.StackHoverChecker;
import net.geforcemods.securitycraft.gui.components.StringHoverChecker;
import net.geforcemods.securitycraft.items.ItemSCManual;
import net.geforcemods.securitycraft.misc.EnumModuleType;
import net.geforcemods.securitycraft.misc.SCManualPage;
import net.geforcemods.securitycraft.util.GuiUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiSCManual extends GuiScreen {

	private ResourceLocation infoBookTexture = new ResourceLocation("securitycraft:textures/gui/info_book_texture.png");
	private ResourceLocation infoBookTextureSpecial = new ResourceLocation("securitycraft:textures/gui/info_book_texture_special.png"); //for items without a recipe
	private ResourceLocation infoBookTitlePage = new ResourceLocation("securitycraft:textures/gui/info_book_title_page.png");
	private ResourceLocation infoBookIcons = new ResourceLocation("securitycraft:textures/gui/info_book_icons.png");
	private static ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
	private List<HoverChecker> hoverCheckers = new ArrayList<>();
	private static int lastPage = -1;
	private int currentPage = lastPage;
	private NonNullList<Ingredient> recipe;
	private int startX = -1;
	private boolean update = false;
	private List<String> subpages = new ArrayList<>();
	private int currentSubpage = 0;
	private final int subpageLength = 1285;
	private final String intro1 = Utils.localize("gui.securitycraft:scManual.intro.1").getFormattedText();
	private final String intro2 = Utils.localize("gui.securitycraft:scManual.intro.2").getFormattedText();

	public GuiSCManual() {
	}

	@Override
	public void initGui(){
		byte startY = 2;

		if((width - 256) / 2 != startX && startX != -1)
			update = true;

		startX = (width - 256) / 2;
		Keyboard.enableRepeatEvents(true);

		buttonList.add(new GuiSCManual.ChangePageButton(1, startX + 210, startY + 188, true)); //next page
		buttonList.add(new GuiSCManual.ChangePageButton(2, startX + 16, startY + 188, false)); //previous page
		buttonList.add(new GuiSCManual.ChangePageButton(3, startX + 180, startY + 97, true)); //next subpage
		buttonList.add(new GuiSCManual.ChangePageButton(4, startX + 155, startY + 97, false)); //previous subpage
		updateRecipeAndIcons();
		ItemSCManual.PAGES.sort((page1, page2) -> {
			String key1 = Utils.localize(page1.getItem().getTranslationKey() + ".name").getFormattedText();
			String key2 = Utils.localize(page2.getItem().getTranslationKey() + ".name").getFormattedText();

			return key1.compareTo(key2);
		});
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks){
		drawDefaultBackground();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		if(update)
		{
			updateRecipeAndIcons();
			update = false;
		}

		if(currentPage == -1)
			mc.getTextureManager().bindTexture(infoBookTitlePage);
		else if(recipe != null || ItemSCManual.PAGES.get(currentPage).isRecipeDisabled())
			mc.getTextureManager().bindTexture(infoBookTexture);
		else
			mc.getTextureManager().bindTexture(infoBookTextureSpecial);

		drawTexturedModalRect(startX, 5, 0, 0, 256, 250);

		if(currentPage > -1){
			if(ItemSCManual.PAGES.get(currentPage).getHelpInfo().equals("help.securitycraft:reinforced.info"))
				fontRenderer.drawString(Utils.localize("gui.securitycraft:scManual.reinforced").getFormattedText(), startX + 39, 27, 0, false);
			else
				fontRenderer.drawString(Utils.localize(ItemSCManual.PAGES.get(currentPage).getItem().getTranslationKey() + ".name").getFormattedText(), startX + 39, 27, 0, false);

			fontRenderer.drawSplitString(subpages.get(currentSubpage), startX + 18, 45, 225, 0);

			String designedBy = ItemSCManual.PAGES.get(currentPage).getDesignedBy();

			if(designedBy != null && !designedBy.isEmpty())
				fontRenderer.drawSplitString(Utils.localize("gui.securitycraft:scManual.designedBy", designedBy).getFormattedText(), startX + 18, 150, 75, 0);
		}else{
			fontRenderer.drawString(intro1, width / 2 - fontRenderer.getStringWidth(intro1) / 2, 22, 0, false);
			fontRenderer.drawString(intro2, width / 2 - fontRenderer.getStringWidth(intro2) / 2, 142, 0, false);

			if(I18n.hasKey("gui.securitycraft:scManual.author"))
			{
				String text = Utils.localize("gui.securitycraft:scManual.author").getFormattedText();

				fontRenderer.drawSplitString(text, width / 2 - 175 / 2, 155, 175, 0);
			}
		}

		for(int i = 0; i < buttonList.size(); i++)
			buttonList.get(i).drawButton(mc, mouseX, mouseY, 0);

		if(currentPage != -1)
		{
			if(subpages.size() > 1)
				fontRenderer.drawString((currentSubpage + 1) + "/" + subpages.size(), startX + 205, 102, 0x8E8270);

			String pageNumberText = (currentPage + 2) + "/" + (ItemSCManual.PAGES.size() + 1); //+1 because the "welcome" page is not included

			fontRenderer.drawString(pageNumberText, startX + 240 - fontRenderer.getStringWidth(pageNumberText), 182, 0x8E8270);
		}
		else //render page number on the "welcome" page as well
		{
			String pageNumberText = "1/" + (ItemSCManual.PAGES.size() + 1); //+1 because the "welcome" page is not included

			fontRenderer.drawString(pageNumberText, startX + 240 - fontRenderer.getStringWidth(pageNumberText), 182, 0x8E8270);
		}

		if(currentPage > -1){
			Item item = ItemSCManual.PAGES.get(currentPage).getItem();
			ItemStack stack = new ItemStack(item);
			Block itemBlock = ((item instanceof ItemBlock) ? ((ItemBlock) item).getBlock() : null);
			TileEntity te = itemBlock instanceof ITileEntityProvider ? ((ITileEntityProvider) itemBlock).createNewTileEntity(Minecraft.getMinecraft().world, 0) : null;

			GuiUtils.drawItemStackToGui(stack, startX + 19, 22, !(ItemSCManual.PAGES.get(currentPage).getItem() instanceof ItemBlock));
			mc.getTextureManager().bindTexture(infoBookIcons);

			if(itemBlock != null){
				if(itemBlock instanceof IExplosive)
					drawTexturedModalRect(startX + 107, 117, 54, 1, 18, 18);

				if(te != null){
					if(te instanceof IOwnable)
						drawTexturedModalRect(startX + 29, 118, 1, 1, 16, 16);

					if(te instanceof IPasswordProtected)
						drawTexturedModalRect(startX + 55, 118, 18, 1, 17, 16);

					if(te instanceof TileEntitySCTE && ((TileEntitySCTE) te).isActivatedByView())
						drawTexturedModalRect(startX + 81, 118, 36, 1, 17, 16);

					if(te instanceof ICustomizable)
					{
						ICustomizable scte = (ICustomizable)te;

						this.drawTexturedModalRect(startX + 213, 118, 72, 1, 16, 16);

						if(scte.customOptions() != null && scte.customOptions().length > 0)
							drawTexturedModalRect(startX + 136, 118, 88, 1, 16, 16);
					}

					if(te instanceof IModuleInventory)
					{
						if(((IModuleInventory)te).acceptedModules() != null && ((IModuleInventory)te).acceptedModules().length > 0)
							drawTexturedModalRect(startX + 163, 118, 105, 1, 16, 16);
					}
				}
			}

			if(recipe != null)
			{
				for(int i = 0; i < 3; i++)
					for(int j = 0; j < 3; j++)
					{
						if(((i * 3) + j) >= recipe.size())
							break;

						ItemStack[] matchingStacks = recipe.get((i * 3) + j).getMatchingStacks();

						if(matchingStacks.length == 0 || matchingStacks[0].isEmpty())
							continue;

						GuiUtils.drawItemStackToGui(matchingStacks[0], (startX + 101) + (j * 19), 144 + (i * 19), !(matchingStacks[0].getItem() instanceof ItemBlock));
					}
			}

			for(HoverChecker chc : hoverCheckers)
			{
				if(chc != null && chc.checkHover(mouseX, mouseY))
				{
					if(chc instanceof StackHoverChecker && !((StackHoverChecker)chc).getStack().isEmpty())
						renderToolTip(((StackHoverChecker)chc).getStack(), mouseX, mouseY);
					else if(chc instanceof StringHoverChecker && ((StringHoverChecker)chc).getName() != null)
						drawHoveringText(((StringHoverChecker)chc).getLines(), mouseX, mouseY);
				}
			}
		}
	}

	@Override
	public void onGuiClosed(){
		super.onGuiClosed();
		lastPage = currentPage;
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException{
		super.keyTyped(typedChar, keyCode);

		if(keyCode == Keyboard.KEY_LEFT)
			previousSubpage();
		else if(keyCode == Keyboard.KEY_RIGHT)
			nextSubpage();
	}

	@Override
	protected void actionPerformed(GuiButton button){
		if(button.id == 1)
			nextPage();
		else if(button.id == 2)
			previousPage();
		else if(button.id == 3)
			nextSubpage();
		else if(button.id == 4)
			previousSubpage();

		//hide subpage buttons on main page
		buttonList.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttonList.get(3).visible = currentPage != -1 && subpages.size() > 1;
	}

	@Override
	public void handleMouseInput() throws IOException
	{
		super.handleMouseInput();

		switch((int)Math.signum(Mouse.getEventDWheel()))
		{
			case -1: nextPage(); break;
			case 1: previousPage(); break;
		}

		//hide subpage buttons on main page
		buttonList.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttonList.get(3).visible = currentPage != -1 && subpages.size() > 1;
	}

	private void nextPage()
	{
		currentPage++;

		if(currentPage > ItemSCManual.PAGES.size() - 1)
			currentPage = -1;

		updateRecipeAndIcons();
	}

	private void previousPage()
	{
		currentPage--;

		if(currentPage < -1)
			currentPage = ItemSCManual.PAGES.size() - 1;

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

		if(currentPage < 0){
			recipe = null;
			hoverCheckers.clear();
			return;
		}

		hoverCheckers.clear();

		SCManualPage page = ItemSCManual.PAGES.get(currentPage);

		if(page.hasCustomRecipe())
			recipe = page.getRecipe();
		else
			for(int o = 0; o < CraftingManager.REGISTRY.getKeys().size(); o++)
			{
				IRecipe object = CraftingManager.REGISTRY.getObjectById(o);

				if(object instanceof ShapedRecipes){
					ShapedRecipes recipe = (ShapedRecipes) object;

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
				}else if(object instanceof ShapelessRecipes){
					ShapelessRecipes recipe = (ShapelessRecipes) object;

					if(!recipe.getRecipeOutput().isEmpty() && recipe.getRecipeOutput().getItem() == page.getItem()){
						//don't show keycard reset recipes
						if(recipe.getRegistryName().getPath().endsWith("_reset"))
							continue;

						NonNullList<Ingredient> recipeItems = NonNullList.<Ingredient>withSize(recipe.recipeItems.size(), Ingredient.EMPTY);

						for(int i = 0; i < recipeItems.size(); i++)
							recipeItems.set(i, recipe.recipeItems.get(i));

						this.recipe = recipeItems;
						break;
					}
				}

				recipe = null;
			}

		String helpInfo = page.getHelpInfo();
		boolean reinforcedPage = helpInfo.equals("help.securitycraft:reinforced.info") || helpInfo.contains("reinforced_hopper");

		if(recipe != null && !reinforcedPage)
		{
			outer: for(int i = 0; i < 3; i++)
			{
				for(int j = 0; j < 3; j++)
				{
					if((i * 3) + j == recipe.size())
						break outer;

					if(recipe.get((i * 3) + j).getMatchingStacks().length > 0 && !recipe.get((i * 3) + j).getMatchingStacks()[0].isEmpty())
						hoverCheckers.add(new StackHoverChecker(recipe.get((i * 3) + j).getMatchingStacks()[0], 144 + (i * 19), 144 + (i * 19) + 16, (startX + 101) + (j * 19), (startX + 100) + (j * 19) + 16));
				}
			}
		}
		else if(page.isRecipeDisabled())
			hoverCheckers.add(new StringHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.disabled").getFormattedText()));
		else if(reinforcedPage)
		{
			recipe = null;
			hoverCheckers.add(new StringHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.recipe.reinforced").getFormattedText()));
		}
		else
		{
			String name = page.getItem().getRegistryName().getPath();

			hoverCheckers.add(new StringHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.recipe." + name).getFormattedText()));
		}

		Item item = page.getItem();
		TileEntity te = ((item instanceof ItemBlock && ((ItemBlock) item).getBlock() instanceof ITileEntityProvider) ? ((ITileEntityProvider) ((ItemBlock) item).getBlock()).createNewTileEntity(Minecraft.getMinecraft().world, 0) : null);
		Block block = ((item instanceof ItemBlock) ? ((ItemBlock) item).getBlock() : null);

		if(te != null){
			if(te instanceof IOwnable)
				hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 29, (startX + 29) + 16, Utils.localize("gui.securitycraft:scManual.ownableBlock").getFormattedText()));

			if(te instanceof IPasswordProtected)
				hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 55, (startX + 55) + 16, Utils.localize("gui.securitycraft:scManual.passwordProtectedBlock").getFormattedText()));

			if(te instanceof TileEntitySCTE && ((TileEntitySCTE) te).isActivatedByView())
				hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 81, (startX + 81) + 16, Utils.localize("gui.securitycraft:scManual.viewActivatedBlock").getFormattedText()));

			if(block instanceof IExplosive)
				hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 107, (startX + 107) + 16, Utils.localize("gui.securitycraft:scManual.explosiveBlock").getFormattedText()));

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
						display.add("- " + Utils.localize("option." + block.getTranslationKey().substring(5) + "." + option.getName() + ".description").getFormattedText());
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

					for(EnumModuleType module : moduleInv.acceptedModules())
					{
						display.add("- " + Utils.localize("module." + block.getTranslationKey().substring(5) + "." + module.getItem().getTranslationKey().substring(5).replace("securitycraft:", "") + ".description").getFormattedText());
						display.add("");
					}

					display.remove(display.size() - 1);
					hoverCheckers.add(new StringHoverChecker(118, 118 + 16, startX + 163, (startX + 163) + 16, display));
				}
			}
		}

		//set up subpages
		helpInfo = Utils.localize(page.getHelpInfo()).getFormattedText();
		subpages.clear();

		while(fontRenderer.getStringWidth(helpInfo) > subpageLength)
		{
			String trimmed = fontRenderer.trimStringToWidth(helpInfo, 1285);
			int temp = trimmed.lastIndexOf(' ');
			if(temp > 0)
				trimmed = trimmed.trim().substring(0, temp); //remove last word to remove the possibility to break it up onto multiple pages
			trimmed = trimmed.trim();
			subpages.add(trimmed);
			helpInfo = helpInfo.replace(trimmed, "").trim();
		}

		subpages.add(helpInfo);
	}

	@SideOnly(Side.CLIENT)
	static class ChangePageButton extends GuiButton {
		private final boolean isForward;

		public ChangePageButton(int index, int xPos, int yPos, boolean forward){
			super(index, xPos, yPos, 23, 13, "");
			isForward = forward;
		}

		/**
		 * Draws this button to the screen.
		 */
		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks){
			if(visible){
				boolean isHovering = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				mc.getTextureManager().bindTexture(bookGuiTextures);
				int textureX = 0;
				int textureY = 192;

				if(isHovering)
					textureX += 23;

				if(!isForward)
					textureY += 13;

				this.drawTexturedModalRect(x, y, textureX, textureY, 23, 13);
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