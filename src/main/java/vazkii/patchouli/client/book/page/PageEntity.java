package vazkii.patchouli.client.book.page;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL13;
import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.common.base.Patchouli;
import vazkii.patchouli.common.util.EntityUtil;

import java.util.function.Function;

public class PageEntity extends PageWithText {

	@SerializedName("entity")
	public String entityId;

	float scale = 1F;
	@SerializedName("offset")
	float extraOffset = 0F;
	String name;

	boolean rotate = true;
	@SerializedName("default_rotation")
	float defaultRotation = -45f;

	transient boolean errored;
	transient Entity entity;
	transient Function<World, Entity> creator;
	transient float renderScale, offset;

	@Override
	public void build(BookEntry entry, int pageNum) {
		super.build(entry, pageNum);

		creator = EntityUtil.loadEntity(entityId);
	}

	@Override
	public void onDisplayed(GuiBookEntry parent, int left, int top) {
		super.onDisplayed(parent, left, top);

		loadEntity(parent.getMinecraft().world);
	}

	@Override
	public int getTextHeight() {
		return 115;
	}

	@Override
	public void render(int mouseX, int mouseY, float pticks) {
		int x = GuiBook.PAGE_WIDTH / 2 - 53;
		int y = 7;
		RenderSystem.enableBlend();
		RenderSystem.color3f(1F, 1F, 1F);
		GuiBook.drawFromTexture(book, x, y, 405, 149, 106, 106);

		parent.drawCenteredStringNoShadow(name, GuiBook.PAGE_WIDTH / 2, 0, book.headerColor);

		if(errored)
			fontRenderer.drawWithShadow(I18n.translate("patchouli.gui.lexicon.loading_error"), 58, 60, 0xFF0000);

		if(entity != null)
			renderEntity(parent.getMinecraft().world, rotate ? ClientTicker.total : defaultRotation);

		super.render(mouseX, mouseY, pticks);
	}

	private void renderEntity(World world, float rotation) {
		renderEntity(entity, world, 58, 60, rotation, renderScale, offset);
	}	

	public static void renderEntity(Entity entity, World world, float x, float y, float rotation, float renderScale, float offset) {
		entity.world = world;

		RenderSystem.pushMatrix();
		RenderSystem.color3f(1F, 1F, 1F);
		MatrixStack matrix = new MatrixStack();
		matrix.translate(x, y, 50);
		matrix.scale(renderScale, renderScale, renderScale);
		matrix.translate(0, offset, 0);
		matrix.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(180));
		matrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(rotation));
		EntityRenderDispatcher erd = MinecraftClient.getInstance().getEntityRenderManager();
		VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
		erd.setRenderShadows(false);
		erd.render(entity, 0, 0, 0, 0, 1, matrix, immediate, 0xF000F0);
		erd.setRenderShadows(true);
		immediate.draw();
		RenderSystem.popMatrix();
	}

	private void loadEntity(World world) {
		if(!errored && (entity == null || !entity.isAlive())) {
			try {
				entity = creator.apply(world);

				float width = entity.getWidth();
				float height = entity.getHeight();
				
				float entitySize = width;
				if(width < height)
					entitySize = height;
				entitySize = Math.max(1F, entitySize);

				renderScale = 100F / entitySize * 0.8F * scale;
				offset = Math.max(height, entitySize) * 0.5F + extraOffset;

				if(name == null || name.isEmpty())
					name = entity.getName().asFormattedString();
			} catch(Exception e) {
				errored = true;
				Patchouli.LOGGER.error("Failed to load entity", e);
			}
		}
	}



}
