package pl.touk.krush.realreferences

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class CookingTest : BaseDatabaseTest() {

    @Test
    @Disabled("Enable to test O2O lists with real references (requires krush.references to be set to real)")
    fun shouldCreateMultilevelLists() {
        transaction {
            SchemaUtils.create(MealPlanTable, RecipeTable, RecipeIngredientsTable, IngredientTable)

            // given
            val pastaIngredient = IngredientTable.insert(Ingredient(name = "Pasta", amount = 300))
            val cheeseIngredient = IngredientTable.insert(Ingredient(name = "Cheese", amount = 100))
            val mushroomIngredient = IngredientTable.insert(Ingredient(name = "Mushroom, Large", amount = 10))
            val eggIngredient = IngredientTable.insert(Ingredient(name = "Eggs", amount = 4))

            val ingredientList = listOf(pastaIngredient, cheeseIngredient, mushroomIngredient, eggIngredient)
            val leftoverMixRecipe = RecipeTable.insert(Recipe(name = "Leftover Mix", ingredients = ingredientList))
            val mealPlan = MealPlanTable.insert(MealPlan(dayOfYear = 123, recipe = leftoverMixRecipe, cook = "Dave"))

            // when
            val selectedMealPlan = Join(
                    table = MealPlanTable,
                    otherTable = RecipeTable,
                    joinType = JoinType.LEFT,
                    onColumn = MealPlanTable.recipeId,
                    otherColumn = RecipeTable.id
            ).join(
                    otherTable = RecipeIngredientsTable,
                    joinType = JoinType.LEFT,
                    onColumn = RecipeTable.id,
                    otherColumn = RecipeIngredientsTable.recipeId
            ).join(
                    otherTable = IngredientTable,
                    joinType = JoinType.LEFT,
                    onColumn = RecipeIngredientsTable.ingredientId,
                    otherColumn = IngredientTable.id
            ).selectAll().toMealPlanList().first()

            // then
            assertThat(selectedMealPlan).isEqualTo(mealPlan)
        }
    }

}
