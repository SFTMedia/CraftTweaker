package com.blamejared.crafttweaker.impl.command.type;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.bracket.custom.RecipeTypeBracketHandler;
import com.blamejared.crafttweaker.api.command.CommandUtilities;
import com.blamejared.crafttweaker.api.command.argument.RecipeTypeArgument;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.plugin.ICommandRegistrationHandler;
import com.blamejared.crafttweaker.api.recipe.handler.IRecipeHandlerRegistry;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import com.blamejared.crafttweaker.api.util.ItemStackUtil;
import com.blamejared.crafttweaker.mixin.common.access.recipe.AccessRecipeManager;
import com.blamejared.crafttweaker.platform.Services;
import com.mojang.brigadier.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RecipeCommands {
    
    private RecipeCommands() {}
    
    public static void registerCommands(final ICommandRegistrationHandler handler) {
        
        handler.registerRootCommand(
                "recipes",
                new TranslatableComponent("crafttweaker.command.description.recipes"),
                builder -> builder.executes(context -> {
                    final ServerPlayer player = context.getSource().getPlayerOrException();
                    return dumpRecipes(player);
                })
        );
        handler.registerSubCommand(
                "recipes",
                "hand",
                new TranslatableComponent("crafttweaker.command.description.recipes.hand"),
                builder -> builder.executes(context -> {
                    final ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    return RecipeCommands.dumpHand(player, player.getMainHandItem());
                })
        );
        handler.registerSubCommand(
                "recipes",
                "inventory",
                new TranslatableComponent("crafttweaker.command.description.recipes.inventory"),
                builder -> builder.executes(context -> {
                    final ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    List<ItemStack> stacks = new ArrayList<>();
                    
                    IntStream.range(0, player.getInventory().getContainerSize())
                            .mapToObj(player.getInventory()::getItem)
                            .filter(itemStack -> !itemStack.isEmpty())
                            .forEach(itemStack -> {
                                if(stacks.stream().noneMatch(itemStack::sameItemStackIgnoreDurability)) {
                                    stacks.add(itemStack);
                                }
                            });
                    return RecipeCommands.dump(player, stacks);
                })
        );
        handler.registerSubCommand(
                "recipes",
                "manager",
                new TranslatableComponent("crafttweaker.command.description.recipes.manager"),
                builder -> builder.then(Commands.argument("type", RecipeTypeArgument.get()).executes(context -> {
                    final ServerPlayer player = context.getSource().getPlayerOrException();
                    return dumpRecipes(player, context.getArgument("type", IRecipeManager.class));
                }))
        );
    }
    
    private static int dumpRecipes(final Player player) {
        
        CraftTweakerAPI.LOGGER.info("Dumping all recipes!");
        
        dumpRecipes(player, it -> true);
        
        CommandUtilities.send(CommandUtilities.openingLogFile(new TranslatableComponent("crafttweaker.command.list.check.log", CommandUtilities.makeNoticeable(new TranslatableComponent("crafttweaker.command.misc.recipes.list")), CommandUtilities.getFormattedLogFile()).withStyle(ChatFormatting.GREEN)), player);
        return Command.SINGLE_SUCCESS;
    }
    
    private static int dumpRecipes(final Player player, final IRecipeManager<?> manager) {
        
        CraftTweakerAPI.LOGGER.info("Dumping recipes for manager " + manager.getCommandString() + "!");
        
        final RecipeType<?> type = manager.getRecipeType();
        dumpRecipes(player, it -> Objects.equals(it, type));
        
        CommandUtilities.send(CommandUtilities.openingLogFile(new TranslatableComponent("crafttweaker.command.list.check.log", CommandUtilities.makeNoticeable(new TranslatableComponent("crafttweaker.command.misc.recipes.list")), CommandUtilities.getFormattedLogFile()).withStyle(ChatFormatting.GREEN)), player);
        return Command.SINGLE_SUCCESS;
    }
    
    private static int dumpHand(final Player player, final ItemStack stack) {
        
        return dump(player, List.of(stack));
    }
    
    private static int dump(final Player player, final List<ItemStack> stacks) {
        
        for(ItemStack stack : stacks) {
            
            if(stack.isEmpty()) {
                // Only done because *a lot* of mods return empty ItemStacks as outputs
                CommandUtilities.send(new TranslatableComponent("crafttweaker.command.recipes.hand.empty").withStyle(ChatFormatting.RED), player);
                return Command.SINGLE_SUCCESS;
            }
            
            final IItemStack workingStack = Services.PLATFORM.createMCItemStack(stack.copy()).setAmount(1);
            
            CraftTweakerAPI.LOGGER.info("Dumping all recipes that output {}!", ItemStackUtil.getCommandString(workingStack.getInternal()));
            
            ((AccessRecipeManager) player.level.getRecipeManager()).crafttweaker$getRecipes()
                    .forEach((recipeType, map) ->
                            dumpRecipe(recipeType, map.values(), it -> workingStack.matches(Services.PLATFORM.createMCItemStack(it.getResultItem())), true));
        }
        CommandUtilities.send(CommandUtilities.openingLogFile(new TranslatableComponent("crafttweaker.command.list.check.log", CommandUtilities.makeNoticeable(new TranslatableComponent("crafttweaker.command.misc.recipes.list")), CommandUtilities.getFormattedLogFile()).withStyle(ChatFormatting.GREEN)), player);
        return Command.SINGLE_SUCCESS;
    }
    
    private static void dumpRecipes(final Player player, final Predicate<RecipeType<?>> typeFilter) {
        
        ((AccessRecipeManager) player.level.getRecipeManager()).crafttweaker$getRecipes()
                .entrySet()
                .stream()
                .filter(it -> typeFilter.test(it.getKey()))
                .forEach(it -> dumpRecipe(it.getKey(), it.getValue().values(), recipe -> true, false));
    }
    
    private static void dumpRecipe(final RecipeType<?> type, final Collection<Recipe<?>> recipes, final Predicate<Recipe<?>> filter, final boolean hideEmpty) {
        
        final IRecipeManager<?> manager = RecipeTypeBracketHandler.getOrDefault(type);
        if(manager == null) {
            // Scripts for example don't have a recipe manager
            return;
        }
        
        final String dumpResult = recipes.stream()
                .filter(filter)
                .sorted(Comparator.comparing(RecipeCommands::serializer).thenComparing(Recipe::getId))
                .map(it -> dump(manager, it))
                .collect(Collectors.joining("\n  "));
        
        if(hideEmpty && dumpResult.isEmpty()) {
            return;
        }
        
        CraftTweakerAPI.LOGGER.info("Recipe type: '{}'\n  {}\n", manager.getCommandString(), dumpResult.isEmpty() ? "No recipe found" : dumpResult);
    }
    
    private static ResourceLocation serializer(final Recipe<?> recipe) {
        
        return Objects.requireNonNull(Services.REGISTRY.getRegistryKey(recipe.getSerializer()));
    }
    
    private static <T extends Recipe<?>> String dump(final IRecipeManager<?> manager, final T recipe) {
        
        return IRecipeHandlerRegistry.getHandlerFor(recipe).dumpToCommandString(manager, recipe);
    }
    
}
