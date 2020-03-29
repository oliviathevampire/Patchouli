package vazkii.patchouli.client.book.gui.button;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Formatting;
import vazkii.patchouli.client.book.gui.GuiBook;

public class GuiButtonBookEdit extends GuiButtonBook {

	public GuiButtonBookEdit(GuiBook parent, int x, int y, ButtonWidget.PressAction onPress) {
		super(parent, x, y, 308, 9, 11, 11, onPress,
				I18n.translate("patchouli.gui.lexicon.button.editor"),
				Formatting.GRAY + I18n.translate("patchouli.gui.lexicon.button.editor.info"));
	}

}
