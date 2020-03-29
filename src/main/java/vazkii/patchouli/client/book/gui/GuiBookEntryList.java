package vazkii.patchouli.client.book.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.button.GuiButtonCategory;
import vazkii.patchouli.client.book.gui.button.GuiButtonEntry;
import vazkii.patchouli.common.book.Book;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class GuiBookEntryList extends GuiBook {

	public static final int ENTRIES_PER_PAGE = 13;
	public static final int ENTRIES_IN_FIRST_PAGE = 11;
	
	BookTextRenderer text;
	
	List<ButtonWidget> dependentButtons;
	List<BookEntry> allEntries;
	List<BookEntry> visibleEntries;
	
	TextFieldWidget searchField;

	public GuiBookEntryList(Book book, Text title) {
		super(book, title);
	}
	
	@Override
	public void init() {
		super.init();
		
		text = new BookTextRenderer(this, getDescriptionText(), LEFT_PAGE_X, TOP_PADDING + 22);
		
		visibleEntries = new ArrayList<>();
		allEntries = new ArrayList<>(getEntries());
		allEntries.removeIf(BookEntry::shouldHide);
		if(shouldSortEntryList())
			Collections.sort(allEntries);
		
		searchField = new TextFieldWidget(textRenderer, 160, 170, 90, 12, "");
		searchField.setMaxLength(32);
		searchField.setHasBorder(false);
		searchField.setFocusUnlocked(false);
		searchField.changeFocus(true);
		
		dependentButtons = new ArrayList<>();
		buildEntryButtons();
	}
	
	protected abstract String getDescriptionText();
	protected abstract Collection<BookEntry> getEntries();
	
	protected boolean doesEntryCountForProgress(BookEntry entry) {
		return true;
	}
	
	protected boolean shouldDrawProgressBar() {
		return true;
	}
	
	protected boolean shouldSortEntryList() {
		return true;
	}
	
	protected void addSubcategoryButtons() {
		// NO-OP
	}
	
	@Override
	void drawForegroundElements(int mouseX, int mouseY, float partialTicks) {
		super.drawForegroundElements(mouseX, mouseY, partialTicks);
		
		if(page == 0) {
			drawCenteredStringNoShadow(getTitle().asFormattedString(), LEFT_PAGE_X + PAGE_WIDTH / 2, TOP_PADDING, book.headerColor);
			drawCenteredStringNoShadow(I18n.translate("patchouli.gui.lexicon.chapters"), RIGHT_PAGE_X + PAGE_WIDTH / 2, TOP_PADDING, book.headerColor);

			drawSeparator(book, LEFT_PAGE_X, TOP_PADDING + 12);
			drawSeparator(book, RIGHT_PAGE_X, TOP_PADDING + 12);

			text.render(mouseX, mouseY);
			if(shouldDrawProgressBar())
				drawProgressBar(book, mouseX, mouseY, this::doesEntryCountForProgress);
		} else if(page % 2 == 1 && page == maxpages - 1 && dependentButtons.size() <= ENTRIES_PER_PAGE)
			drawPageFiller(book);
		
		if(!searchField.getText().isEmpty()) {
			RenderSystem.color4f(1F, 1F, 1F, 1F);
			drawFromTexture(book, searchField.x - 8, searchField.y, 140, 183, 99, 14);
			book.getFont().draw(searchField.getText(), searchField.x + 7, searchField.y + 1, 0);
		}
		
		if(visibleEntries.isEmpty()) {
			drawCenteredStringNoShadow(I18n.translate("patchouli.gui.lexicon.no_results"), GuiBook.RIGHT_PAGE_X + GuiBook.PAGE_WIDTH / 2, 80, 0x333333);
			RenderSystem.scalef(2F, 2F, 2F);
			drawCenteredStringNoShadow(I18n.translate("patchouli.gui.lexicon.sad"), GuiBook.RIGHT_PAGE_X / 2 + GuiBook.PAGE_WIDTH / 4, 47, 0x999999);
			RenderSystem.scalef(0.5F, 0.5F, 0.5F);
		}
	}
	
	@Override
	public boolean mouseClickedScaled(double mouseX, double mouseY, int mouseButton) {
		return text.click(mouseX, mouseY, mouseButton)
			|| searchField.mouseClicked(mouseX - bookLeft, mouseY - bookTop, mouseButton)
			|| super.mouseClickedScaled(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public boolean charTyped(char c, int i) {
		String currQuery = searchField.getText();
		if(searchField.charTyped(c, i)) {
			if(!searchField.getText().equals(currQuery))
				buildEntryButtons();
			
			return true;
		}
		
		return super.charTyped(c, i);
 	}
	
	@Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
		String currQuery = searchField.getText();
		
		if(key == GLFW.GLFW_KEY_ENTER) {
			if(visibleEntries.size() == 1) {
				displayLexiconGui(new GuiBookEntry(book, visibleEntries.get(0)), true);
				return true;
			}
		} else if(searchField.keyPressed(key, scanCode, modifiers)) {
			if(!searchField.getText().equals(currQuery))
				buildEntryButtons();
			
			return true;
		}
		
		return super.keyPressed(key, scanCode, modifiers);
	}
	
	public void handleButtonCategory(ButtonWidget button) {
		displayLexiconGui(new GuiBookCategory(book, ((GuiButtonCategory) button).getCategory()), true);
	}
	
	public void handleButtonEntry(ButtonWidget button) {
		GuiBookEntry.displayOrBookmark(this, ((GuiButtonEntry) button).getEntry());
	}
	
	@Override
	void onPageChanged() {
		buildEntryButtons();
	}
	
	void buildEntryButtons() {
		removeButtonsIn(dependentButtons);
		dependentButtons.clear();
		visibleEntries.clear();
		
		String query = searchField.getText().toLowerCase();
		allEntries.stream().filter((e) -> e.isFoundByQuery(query)).forEach(visibleEntries::add);

		maxpages = 1;
		int count = visibleEntries.size();
		count -= ENTRIES_IN_FIRST_PAGE;
		if(count > 0)
			maxpages += (int) Math.ceil((float) count / (ENTRIES_PER_PAGE * 2));
		
		while(getEntryCountStart() > visibleEntries.size())
			page--;
		
		if(page == 0) {
			addEntryButtons(RIGHT_PAGE_X, TOP_PADDING + 20, 0, ENTRIES_IN_FIRST_PAGE);
			addSubcategoryButtons();
		} else {
			int start = getEntryCountStart();
			addEntryButtons(LEFT_PAGE_X, TOP_PADDING, start, ENTRIES_PER_PAGE);
			addEntryButtons(RIGHT_PAGE_X, TOP_PADDING, start + ENTRIES_PER_PAGE, ENTRIES_PER_PAGE);
		}
	}
	
	int getEntryCountStart() {
		if(page == 0)
			return 0;
		
		int start = ENTRIES_IN_FIRST_PAGE;
		start += (ENTRIES_PER_PAGE * 2) * (page - 1);
		return start;
	}
	
	void addEntryButtons(int x, int y, int start, int count) {
		for(int i = 0; i < count && (i + start) < visibleEntries.size(); i++) {
			ButtonWidget button = new GuiButtonEntry(this, bookLeft + x, bookTop + y + i * 11, visibleEntries.get(start + i), this::handleButtonEntry);
			addButton(button);
			dependentButtons.add(button);
		}
	}

}
