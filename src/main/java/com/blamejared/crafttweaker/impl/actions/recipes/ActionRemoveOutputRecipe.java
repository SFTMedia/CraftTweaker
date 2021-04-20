package com.blamejared.crafttweaker.impl.actions.recipes;

import com.blamejared.crafttweaker.api.item.IIngredient;
import com.blamejared.crafttweaker.api.managers.IRecipeManager;
import com.blamejared.crafttweaker.impl.item.MCItemStackMutable;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.stream.Collectors;

// See AcitonRemoveRecipeByOutput
@Deprecated
public class ActionRemoveOutputRecipe extends ActionRecipeBase {
    
    private final IIngredient output;
    
    public ActionRemoveOutputRecipe(IRecipeManager manager, IIngredient output) {
        super(manager);
        this.output = output;
    }
    
    @Override
    public void apply() {
        List<ResourceLocation> list = getManager().getRecipes().values().stream().filter(iRecipe -> output.matches(new MCItemStackMutable(iRecipe.getRecipeOutput()))).map(IRecipe::getId).collect(Collectors.toList());
        list.forEach(getManager().getRecipes()::remove);
    }
    
    @Override
    public String describe() {
        return "Removing all \"" + getRecipeTypeName() + "\" recipes, that output: " + output;
    }
}
