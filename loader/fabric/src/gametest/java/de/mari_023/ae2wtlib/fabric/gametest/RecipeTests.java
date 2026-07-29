package de.mari_023.ae2wtlib.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.wut.recipe.Combine;
import de.mari_023.ae2wtlib.wut.recipe.Upgrade;

/**
 * Recipe presence — playbook rule #8. Two custom serializers (`ae2wtlib:upgrade` adds a terminal to a WUT,
 * `ae2wtlib:combine` merges two terminals into one) plus the hand-written JSON recipes that use them.
 * <p>
 * This is deliberately a <em>datapack-loaded</em> check, not just a registry check: `RegistrationTests` already asserts
 * the serializers registered, whereas this asserts the server actually parsed recipes that reference them — which is
 * what would break if a recipe JSON's shape drifted from the serializer's codec.
 */
public class RecipeTests {
    @GameTest
    public void serializerInstancesAreTheRegisteredOnes(GameTestHelper helper) {
        RecipeSerializer<?> upgrade = BuiltInRegistries.RECIPE_SERIALIZER.getValue(AE2wtlibAPI.id("upgrade"));
        RecipeSerializer<?> combine = BuiltInRegistries.RECIPE_SERIALIZER.getValue(AE2wtlibAPI.id("combine"));
        helper.assertTrue(upgrade == Upgrade.serializer, "ae2wtlib:upgrade is not Upgrade.serializer");
        helper.assertTrue(combine == Combine.serializer, "ae2wtlib:combine is not Combine.serializer");
        helper.succeed();
    }

    @GameTest
    public void datapackContainsRecipesForBothSerializers(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager().getRecipes();

        long upgradeCount = recipes.stream().filter(h -> h.value().getSerializer() == Upgrade.serializer).count();
        long combineCount = recipes.stream().filter(h -> h.value().getSerializer() == Combine.serializer).count();

        helper.assertTrue(upgradeCount > 0, "no loaded recipe uses the ae2wtlib:upgrade serializer");
        helper.assertTrue(combineCount > 0, "no loaded recipe uses the ae2wtlib:combine serializer");
        helper.succeed();
    }

    /** The mod's own crafting recipes loaded (terminals + cards are all hand-written JSON upstream). */
    @GameTest
    public void modRecipesLoaded(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager().getRecipes();
        long owned = recipes.stream()
                .filter(h -> h.id().identifier().getNamespace().equals(AE2wtlibAPI.MOD_NAME))
                .count();
        helper.assertTrue(owned >= 5,
                "only " + owned + " ae2wtlib recipes loaded - expected at least 5 (3 terminals + 2 cards)");
        helper.succeed();
    }
}
