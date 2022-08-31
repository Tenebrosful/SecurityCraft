package net.geforcemods.securitycraft.screen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.ICustomizable;
import net.geforcemods.securitycraft.api.IExplosive;
import net.geforcemods.securitycraft.api.ILockable;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasswordProtected;
import net.geforcemods.securitycraft.api.IViewActivated;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.items.SCManualItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.PageGroup;
import net.geforcemods.securitycraft.misc.SCManualPage;
import net.geforcemods.securitycraft.screen.components.ColorableScrollPanel;
import net.geforcemods.securitycraft.screen.components.HoverChecker;
import net.geforcemods.securitycraft.screen.components.IngredientDisplay;
import net.geforcemods.securitycraft.screen.components.TextHoverChecker;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;
import net.minecraftforge.fml.loading.FMLEnvironment;

@OnlyIn(Dist.CLIENT)
public class SCManualScreen extends Screen {
	private static final ResourceLocation PAGE = new ResourceLocation("securitycraft:textures/gui/info_book_texture.png");
	private static final ResourceLocation PAGE_WITH_SCROLL = new ResourceLocation("securitycraft:textures/gui/info_book_texture_special.png"); //for items without a recipe
	private static final ResourceLocation TITLE_PAGE = new ResourceLocation("securitycraft:textures/gui/info_book_title_page.png");
	private static final ResourceLocation ICONS = new ResourceLocation("securitycraft:textures/gui/info_book_icons.png");
	private static final ResourceLocation VANILLA_BOOK = new ResourceLocation("textures/gui/book.png");
	private static final int SUBPAGE_LENGTH = 1285;
	private static int lastPage = -1;
	private final IFormattableTextComponent intro1 = Utils.localize("gui.securitycraft:scManual.intro.1").setStyle(Style.EMPTY.setUnderlined(true));
	private final TranslationTextComponent ourPatrons = Utils.localize("gui.securitycraft:scManual.patreon.title");
	private List<HoverChecker> hoverCheckers = new ArrayList<>();
	private int currentPage = lastPage;
	private NonNullList<Ingredient> recipe;
	private IngredientDisplay[] displays = new IngredientDisplay[9];
	private int startX = -1;
	private List<ITextProperties> subpages = new ArrayList<>();
	private List<IReorderingProcessor> author = new ArrayList<>();
	private int currentSubpage = 0;
	private List<IReorderingProcessor> intro2;
	private PatronList patronList;
	private Button patreonLinkButton;
	private boolean explosive, ownable, passwordProtected, viewActivated, customizable, lockable, moduleInventory;
	private IngredientDisplay pageIcon;
	private TranslationTextComponent pageTitle, designedBy;
	private PageGroup pageGroup = PageGroup.NONE;

	public SCManualScreen() {
		super(new TranslationTextComponent(SCContent.SC_MANUAL.get().getDescriptionId()));
	}

	@Override
	public void init() {
		byte startY = 2;

		startX = (width - 256) / 2;
		minecraft.keyboardHandler.setSendRepeatsToGui(true);
		addButton(new ChangePageButton(startX + 210, startY + 188, true, b -> nextPage()));
		addButton(new ChangePageButton(startX + 22, startY + 188, false, b -> previousPage()));
		addButton(new ChangePageButton(startX + 180, startY + 97, true, b -> nextSubpage()));
		addButton(new ChangePageButton(startX + 155, startY + 97, false, b -> previousSubpage()));
		addButton(patreonLinkButton = new HyperlinkButton(startX + 225, 143, 16, 16, StringTextComponent.EMPTY, b -> handleComponentClicked(Style.EMPTY.withClickEvent(new ClickEvent(Action.OPEN_URL, "https://www.patreon.com/Geforce")))));
		children.add(patronList = new PatronList(minecraft, 115, 90, 50, startX + 125));

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				displays[(i * 3) + j] = new IngredientDisplay((startX + 101) + (j * 19), 144 + (i * 19));
			}
		}

		pageIcon = new IngredientDisplay(startX + 19, 22);
		updateRecipeAndIcons();
		SCManualItem.PAGES.sort((page1, page2) -> {
			String key1 = page1.getTitle().getString();
			String key2 = page2.getTitle().getString();

			return key1.compareTo(key2);
		});
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		renderBackground(matrix);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		if (currentPage == -1)
			minecraft.getTextureManager().bind(TITLE_PAGE);
		else if (recipe != null && recipe.size() > 0)
			minecraft.getTextureManager().bind(PAGE);
		else
			minecraft.getTextureManager().bind(PAGE_WITH_SCROLL);

		blit(matrix, startX, 5, 0, 0, 256, 250);

		for (int i = 0; i < buttons.size(); i++) {
			buttons.get(i).render(matrix, mouseX, mouseY, partialTicks);
		}

		if (currentPage > -1) {
			String pageNumberText = (currentPage + 2) + "/" + (SCManualItem.PAGES.size() + 1); //+1 because the "welcome" page is not included

			if (subpages.size() > 1)
				font.draw(matrix, (currentSubpage + 1) + "/" + subpages.size(), startX + 205, 102, 0x8E8270);

			if (designedBy != null)
				font.drawWordWrap(designedBy, startX + 18, 150, 75, 0);

			font.draw(matrix, pageTitle, startX + 39, 27, 0);
			font.drawWordWrap(subpages.get(currentSubpage), startX + 18, 45, 225, 0);
			font.draw(matrix, pageNumberText, startX + 240 - font.width(pageNumberText), 182, 0x8E8270);
			pageIcon.render(minecraft, partialTicks);
			minecraft.getTextureManager().bind(ICONS);

			if (ownable)
				blit(matrix, startX + 29, 118, 1, 1, 16, 16);

			if (passwordProtected)
				blit(matrix, startX + 55, 118, 18, 1, 17, 16);

			if (viewActivated)
				blit(matrix, startX + 81, 118, 36, 1, 17, 16);

			if (explosive)
				blit(matrix, startX + 107, 117, 54, 1, 18, 18);

			if (customizable)
				blit(matrix, startX + 136, 118, 88, 1, 16, 16);

			if (moduleInventory)
				blit(matrix, startX + 163, 118, 105, 1, 16, 16);

			if (lockable)
				blit(matrix, startX + 189, 118, 154, 1, 16, 16);

			if (customizable || moduleInventory)
				blit(matrix, startX + 213, 118, 72, 1, 16, 16);

			for (IngredientDisplay display : displays) {
				display.render(minecraft, partialTicks);
			}

			for (int i = 0; i < hoverCheckers.size(); i++) {
				HoverChecker chc = hoverCheckers.get(i);

				if (chc != null && chc.checkHover(mouseX, mouseY)) {
					if (chc instanceof TextHoverChecker && ((TextHoverChecker) chc).getName() != null)
						GuiUtils.drawHoveringText(matrix, ((TextHoverChecker) chc).getLines(), mouseX, mouseY, width, height, -1, font);
					else if (i < displays.length && !displays[i].getCurrentStack().isEmpty())
						renderTooltip(matrix, displays[i].getCurrentStack(), mouseX, mouseY);
				}
			}
		}
		else { //"welcome" page
			String pageNumberText = "1/" + (SCManualItem.PAGES.size() + 1); //+1 because the "welcome" page is not included

			font.draw(matrix, intro1, width / 2 - font.width(intro1) / 2, 22, 0);

			for (int i = 0; i < intro2.size(); i++) {
				IReorderingProcessor text = intro2.get(i);

				font.draw(matrix, text, width / 2 - font.width(text) / 2, 150 + 10 * i, 0);
			}

			for (int i = 0; i < author.size(); i++) {
				IReorderingProcessor text = author.get(i);

				font.draw(matrix, text, width / 2 - font.width(text) / 2, 180 + 10 * i, 0);
			}

			//the patreon link button may overlap with a name tooltip from the list, so draw the list after the buttons
			if (patronList != null)
				patronList.render(matrix, mouseX, mouseY, partialTicks);

			font.draw(matrix, pageNumberText, startX + 240 - font.width(pageNumberText), 182, 0x8E8270);
			font.draw(matrix, ourPatrons, width / 2 - font.width(ourPatrons) / 2 + 30, 40, 0);
		}
	}

	@Override
	public void removed() {
		super.removed();
		lastPage = currentPage;
		minecraft.keyboardHandler.setSendRepeatsToGui(false);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_LEFT)
			previousSubpage();
		else if (keyCode == GLFW.GLFW_KEY_RIGHT)
			nextSubpage();

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void hideSubpageButtonsOnMainPage() {
		buttons.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttons.get(3).visible = currentPage != -1 && subpages.size() > 1;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
		if (Screen.hasShiftDown()) {
			for (IngredientDisplay display : displays) {
				if (display != null)
					display.changeRenderingStack(-scroll);
			}

			if (pageIcon != null)
				pageIcon.changeRenderingStack(-scroll);

			return true;
		}

		if (currentPage == -1 && patronList != null && patronList.isMouseOver(mouseX, mouseY) && !patronList.patrons.isEmpty()) {
			patronList.mouseScrolled(mouseX, mouseY, scroll);
			return true;
		}

		switch ((int) Math.signum(scroll)) {
			case -1:
				nextPage();
				break;
			case 1:
				previousPage();
				break;
		}

		//hide subpage buttons on main page
		buttons.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttons.get(3).visible = currentPage != -1 && subpages.size() > 1;
		return true;
	}

	private void nextPage() {
		currentPage++;

		if (currentPage > SCManualItem.PAGES.size() - 1)
			currentPage = -1;

		updateRecipeAndIcons();
		hideSubpageButtonsOnMainPage();
	}

	private void previousPage() {
		currentPage--;

		if (currentPage < -1)
			currentPage = SCManualItem.PAGES.size() - 1;

		updateRecipeAndIcons();
		hideSubpageButtonsOnMainPage();
	}

	private void nextSubpage() {
		currentSubpage++;

		if (currentSubpage == subpages.size())
			currentSubpage = 0;
	}

	private void previousSubpage() {
		currentSubpage--;

		if (currentSubpage == -1)
			currentSubpage = subpages.size() - 1;
	}

	private void updateRecipeAndIcons() {
		currentSubpage = 0;
		hoverCheckers.clear();
		patreonLinkButton.visible = currentPage == -1;

		if (currentPage < 0) {
			for (IngredientDisplay display : displays) {
				display.setIngredient(Ingredient.EMPTY);
			}

			pageIcon.setIngredient(Ingredient.EMPTY);
			recipe = null;
			buttons.get(2).visible = false;
			buttons.get(3).visible = false;

			if (I18n.exists("gui.securitycraft:scManual.author"))
				author = font.split(Utils.localize("gui.securitycraft:scManual.author"), 180);
			else
				author.clear();

			intro2 = font.split(Utils.localize("gui.securitycraft:scManual.intro.2"), 202);
			patronList.fetchPatrons();
			return;
		}

		SCManualPage page = SCManualItem.PAGES.get(currentPage);
		String designedBy = page.getDesignedBy();
		Item item = page.getItem();

		if (designedBy != null && !designedBy.isEmpty())
			this.designedBy = Utils.localize("gui.securitycraft:scManual.designedBy", designedBy);
		else
			this.designedBy = null;

		recipe = null;
		pageGroup = page.getGroup();

		if (pageGroup == PageGroup.NONE) {
			for (IRecipe<?> object : Minecraft.getInstance().level.getRecipeManager().getRecipes()) {
				if (object instanceof ShapedRecipe) {
					ShapedRecipe recipe = (ShapedRecipe) object;

					if (!recipe.getResultItem().isEmpty() && recipe.getResultItem().getItem() == page.getItem()) {
						NonNullList<Ingredient> ingredients = recipe.getIngredients();
						NonNullList<Ingredient> recipeItems = NonNullList.<Ingredient> withSize(9, Ingredient.EMPTY);

						for (int i = 0; i < ingredients.size(); i++) {
							recipeItems.set(getCraftMatrixPosition(i, recipe.getWidth(), recipe.getHeight()), ingredients.get(i));
						}

						this.recipe = recipeItems;
						break;
					}
				}
				else if (object instanceof ShapelessRecipe) {
					ShapelessRecipe recipe = (ShapelessRecipe) object;

					if (!recipe.getResultItem().isEmpty() && recipe.getResultItem().getItem() == page.getItem()) {
						//don't show keycard reset recipes
						if (recipe.getId().getPath().endsWith("_reset"))
							continue;

						NonNullList<Ingredient> recipeItems = NonNullList.<Ingredient> withSize(recipe.getIngredients().size(), Ingredient.EMPTY);

						for (int i = 0; i < recipeItems.size(); i++) {
							recipeItems.set(i, recipe.getIngredients().get(i));
						}

						this.recipe = recipeItems;
						break;
					}
				}
			}
		}
		else if (pageGroup.hasRecipeGrid()) {
			Map<Integer, ItemStack[]> recipeStacks = new HashMap<>();
			List<Item> pageItems = Arrays.stream(pageGroup.getItems().getItems()).map(ItemStack::getItem).collect(Collectors.toList());
			int stacksLeft = pageItems.size();

			for (int i = 0; i < 9; i++) {
				recipeStacks.put(i, new ItemStack[pageItems.size()]);
			}

			for (IRecipe<?> object : Minecraft.getInstance().level.getRecipeManager().getRecipes()) {
				if (stacksLeft == 0)
					break;

				if (object instanceof ShapedRecipe) {
					ShapedRecipe recipe = (ShapedRecipe) object;

					if (!recipe.getResultItem().isEmpty() && pageItems.contains(recipe.getResultItem().getItem())) {
						NonNullList<Ingredient> ingredients = recipe.getIngredients();

						for (int i = 0; i < ingredients.size(); i++) {
							ItemStack[] items = ingredients.get(i).getItems();

							if (items.length == 0)
								continue;

							int indexToAddAt = pageItems.indexOf(recipe.getResultItem().getItem());

							//first item needs to suffice since multiple recipes are being cycled through
							recipeStacks.get(getCraftMatrixPosition(i, recipe.getWidth(), recipe.getHeight()))[indexToAddAt] = items[0];
						}

						stacksLeft--;
					}
				}
				else if (object instanceof ShapelessRecipe) {
					ShapelessRecipe recipe = (ShapelessRecipe) object;

					if (!recipe.getResultItem().isEmpty() && pageItems.contains(recipe.getResultItem().getItem())) {
						//don't show keycard reset recipes
						if (recipe.getId().getPath().endsWith("_reset"))
							continue;

						NonNullList<Ingredient> ingredients = recipe.getIngredients();

						for (int i = 0; i < ingredients.size(); i++) {
							ItemStack[] items = ingredients.get(i).getItems();

							if (items.length == 0)
								continue;

							int indexToAddAt = pageItems.indexOf(recipe.getResultItem().getItem());

							//first item needs to suffice since multiple recipes are being cycled through
							recipeStacks.get(i)[indexToAddAt] = items[0];
						}

						stacksLeft--;
					}
				}
			}

			recipe = NonNullList.withSize(9, Ingredient.EMPTY);
			recipeStacks.forEach((i, stackArray) -> recipe.set(i, Ingredient.of(Arrays.stream(stackArray).map(s -> s == null ? ItemStack.EMPTY : s))));
		}

		if (page.hasRecipeDescription()) {
			String name = page.getItem().getRegistryName().getPath();

			hoverCheckers.add(new TextHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.recipe." + name)));
		}
		else if (pageGroup == PageGroup.REINFORCED || item == SCContent.REINFORCED_HOPPER.get().asItem()) {
			recipe = null;
			hoverCheckers.add(new TextHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.recipe.reinforced")));
		}
		else if (recipe != null) {
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					hoverCheckers.add(new HoverChecker(144 + (row * 19), 144 + (row * 19) + 16, (startX + 101) + (column * 19), (startX + 101) + (column * 19) + 16));
				}
			}
		}
		else
			hoverCheckers.add(new TextHoverChecker(144, 144 + (2 * 20) + 16, startX + 100, (startX + 100) + (2 * 20) + 16, Utils.localize("gui.securitycraft:scManual.disabled")));

		pageTitle = page.getTitle();

		if (pageGroup != PageGroup.NONE)
			pageIcon.setIngredient(pageGroup.getItems());
		else
			pageIcon.setIngredient(Ingredient.of(item));

		resetTileEntityInfo();

		if (item instanceof BlockItem) {
			Block block = ((BlockItem) item).getBlock();

			if (explosive = block instanceof IExplosive)
				hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 107, (startX + 107) + 16, Utils.localize("gui.securitycraft:scManual.explosiveBlock")));

			if (block.hasTileEntity(block.defaultBlockState())) {
				TileEntity te = block.createTileEntity(block.defaultBlockState(), Minecraft.getInstance().level);

				if (ownable = te instanceof IOwnable)
					hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 29, (startX + 29) + 16, Utils.localize("gui.securitycraft:scManual.ownableBlock")));

				if (passwordProtected = te instanceof IPasswordProtected)
					hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 55, (startX + 55) + 16, Utils.localize("gui.securitycraft:scManual.passwordProtectedBlock")));

				if (viewActivated = te instanceof IViewActivated)
					hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 81, (startX + 81) + 16, Utils.localize("gui.securitycraft:scManual.viewActivatedBlock")));

				if (te instanceof ICustomizable) {
					ICustomizable scte = (ICustomizable) te;
					Option<?>[] options = scte.customOptions();

					if (options != null && options.length > 0) {
						List<ITextComponent> display = new ArrayList<>();

						customizable = true;
						display.add(Utils.localize("gui.securitycraft:scManual.options"));
						display.add(new StringTextComponent("---"));

						for (Option<?> option : options) {
							display.add(new StringTextComponent("- ").append(Utils.localize(option.getDescriptionKey(block))));
							display.add(StringTextComponent.EMPTY);
						}

						display.remove(display.size() - 1);
						hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 136, (startX + 136) + 16, display));
					}
				}

				if (te instanceof IModuleInventory) {
					IModuleInventory moduleInv = (IModuleInventory) te;

					if (moduleInv.acceptedModules() != null && moduleInv.acceptedModules().length > 0) {
						List<ITextComponent> display = new ArrayList<>();

						moduleInventory = true;
						display.add(Utils.localize("gui.securitycraft:scManual.modules"));
						display.add(new StringTextComponent("---"));

						for (ModuleType module : moduleInv.acceptedModules()) {
							display.add(new StringTextComponent("- ").append(Utils.localize("module" + block.getDescriptionId().substring(5) + "." + module.getItem().getDescriptionId().substring(5).replace("securitycraft.", "") + ".description")));
							display.add(StringTextComponent.EMPTY);
						}

						display.remove(display.size() - 1);
						hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 163, (startX + 163) + 16, display));
					}
				}

				if (lockable = te instanceof ILockable)
					hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 189, startX + 189 + 16, Utils.localize("gui.securitycraft:scManual.lockable")));

				if (customizable || moduleInventory)
					hoverCheckers.add(new TextHoverChecker(118, 118 + 16, startX + 213, (startX + 213) + 16, Utils.localize("gui.securitycraft:scManual.customizableBlock")));
			}
		}

		if (recipe != null && recipe.size() > 0) {
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					int index = (i * 3) + j;

					if (index >= recipe.size())
						displays[index].setIngredient(Ingredient.EMPTY);
					else
						displays[index].setIngredient(recipe.get(index));
				}
			}
		}
		else {
			for (IngredientDisplay display : displays) {
				display.setIngredient(Ingredient.EMPTY);
			}
		}

		//set up subpages
		subpages = font.getSplitter().splitLines(page.getHelpInfo(), SUBPAGE_LENGTH, Style.EMPTY);
		buttons.get(2).visible = currentPage != -1 && subpages.size() > 1;
		buttons.get(3).visible = currentPage != -1 && subpages.size() > 1;
	}

	private void resetTileEntityInfo() {
		explosive = false;
		ownable = false;
		passwordProtected = false;
		viewActivated = false;
		customizable = false;
		lockable = false;
		moduleInventory = false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (patronList != null)
			patronList.mouseClicked(mouseX, mouseY, button);

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (patronList != null)
			patronList.mouseReleased(mouseX, mouseY, button);

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (patronList != null)
			patronList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	class PatronList extends ColorableScrollPanel {
		private final String patronListLink = FMLEnvironment.production ? "https://gist.githubusercontent.com/bl4ckscor3/bdda6596012b1206816db034350b5717/raw" : "https://gist.githubusercontent.com/bl4ckscor3/3196e6740774e386871a74a9606eaa61/raw";
		private final int slotHeight = 12;
		private final ExecutorService executor = Executors.newSingleThreadExecutor();
		private Future<List<String>> patronRequestFuture;
		private List<String> patrons = new ArrayList<>();
		private boolean patronsAvailable = false;
		private boolean error = false;
		private boolean patronsRequested;
		private final List<IReorderingProcessor> fetchErrorLines;
		private final List<IReorderingProcessor> noPatronsLines;
		private final ITextComponent loadingText = Utils.localize("gui.securitycraft:scManual.patreon.loading");

		public PatronList(Minecraft client, int width, int height, int top, int left) {
			super(client, width, height, top, left, new Color(0xC0, 0xBF, 0xBB, 0xB2), new Color(0xD0, 0xBF, 0xBB, 0xB2), new Color(0x8E, 0x82, 0x70, 0xFF), new Color(0x80, 0x70, 0x55, 0xFF), new Color(0xD1, 0xBF, 0xA1, 0xFF));

			fetchErrorLines = font.split(Utils.localize("gui.securitycraft:scManual.patreon.error"), width);
			noPatronsLines = font.split(Utils.localize("advancements.empty"), width - 10);
		}

		@Override
		protected int getContentHeight() {
			int height = patrons.size() * (font.lineHeight + 3);

			if (height < bottom - top - 8)
				height = bottom - top - 8;

			return height;
		}

		@Override
		public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
			if (patronsAvailable) {
				int baseY = top + border - (int) scrollDistance;

				super.render(matrix, mouseX, mouseY, partialTicks);

				//draw tooltip for long patron names
				int mouseListY = (int) (mouseY - top + scrollDistance - border);
				int slotIndex = mouseListY / slotHeight;

				if (mouseX >= left && mouseX < right - 6 && slotIndex >= 0 && mouseListY >= 0 && slotIndex < patrons.size() && mouseY >= top && mouseY <= bottom) {
					String patron = patrons.get(slotIndex);
					int length = font.width(patron);

					if (length >= width - barWidth)
						renderTooltip(matrix, new StringTextComponent(patron), left - 10, baseY + (slotHeight * slotIndex + slotHeight));
				}

				if (patrons.isEmpty()) {
					for (int i = 0; i < noPatronsLines.size(); i++) {
						IReorderingProcessor line = noPatronsLines.get(i);

						font.draw(matrix, line, left + width / 2 - font.width(line) / 2, top + 30 + i * 10, 0xFF333333);
					}
				}
			}
			else if (error) {
				for (int i = 0; i < fetchErrorLines.size(); i++) {
					IReorderingProcessor line = fetchErrorLines.get(i);

					font.draw(matrix, line, left + width / 2 - font.width(line) / 2, top + 30 + i * 10, 0xFFB00101);
				}
			}
			else if (patronRequestFuture != null && patronRequestFuture.isDone()) {
				try {
					patrons = patronRequestFuture.get();
					executor.shutdown();
					patronsAvailable = true;
				}
				catch (InterruptedException | ExecutionException e) {
					error = true;
				}
			}
			else {
				font.draw(matrix, loadingText, left + width / 2 - font.width(loadingText) / 2, top + 30, 0);
			}
		}

		@Override
		protected void drawPanel(MatrixStack matrix, int entryRight, int relativeY, Tessellator tesselator, int mouseX, int mouseY) {
			//draw entry strings
			for (int i = 0; i < patrons.size(); i++) {
				String patron = patrons.get(i);

				if (patron != null && !patron.isEmpty())
					font.draw(matrix, patron, left + 2, relativeY + (slotHeight * i), 0);
			}
		}

		public void fetchPatrons() {
			if (!patronsRequested) {
				//create thread to fetch patrons. without this, and for example if the player has no internet connection, the game will hang
				patronRequestFuture = executor.submit(() -> {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(patronListLink).openStream()))) {
						return reader.lines().collect(Collectors.toList());
					}
					catch (IOException e) {
						error = true;
						return new ArrayList<>();
					}
				});
				patronsRequested = true;
			}
		}
	}

	static class ChangePageButton extends ExtendedButton {
		private final int textureY;

		public ChangePageButton(int xPos, int yPos, boolean forward, IPressable onClick) {
			super(xPos, yPos, 23, 13, StringTextComponent.EMPTY, onClick);
			textureY = forward ? 192 : 205;
		}

		@Override
		public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
			if (visible) {
				isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				Minecraft.getInstance().getTextureManager().bind(VANILLA_BOOK);
				blit(matrix, x, y, isHovered() ? 23 : 0, textureY, 23, 13);
			}
		}
	}

	class HyperlinkButton extends Button {
		public HyperlinkButton(int xPos, int yPos, int width, int height, ITextComponent displayString, IPressable handler) {
			super(xPos, yPos, width, height, displayString, handler);
		}

		@Override
		public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partial) {
			minecraft.getTextureManager().bind(ICONS);
			isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
			blit(matrix, x, y, isHovered() ? 138 : 122, 1, 16, 16);
		}
	}

	//from JEI
	private int getCraftMatrixPosition(int i, int width, int height) {
		int index;

		if (width == 1) {
			if (height == 3)
				index = (i * 3) + 1;
			else if (height == 2)
				index = (i * 3) + 1;
			else
				index = 4;
		}
		else if (height == 1)
			index = i + 3;
		else if (width == 2) {
			index = i;

			if (i > 1) {
				index++;

				if (i > 3)
					index++;
			}
		}
		else if (height == 2)
			index = i + 3;
		else
			index = i;

		return index;
	}
}
