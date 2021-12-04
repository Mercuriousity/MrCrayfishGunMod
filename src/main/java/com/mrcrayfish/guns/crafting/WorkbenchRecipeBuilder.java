package com.mrcrayfish.guns.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.init.ModRecipeSerializers;
import com.mrcrayfish.guns.item.GunItem;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.IRequirementsStrategy;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Ocelot
 */
public class WorkbenchRecipeBuilder
{
    private final Item result;
    private final int count;
    private final List<WorkbenchIngredient> ingredients;
    private final Advancement.Builder advancementBuilder;
    private final List<ICondition> conditions = new ArrayList<>();

    private WorkbenchRecipeBuilder(IItemProvider item, int count)
    {
        this.result = item.asItem();
        this.count = count;
        this.ingredients = new ArrayList<>();
        this.advancementBuilder = Advancement.Builder.builder();
    }

    public static WorkbenchRecipeBuilder crafting(IItemProvider item)
    {
        return new WorkbenchRecipeBuilder(item, 1);
    }

    public static WorkbenchRecipeBuilder crafting(IItemProvider item, int count)
    {
        return new WorkbenchRecipeBuilder(item, count);
    }

    public WorkbenchRecipeBuilder addIngredient(IItemProvider item, int count)
    {
        this.ingredients.add(WorkbenchIngredient.of(item, count));
        return this;
    }

    public WorkbenchRecipeBuilder addIngredient(WorkbenchIngredient ingredient)
    {
        this.ingredients.add(ingredient);
        return this;
    }

    public WorkbenchRecipeBuilder addCriterion(String name, ICriterionInstance criterionIn)
    {
        this.advancementBuilder.withCriterion(name, criterionIn);
        return this;
    }

    public WorkbenchRecipeBuilder addCondition(ICondition condition)
    {
        this.conditions.add(condition);
        return this;
    }

    public void build(Consumer<IFinishedRecipe> consumer)
    {
        ResourceLocation resourcelocation = Registry.ITEM.getKey(this.result);
        this.build(consumer, resourcelocation);
    }

    public void build(Consumer<IFinishedRecipe> consumer, ResourceLocation id)
    {
        this.validate(id);
        this.advancementBuilder.withParentId(new ResourceLocation("recipes/root")).withCriterion("has_the_recipe", RecipeUnlockedTrigger.create(id)).withRewards(AdvancementRewards.Builder.recipe(id)).withRequirementsStrategy(IRequirementsStrategy.OR);
        consumer.accept(new WorkbenchRecipeBuilder.Result(id, this.result, this.count, this.ingredients, this.conditions, this.advancementBuilder, new ResourceLocation(id.getNamespace(), "recipes/" + this.result.getGroup().getPath() + "/" + id.getPath())));
    }

    /**
     * Makes sure that this recipe is valid and obtainable.
     */
    private void validate(ResourceLocation id)
    {
        if(this.advancementBuilder.getCriteria().isEmpty())
        {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    public static class Result implements IFinishedRecipe
    {
        private final ResourceLocation id;
        private final Item item;
        private final int count;
        private final List<WorkbenchIngredient> ingredients;
        private final List<ICondition> conditions;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;

        public Result(ResourceLocation id, IItemProvider item, int count, List<WorkbenchIngredient> ingredients, List<ICondition> conditions, Advancement.Builder advancement, ResourceLocation advancementId)
        {
            this.id = id;
            this.item = item.asItem();
            this.count = count;
            this.ingredients = ingredients;
            this.conditions = conditions;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        public void serialize(JsonObject json)
        {
            JsonArray conditions = new JsonArray();
            this.conditions.forEach(condition -> conditions.add(CraftingHelper.serialize(condition)));
            if(conditions.size() > 0)
            {
                json.add("conditions", conditions);
            }

            JsonArray materials = new JsonArray();
            this.ingredients.forEach(ingredient -> materials.add(ingredient.serialize()));
            json.add("materials", materials);

            JsonObject resultObject = new JsonObject();
            resultObject.addProperty("item", Registry.ITEM.getKey(this.item).toString());
            if(this.count > 1)
            {
                resultObject.addProperty("count", this.count);
            }
            json.add("result", resultObject);
        }

        @Override
        public ResourceLocation getID()
        {
            return this.id;
        }

        @Override
        public IRecipeSerializer<?> getSerializer()
        {
            return ModRecipeSerializers.WORKBENCH.get();
        }

        @Override
        public JsonObject getAdvancementJson()
        {
            return this.advancement.serialize();
        }

        @Override
        public ResourceLocation getAdvancementID()
        {
            return this.advancementId;
        }
    }
}