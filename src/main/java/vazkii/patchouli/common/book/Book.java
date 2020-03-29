package vazkii.patchouli.common.book;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import vazkii.patchouli.api.BookContentsReloadCallback;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.ExternalBookContents;
import vazkii.patchouli.client.handler.UnicodeFontHandler;
import vazkii.patchouli.common.base.Patchouli;
import vazkii.patchouli.common.handler.AdvancementSyncHandler;
import vazkii.patchouli.common.item.ItemModBook;
import vazkii.patchouli.common.util.ItemStackUtil;

import java.util.*;

public class Book {
	
	public static final Identifier DEFAULT_MODEL = new Identifier(Patchouli.MOD_ID, "book_brown");

	private static final Map<String, String> DEFAULT_MACROS = Util.make(() -> {
		Map<String, String> ret = new HashMap<>();
		ret.put("$(list", "$(li"); //  The lack of ) is intended
		ret.put("/$", "$()");
		ret.put("<br>", "$(br)");

		ret.put("$(item)", "$(#b0b)");
		ret.put("$(thing)", "$(#490)");
		return ret;
	});

	public transient BookContents contents;

	private transient boolean wasUpdated = false;
	
	public transient ModContainer owner;
	public transient Identifier id;
	private transient ItemStack bookItem;
	
	public transient int textColor, headerColor, nameplateColor, linkColor, linkHoverColor, progressBarColor, progressBarBackground;
	
	public transient boolean isExtension = false;
	public transient List<Book> extensions = new LinkedList<>();
	public transient Book extensionTarget;
	
	public transient boolean isExternal;
	
	// JSON Loaded properties
	
	public String name = "";
	@SerializedName("landing_text")
	public String landingText = "patchouli.gui.lexicon.landing_info";

	@SerializedName("advancement_namespaces")
	public List<String> advancementNamespaces = new ArrayList<>();

	@SerializedName("book_texture")
	public Identifier bookTexture = new Identifier(Patchouli.MOD_ID, "textures/gui/book_brown.png");

	@SerializedName("filler_texture")
	public Identifier fillerTexture = new Identifier(Patchouli.MOD_ID, "textures/gui/page_filler.png");

	@SerializedName("crafting_texture")
	public Identifier craftingTexture = new Identifier(Patchouli.MOD_ID, "textures/gui/crafting.png");

	public Identifier model = DEFAULT_MODEL;

	@SerializedName("text_color")
	public String textColorRaw = "000000";
	@SerializedName("header_color")
	public String headerColorRaw = "333333";
	@SerializedName("nameplate_color")
	public String nameplateColorRaw = "FFDD00";
	@SerializedName("link_color")
	public String linkColorRaw = "0000EE";
	@SerializedName("link_hover_color")
	public String linkHoverColorRaw = "8800EE";
	
	@SerializedName("use_blocky_font")
	public boolean useBlockyFont = false;
	
	@SerializedName("progress_bar_color")
	public String progressBarColorRaw = "FFFF55";
	@SerializedName("progress_bar_background")
	public String progressBarBackgroundRaw = "DDDDDD";
	
	@SerializedName("open_sound")
	public Identifier openSound = new Identifier(Patchouli.MOD_ID, "book_open");

	@SerializedName("flip_sound")
	public Identifier flipSound = new Identifier(Patchouli.MOD_ID, "book_flip");
	
	@SerializedName("show_progress")
	public boolean showProgress = true;
	
	@SerializedName("index_icon")
	public String indexIconRaw = "";

	public String version = "0";
	public String subtitle = "";

	@SerializedName("creative_tab")
	public String creativeTab = "misc";

	@SerializedName("advancements_tab")
	public Identifier advancementsTab;
	
	@SerializedName("dont_generate_book")
	public boolean noBook = false;

	@SerializedName("custom_book_item")
	public String customBookItem = "";
	
	@SerializedName("show_toasts")
	public boolean showToasts = true;
	
	@SerializedName("extend")
	public Identifier extend;

	@SerializedName("allow_extensions")
	public boolean allowExtensions = true;
	
	public boolean i18n = false;
	
	public Map<String, String> macros = new HashMap<>();
	
	public void build(ModContainer owner, Identifier resource, boolean external) {
		this.owner = owner;
		this.id = resource;
		this.isExternal = external;
		
		isExtension = extend != null;
		
		// minecraft has an advancement for every recipe, so we don't allow
		// tracking it to keep packets at a reasonable size
		advancementNamespaces.remove("minecraft"); 
		AdvancementSyncHandler.trackedNamespaces.addAll(advancementNamespaces);
		
		if(!isExtension) {
			textColor = 0xFF000000 | Integer.parseInt(textColorRaw, 16);
			headerColor = 0xFF000000 | Integer.parseInt(headerColorRaw, 16);
			nameplateColor = 0xFF000000 | Integer.parseInt(nameplateColorRaw, 16);
			linkColor = 0xFF000000 | Integer.parseInt(linkColorRaw, 16);
			linkHoverColor = 0xFF000000 | Integer.parseInt(linkHoverColorRaw, 16);
			progressBarColor = 0xFF000000 | Integer.parseInt(progressBarColorRaw, 16);
			progressBarBackground = 0xFF000000 | Integer.parseInt(progressBarBackgroundRaw, 16);

			for(String m : DEFAULT_MACROS.keySet())
				if(!macros.containsKey(m))
					macros.put(m, DEFAULT_MACROS.get(m));
		}
	}
	
	public boolean usesAdvancements() {
		return !advancementNamespaces.isEmpty();
	}
	
	public String getModNamespace() {
		return id.getNamespace();
	}
	
	public ItemStack getBookItem() {
		if(bookItem == null) {
			if(noBook)
				bookItem = ItemStackUtil.loadStackFromString(customBookItem);
			else bookItem = ItemModBook.forBook(this);
		}
		
		return bookItem;
	}
	
	@Environment(EnvType.CLIENT)
	public void markUpdated() {
		wasUpdated = true;
	}
	
	public boolean popUpdated() {
		boolean updated = wasUpdated;
		wasUpdated = false;
		return updated;
	}
	
	@Environment(EnvType.CLIENT)
	public void reloadContentsAndExtensions() {
		reloadContents();

		for(Book b : extensions)
			b.reloadExtensionContents();
	}
	
	@Environment(EnvType.CLIENT)
	public void reloadContents() {
		if(contents == null)
			contents = isExternal ? new ExternalBookContents(this) : new BookContents(this);
	
		if(!isExtension) {
			contents.reload(false);
			BookContentsReloadCallback.EVENT.invoker().trigger(this.id);
		}
	}
	
	@Environment(EnvType.CLIENT)
	public void reloadExtensionContents() {
		if(isExtension) {
			if(extensionTarget == null) {
				extensionTarget = BookRegistry.INSTANCE.books.get(extend);

				if(extensionTarget == null)
					throw new IllegalArgumentException("Extension Book " + id + " has no valid target");
				else if(!extensionTarget.allowExtensions)
					throw new IllegalArgumentException("Book " + extensionTarget.id + " doesn't allow extensions, so " + id + " can't resolve");
				
				extensionTarget.extensions.add(this);
				
				contents.categories = extensionTarget.contents.categories;
				contents.entries = extensionTarget.contents.entries;
				contents.templates = extensionTarget.contents.templates;
				contents.recipeMappings = extensionTarget.contents.recipeMappings;
			}
			
			contents.reload(true);
			BookContentsReloadCallback.EVENT.invoker().trigger(this.id);
		}
	}
	
	@Environment(EnvType.CLIENT)
	public void reloadLocks(boolean reset) {
		contents.entries.values().forEach(BookEntry::updateLockStatus);
		contents.categories.values().forEach((c) -> c.updateLockStatus(true));
		
		if(reset)
			popUpdated();
	}
	
	public String getOwnerName() {
		return owner.getMetadata().getName();
	}
	
	@Environment(EnvType.CLIENT)
	public TextRenderer getFont() {
		return useBlockyFont ? MinecraftClient.getInstance().textRenderer : UnicodeFontHandler.getUnicodeFont();
	}
	
}
