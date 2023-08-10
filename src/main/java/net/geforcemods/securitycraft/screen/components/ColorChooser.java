package net.geforcemods.securitycraft.screen.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

public class ColorChooser extends Screen implements INestedGuiEventHandler {
	private static final ResourceLocation TEXTURE = new ResourceLocation(SecurityCraft.MODID, "textures/gui/container/color_chooser.png");
	private static final int COLOR_FIELD_SIZE = 75;
	protected boolean disabled = true;
	private final int xStart, yStart;
	private final List<Rectangle2d> extraAreas = new ArrayList<>();
	private final ITextComponent rText = new StringTextComponent("R");
	private final ITextComponent gText = new StringTextComponent("G");
	private final ITextComponent bText = new StringTextComponent("B");
	private final ITextComponent rgbHexText = new StringTextComponent("#");
	private boolean clickedInDragRegion = false;
	private float h, s, b;
	private int colorFieldTop, colorFieldBottom, colorFieldLeft, colorFieldRight;
	private final HoverChecker colorFieldHoverChecker;
	private float selectionX, selectionY;
	private final int rgbColor;
	private FixedTextFieldWidget rBox, gBox, bBox, rgbHexBox;
	private HueSlider hueSlider;

	public ColorChooser(ITextComponent title, int xStart, int yStart, int rgbColor) {
		super(title);
		this.xStart = xStart;
		this.yStart = yStart;
		colorFieldLeft = xStart + 6;
		colorFieldTop = yStart + 6;
		colorFieldRight = colorFieldLeft + COLOR_FIELD_SIZE;
		colorFieldBottom = colorFieldTop + COLOR_FIELD_SIZE;
		this.rgbColor = rgbColor;
		colorFieldHoverChecker = new HoverChecker(colorFieldTop, colorFieldBottom, colorFieldLeft, colorFieldRight);
	}

	@Override
	protected void init() {
		Predicate<String> boxFilter = string -> string.isEmpty() || StringUtils.isNumeric(string);
		Function<TextFieldWidget, Consumer<String>> boxResponder = box -> string -> {
			if (!string.isEmpty()) {
				int number = Integer.parseInt(string);
				ToIntFunction<TextFieldWidget> parsingFunction = editBox -> {
					if (editBox.getValue().isEmpty())
						return 0;
					else
						return Integer.parseInt(editBox.getValue());
				};

				if (number < 0)
					box.setValue("0");
				else if (number > 255)
					box.setValue("255");

				updateHSBValues(parsingFunction.applyAsInt(rBox), parsingFunction.applyAsInt(gBox), parsingFunction.applyAsInt(bBox));
				updateTextFields(box);
				onColorChange();
			}
		};
		int red = rgbColor >> 16 & 255;
		int green = rgbColor >> 8 & 255;
		int blue = rgbColor & 255;

		updateHSBValues(red, green, blue);
		extraAreas.add(new Rectangle2d(xStart, yStart, 144, 108));
		hueSlider = addButton(new HueSlider(colorFieldLeft - 2, yStart + 85, 82, 20, h * 360.0D, slider -> {
			h = slider.getValueInt() / 360.0F;
			updateTextFields(null);
			onColorChange();
		}));
		rBox = addButton(new FixedTextFieldWidget(font, colorFieldRight + 13, colorFieldTop, 26, 10, rText));
		gBox = addButton(new FixedTextFieldWidget(font, colorFieldRight + 13, colorFieldTop + 15, 26, 10, gText));
		bBox = addButton(new FixedTextFieldWidget(font, colorFieldRight + 13, colorFieldTop + 30, 26, 10, bText));
		rgbHexBox = addButton(new FixedTextFieldWidget(font, colorFieldRight + 13, colorFieldTop + 45, 46, 10, rgbHexText));
		rBox.setValue("" + red);
		gBox.setValue("" + green);
		bBox.setValue("" + blue);
		getRgbHexBox().setValue(Integer.toHexString(rgbColor).substring(2));
		rBox.setMaxLength(3);
		gBox.setMaxLength(3);
		bBox.setMaxLength(3);
		getRgbHexBox().setMaxLength(6);
		rBox.setFilter(boxFilter);
		gBox.setFilter(boxFilter);
		bBox.setFilter(boxFilter);
		getRgbHexBox().setFilter(string -> string.matches("[0-9a-fA-F]*"));
		rBox.setResponder(boxResponder.apply(rBox));
		gBox.setResponder(boxResponder.apply(gBox));
		bBox.setResponder(boxResponder.apply(bBox));
		getRgbHexBox().setResponder(string -> {
			if (!string.isEmpty()) {
				int hexColor = Integer.parseInt(string, 16);

				updateHSBValues(hexColor >> 16 & 255, hexColor >> 8 & 255, hexColor & 255);
				updateTextFields(getRgbHexBox());
				onColorChange();
			}
		});
	}

	@Override
	public void render(MatrixStack pose, int mouseX, int mouseY, float partialTick) {
		if (!disabled) {
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			minecraft.getTextureManager().bind(TEXTURE);
			blit(pose, xStart, yStart, 0, 0, 145, 109);
			ClientUtils.fillHorizontalGradient(pose, 0, colorFieldLeft, colorFieldTop, colorFieldRight + 1, colorFieldBottom + 1, 0xFFFFFFFF, ClientUtils.HSBtoRGB(h, 1.0F, 1.0F));
			fillGradient(pose, colorFieldLeft, colorFieldTop, colorFieldRight + 1, colorFieldBottom + 1, 0x00000000, 0xFF000000);
			blit(pose, (int) selectionX - 1, (int) selectionY - 1, colorFieldHoverChecker.checkHover(mouseX, mouseY) ? 148 : 145, 20, 3, 3); //color field indicator
			super.render(pose, mouseX, mouseY, partialTick);
			font.draw(pose, rText, colorFieldRight + 5, colorFieldTop + 1, 0x404040);
			font.draw(pose, gText, colorFieldRight + 5, colorFieldTop + 16, 0x404040);
			font.draw(pose, bText, colorFieldRight + 5, colorFieldTop + 31, 0x404040);
			font.draw(pose, rgbHexText, colorFieldRight + 5, colorFieldTop + 46, 0x404040);
		}
	}

	@Override
	public void tick() {
		if (!disabled) {
			//this is validated here and not in the edit box' responder in order to allow for an empty box while typing
			validateNotEmpty(rBox);
			validateNotEmpty(gBox);
			validateNotEmpty(bBox);

			if (getRgbHexBox() != null && !getRgbHexBox().isFocused() && getRgbHexBox().getValue().isEmpty())
				getRgbHexBox().setValue("000000");
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!disabled) {
			super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

			if (button == 0 && clickedInDragRegion) {
				setSelection(mouseX, mouseY);
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!disabled) {
			super.mouseClicked(mouseX, mouseY, button);
			clickedInDragRegion = colorFieldHoverChecker.checkHover(mouseX, mouseY);

			if (clickedInDragRegion)
				setSelection(mouseX, mouseY);
		}

		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (!disabled) {
			super.mouseReleased(mouseX, mouseY, button);
			clickedInDragRegion = false;
		}

		return false;
	}

	public List<Rectangle2d> getGuiExtraAreas() {
		return disabled ? new ArrayList<>() : extraAreas;
	}

	public int getRGBColor() {
		return ClientUtils.HSBtoRGB(h, s, b);
	}

	private void setSelection(double mouseX, double mouseY) {
		selectionX = (int) MathHelper.clamp(mouseX, colorFieldLeft, colorFieldRight);
		selectionY = (int) MathHelper.clamp(mouseY, colorFieldTop, colorFieldBottom);
		s = ((selectionX - colorFieldLeft) / COLOR_FIELD_SIZE);
		b = 1.0F - ((selectionY - colorFieldTop) / COLOR_FIELD_SIZE);
		updateTextFields(null);
		onColorChange();
	}

	private void updateHSBValues(int red, int green, int blue) {
		float[] hsbColor = ClientUtils.RGBtoHSB(red, green, blue);

		h = hsbColor[0];
		s = hsbColor[1];
		b = hsbColor[2];
		updateSelection();
	}

	private void updateTextFields(TextFieldWidget excluded) {
		int currentRGBColor = getRGBColor();
		int red = currentRGBColor >> 16 & 255;
		int green = currentRGBColor >> 8 & 255;
		int blue = currentRGBColor & 255;

		//setting the value directly to prevent a stack overflow due to setValue calling the responder, which in turn calls this
		trySetText(excluded, rBox, "" + red);
		trySetText(excluded, gBox, "" + green);
		trySetText(excluded, bBox, "" + blue);
		trySetText(excluded, getRgbHexBox(), Integer.toHexString(currentRGBColor).substring(2));
	}

	private void trySetText(TextFieldWidget excluded, FixedTextFieldWidget editBox, String value) {
		if (excluded != editBox) {
			editBox.value = value;
			editBox.updateCursor();
		}
	}

	private void updateSelection() {
		selectionX = s * COLOR_FIELD_SIZE + colorFieldLeft;
		selectionY = -b * COLOR_FIELD_SIZE + COLOR_FIELD_SIZE + colorFieldTop;

		if (getHueSlider() != null)
			getHueSlider().setValue(h * 360.0D);
	}

	public void onColorChange() {}

	private void validateNotEmpty(TextFieldWidget box) {
		if (box != null && !box.isFocused() && box.getValue().isEmpty())
			box.setValue("0");
	}

	public FixedTextFieldWidget getRgbHexBox() {
		return rgbHexBox;
	}

	public HueSlider getHueSlider() {
		return hueSlider;
	}

	public class HueSlider extends Slider {
		public HueSlider(int x, int y, int width, int height, double currentValue, ISlider onChangeSliderValue) {
			super(x, y, width, height, StringTextComponent.EMPTY, StringTextComponent.EMPTY, 0.0D, 360.0D, currentValue, false, false, b -> {}, onChangeSliderValue);
		}

		@Override
		protected void renderBg(MatrixStack pose, Minecraft minecraft, int mouseX, int mouseY) {}

		@Override
		public void renderButton(MatrixStack pose, int mouseX, int mouseY, float partialTick) {
			if (visible && dragging) {
				sliderValue = (mouseX - (x + 4)) / (float) (width - 8);
				updateSlider();
			}

			minecraft.getTextureManager().bind(TEXTURE);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			blit(pose, x + (int) (sliderValue * (width - 8)), y, isHovered() ? 151 : 145, 0, 6, height);
		}
	}

	//fixes focus when selecting boxes from last added to first added, as well as the highlight position sometimes being wrong when setting the value
	public class FixedTextFieldWidget extends TextFieldWidget {
		public FixedTextFieldWidget(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
			super(font, x, y, width, height, message);
		}

		@Override
		public void setFocus(boolean focused) {
			if (focused) {
				rBox.setFocused(false);
				gBox.setFocused(false);
				bBox.setFocused(false);
				getRgbHexBox().setFocused(false);
			}

			super.setFocused(focused);
		}

		public void updateCursor() {
			setCursorPosition(value.length()); //calling moveCursorToEnd also calls onValueChange, causing this method to be called again
			setHighlightPos(getCursorPosition());
		}
	}
}