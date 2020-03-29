package vazkii.patchouli.client.book.page;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.collection.DefaultedList;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

public class PageCrafting extends PageDoubleRecipeRegistry<CraftingRecipe> {
	
	public PageCrafting() {
		super(RecipeType.CRAFTING);
	}
	
	@Override
	protected void drawRecipe(CraftingRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
		mc.getTextureManager().bindTexture(book.craftingTexture);
		RenderSystem.enableBlend();
		DrawableHelper.drawTexture(recipeX - 2, recipeY - 2, 0, 0, 100, 62, 128, 128);

		boolean shaped = recipe instanceof ShapedRecipe;
		if(!shaped) {
			int iconX = recipeX + 62;
			int iconY = recipeY + 2;
			DrawableHelper.drawTexture(iconX, iconY, 0, 64, 11, 11, 128, 128);
			if(parent.isMouseInRelativeRange(mouseX, mouseY, iconX, iconY, 11, 11))
				parent.setTooltip(I18n.translate("patchouli.gui.lexicon.shapeless"));
		}

		parent.drawCenteredStringNoShadow(getTitle(second), GuiBook.PAGE_WIDTH / 2, recipeY - 10, book.headerColor);
		
		parent.renderItemStack(recipeX + 79, recipeY + 22, mouseX, mouseY, recipe.getOutput());
		
		DefaultedList<Ingredient> ingredients = recipe.getPreviewInputs();
		int wrap = 3;
		if(shaped)
			wrap = ((ShapedRecipe) recipe).getWidth();
		
		for(int i = 0; i < ingredients.size(); i++)
			parent.renderIngredient(recipeX + (i % wrap) * 19 + 3, recipeY + (i / wrap) * 19 + 3, mouseX, mouseY, ingredients.get(i));
	}

	@Override
	protected int getRecipeHeight() {
		return 78;
	}

	@Override
	protected ItemStack getRecipeOutput(CraftingRecipe recipe) {
		if (recipe == null)
			return ItemStack.EMPTY;

		return recipe.getOutput();
	}

}
