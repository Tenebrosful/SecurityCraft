package net.geforcemods.securitycraft.screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.entity.SentryEntity;
import net.geforcemods.securitycraft.entity.SentryEntity.SentryMode;
import net.geforcemods.securitycraft.network.server.SetSentryMode;
import net.geforcemods.securitycraft.network.server.UpdateNBTTagOnServer;
import net.geforcemods.securitycraft.screen.components.IdButton;
import net.geforcemods.securitycraft.screen.components.StringHoverChecker;
import net.geforcemods.securitycraft.screen.components.TogglePictureButton;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class SentryRemoteAccessToolScreen extends Screen {

	private static final ResourceLocation TEXTURE = new ResourceLocation(SecurityCraft.MODID, "textures/gui/container/srat.png");
	private static final ResourceLocation SENTRY_ICONS = new ResourceLocation(SecurityCraft.MODID, "textures/gui/container/sentry_icons.png");
	private ItemStack srat;
	private IdButton[][] guiButtons = new IdButton[12][3]; // 12 sentries, 3 actions (mode, targets, unbind)
	private ITextComponent[] names = new ITextComponent[12];
	private IdButton[] guiButtonsGlobal = new IdButton[3];
	private static final int MODE = 0, TARGETS = 1, UNBIND = 2;
	private int xSize = 440, ySize = 215;
	private static final int SENTRY_TRACKING_RANGE = 256; // as defined when registering SentryEntity
	private int viewDistance;
	private List<StringHoverChecker> hoverCheckers = new ArrayList<>();

	public SentryRemoteAccessToolScreen(ItemStack item, int viewDistance) {
		super(new TranslationTextComponent(item.getTranslationKey()));

		srat = item;
		this.viewDistance = viewDistance;
	}

	@Override
	public void init() {
		super.init();

		int startX = (width - xSize) / 2;
		int startY = (height - ySize) / 2;
		int paddingX = 22;
		int paddingY = 25;
		int[] coords = null;
		int id = 0;
		boolean foundSentry = false;
		int[] modeTextureX = {0, 16, 32};
		int[] targetTextureX = {48, 64, 80};
		int[] yStarts = {0, 0, 0};

		hoverCheckers.clear();

		for (int i = 0; i < 12; i++) {
			int x = (i / 6) * xSize / 2; //first six sentries in the left column, second six sentries in the right column
			int y = ((i % 6) + 1) * 30 + paddingY;
			coords = getSentryCoordinates(i);

			for (int j = 0; j < 3; j++) {
				int btnX = startX + j * paddingX + 147 + x;
				int btnY = startY + y - 48;

				switch (j) {
					case MODE:
						guiButtons[i][j] = new TogglePictureButton(id++, btnX, btnY, 20, 20, SENTRY_ICONS, modeTextureX, yStarts, 2, 3, this::actionPerformed);
						guiButtons[i][j].active = false;
						break;
					case TARGETS:
						guiButtons[i][j] = new TogglePictureButton(id++, btnX, btnY, 20, 20, SENTRY_ICONS, targetTextureX, yStarts, 2, 3, this::actionPerformed);
						guiButtons[i][j].active = false;
						break;
					case UNBIND:
						guiButtons[i][j] = new IdButton(id++, btnX, btnY, 20, 20, "X", this::clickUnbind);
						guiButtons[i][j].active = false;
						break;
				}

				addButton(guiButtons[i][j]);
			}

			BlockPos sentryPos = new BlockPos(coords[0], coords[1], coords[2]);

			if (!(coords[0] == 0 && coords[1] == 0 && coords[2] == 0)) {
				guiButtons[i][UNBIND].active = true;

				if (Minecraft.getInstance().player.world.isBlockPresent(sentryPos) && isSentryVisibleToPlayer(sentryPos)) {
					List<SentryEntity> sentries = Minecraft.getInstance().player.world.getEntitiesWithinAABB(SentryEntity.class, new AxisAlignedBB(sentryPos));

					if (!sentries.isEmpty()) {
						SentryEntity sentry = sentries.get(0);
						SentryMode mode = sentry.getMode();

						if(sentry.hasCustomName())
						{
							String line = Utils.getFormattedCoordinates(new BlockPos(coords[0], coords[1], coords[2])).getFormattedText();
							int nameWidth = font.getStringWidth(sentry.getCustomName().getFormattedText());
							int nameX = startX + xSize / 4 - nameWidth + 33 + (i / 6) * xSize / 2;
							int nameY = startY + (i % 6) * 30 + 7;
							StringHoverChecker posTooltipText = new StringHoverChecker(nameY + 4, nameY + 18, nameX, nameX + nameWidth + 2, line);

							names[i] = sentry.getCustomName();
							hoverCheckers.add(posTooltipText);
						}

						guiButtons[i][MODE].active = true;
						guiButtons[i][TARGETS].active = mode != SentryMode.IDLE;
						guiButtons[i][UNBIND].active = true;
						((TogglePictureButton)guiButtons[i][0]).setCurrentIndex(mode.ordinal() / 3);
						((TogglePictureButton)guiButtons[i][1]).setCurrentIndex(mode.ordinal() % 3);
						hoverCheckers.add(new StringHoverChecker(guiButtons[i][MODE], Arrays.asList(Utils.localize("gui.securitycraft:srat.mode2").getFormattedText(), Utils.localize("gui.securitycraft:srat.mode1").getFormattedText(), Utils.localize("gui.securitycraft:srat.mode3").getFormattedText())));
						hoverCheckers.add(new StringHoverChecker(guiButtons[i][TARGETS], Arrays.asList(Utils.localize("gui.securitycraft:srat.targets1").getFormattedText(), Utils.localize("gui.securitycraft:srat.targets2").getFormattedText(), Utils.localize("gui.securitycraft:srat.targets3").getFormattedText())));
						hoverCheckers.add(new StringHoverChecker(guiButtons[i][UNBIND], Utils.localize("gui.securitycraft:srat.unbind").getFormattedText()));
						foundSentry = true;
					}
					else {
						removeTagFromToolAndUpdate(srat, coords[0], coords[1], coords[2]);

						for (int j = 0; j < 3; j++) {
							guiButtons[i][j].active = false;
						}
					}
				}
				else {
					for (int j = 0; j < 2; j++) {
						hoverCheckers.add(new StringHoverChecker(guiButtons[i][j], Utils.localize("gui.securitycraft:srat.outOfRange").getFormattedText()));
					}

					hoverCheckers.add(new StringHoverChecker(guiButtons[i][UNBIND], Utils.localize("gui.securitycraft:srat.unbind").getFormattedText()));
				}
			}
		}

		//Add buttons for global operation (all sentries), large id
		guiButtonsGlobal[MODE] = new TogglePictureButton(1000, startX + 260, startY + 188, 20, 20, SENTRY_ICONS, modeTextureX, yStarts, 2, 3, this::actionPerformedGlobal);
		guiButtonsGlobal[TARGETS] = new TogglePictureButton(1001, startX + 22 + 260, startY + 188, 20, 20, SENTRY_ICONS, targetTextureX, yStarts, 2, 3, this::actionPerformedGlobal);
		guiButtonsGlobal[UNBIND] = new IdButton(1002, startX + 44 + 260, startY + 188, 20, 20, "X", this::clickGlobalUnbind);

		for (int j = 0; j < 3; j++) {
			guiButtonsGlobal[j].active = foundSentry;
			addButton(guiButtonsGlobal[j]);
		}

		hoverCheckers.add(new StringHoverChecker(guiButtonsGlobal[MODE], Arrays.asList(Utils.localize("gui.securitycraft:srat.mode2").getFormattedText(), Utils.localize("gui.securitycraft:srat.mode1").getFormattedText(), Utils.localize("gui.securitycraft:srat.mode3").getFormattedText())));
		hoverCheckers.add(new StringHoverChecker(guiButtonsGlobal[TARGETS], Arrays.asList(Utils.localize("gui.securitycraft:srat.targets1").getFormattedText(), Utils.localize("gui.securitycraft:srat.targets2").getFormattedText(), Utils.localize("gui.securitycraft:srat.targets3").getFormattedText())));
		hoverCheckers.add(new StringHoverChecker(guiButtonsGlobal[UNBIND], Utils.localize("gui.securitycraft:srat.unbind").getFormattedText()));
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		int startX = (width - xSize) / 2;
		int startY = (height - ySize) / 2;
		String modifyAll = Utils.localize("gui.securitycraft:srat.modifyAll").getFormattedText();

		renderBackground();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		minecraft.getTextureManager().bindTexture(TEXTURE);
		blit(startX, startY, 0, 0, xSize, ySize, 512, 256);
		super.render(mouseX, mouseY, partialTicks);
		font.drawString(Utils.localize(SCContent.REMOTE_ACCESS_SENTRY.get().getTranslationKey()).getFormattedText(), startX + 5, startY - 25 + 13, 0xFF0000);

		for (int i = 0; i < 12; i++) {
			int[] coords = getSentryCoordinates(i);
			String line;

			if (coords[0] == 0 && coords[1] == 0 && coords[2] == 0)
				line = Utils.localize("gui.securitycraft:srat.notBound").getFormattedText();
			else if(names[i] != null)
				line = names[i].getFormattedText();
			else
				line = Utils.getFormattedCoordinates(new BlockPos(coords[0], coords[1], coords[2])).getFormattedText();

			font.drawString(line, startX + xSize / 4 - font.getStringWidth(line) + 35 + (i / 6) * xSize / 2, startY + (i % 6) * 30 + 13, 4210752);
		}

		font.drawString(modifyAll, startX + xSize / 2 - font.getStringWidth(modifyAll) + 25, startY + 194, 4210752);

		for(StringHoverChecker chc : hoverCheckers)
		{
			if(chc != null && chc.checkHover(mouseX, mouseY))
				renderTooltip(chc.getName(), mouseX, mouseY);
		}
	}

	/**
	 * Change the sentry mode, and update GUI buttons state
	 */
	protected void performSingleAction(int sentry, int mode, int targets){
		int[] coords = getSentryCoordinates(sentry);
		List<SentryEntity> sentries = Minecraft.getInstance().player.world.getEntitiesWithinAABB(SentryEntity.class, new AxisAlignedBB(new BlockPos(coords[0], coords[1], coords[2])));

		if(!sentries.isEmpty()) {
			int resultingMode = Math.max(0, Math.min(targets + mode * 3, 6)); //bind between 0 and 6

			guiButtons[sentry][TARGETS].active = SentryMode.values()[resultingMode] != SentryMode.IDLE;
			sentries.get(0).toggleMode(Minecraft.getInstance().player, resultingMode, false);
			SecurityCraft.channel.sendToServer(new SetSentryMode(sentries.get(0).getPosition(), resultingMode));
		}
	}

	private void clickUnbind(IdButton button)
	{
		unbindSentry(button.id / 3);
	}

	private void clickGlobalUnbind(IdButton button)
	{
		for(int i = 0; i < 12; i++)
		{
			unbindSentry(i);
		}
	}

	private void unbindSentry(int sentry)
	{
		int[] coords = getSentryCoordinates(sentry);

		removeTagFromToolAndUpdate(srat, coords[0], coords[1], coords[2]);

		for(int i = 0; i < 3; i++) {
			guiButtons[sentry][i].active = false;
		}

		for(int i = 0; i < guiButtons.length; i++)
		{
			if(guiButtons[i][UNBIND].active)
				return;
		}

		for (int i = 0; i < 3; i++) {
			guiButtonsGlobal[i].active = false;
		}
	}

	/**
	 * Action to perform when a button is clicked
	 */
	protected void actionPerformed(IdButton button) {
		int sentry = button.id / 3;
		int type = button.id % 3;
		int mode = ((TogglePictureButton)button).getCurrentIndex();
		int targets = mode;

		if(type == 0)
			targets = ((TogglePictureButton)guiButtons[sentry][TARGETS]).getCurrentIndex();
		else if(type == 1)
			mode = ((TogglePictureButton)guiButtons[sentry][MODE]).getCurrentIndex();

		performSingleAction(sentry, mode, targets);
	}

	protected void actionPerformedGlobal(IdButton button) {
		for (int i = 0; i < buttons.size() / 3; i++) {
			Widget widget = buttons.get(i * 3);

			if(widget instanceof IdButton && getSentryCoordinates(i)[1] != 0)
			{
				int sentry = ((IdButton)buttons.get(i * 3)).id / 3;
				int mode = button.id == guiButtonsGlobal[MODE].id ? ((TogglePictureButton)guiButtonsGlobal[MODE]).getCurrentIndex() : ((TogglePictureButton)guiButtons[sentry][MODE]).getCurrentIndex();
				int targets = button.id == guiButtonsGlobal[TARGETS].id ? ((TogglePictureButton)guiButtonsGlobal[TARGETS]).getCurrentIndex() : ((TogglePictureButton)guiButtons[sentry][TARGETS]).getCurrentIndex();

				((TogglePictureButton)guiButtons[sentry][MODE]).setCurrentIndex(mode);
				((TogglePictureButton)guiButtons[sentry][TARGETS]).setCurrentIndex(targets);
				performSingleAction(sentry, mode, targets);
			}
		}
	}

	/**
	 * @param sentry 0 based
	 */
	private int[] getSentryCoordinates(int sentry) {
		sentry++; // sentries are stored starting by sentry1 up to sentry12

		if (srat.getItem() != null && srat.getItem() == SCContent.REMOTE_ACCESS_SENTRY.get() && srat.getTag() != null && srat.getTag().getIntArray("sentry" + sentry) != null && srat.getTag().getIntArray("sentry" + sentry).length > 0)
			return srat.getTag().getIntArray("sentry" + sentry);
		else
			return new int[] { 0, 0, 0 };
	}

	private void removeTagFromToolAndUpdate(ItemStack stack, int x, int y, int z) {
		if (stack.getTag() == null)
			return;

		for (int i = 1; i <= 12; i++) {
			if (stack.getTag().getIntArray("sentry" + i).length > 0) {
				int[] coords = stack.getTag().getIntArray("sentry" + i);

				if (coords[0] == x && coords[1] == y && coords[2] == z) {
					stack.getTag().putIntArray("sentry" + i, new int[] { 0, 0, 0 });
					SecurityCraft.channel.sendToServer(new UpdateNBTTagOnServer(stack));
					return;
				}
			}
		}
	}

	// Based on ChunkManager$EntityTrackerEntry#updateTrackingState
	private boolean isSentryVisibleToPlayer(BlockPos sentryPos){
		PlayerEntity player = Minecraft.getInstance().player;
		double d0 = player.posX - sentryPos.getX();
		double d1 = player.posZ - sentryPos.getZ();
		int i = Math.min(SENTRY_TRACKING_RANGE, viewDistance) - 1;
		return d0 >= (-i) && d0 <= i && d1 >= (-i) && d1 <= i;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (minecraft.gameSettings.keyBindInventory.isActiveAndMatches(InputMappings.getInputByCode(p_keyPressed_1_, p_keyPressed_2_))) {
			this.onClose();
			return true;
		}
		return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
	}
}