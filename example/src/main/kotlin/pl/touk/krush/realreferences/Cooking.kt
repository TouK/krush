package pl.touk.krush.realreferences

import javax.persistence.*

@Entity
data class MealPlan(
        @Id @GeneratedValue
        val id: Int? = null,
        val dayOfYear: Int,
        @OneToOne
        val recipe: Recipe?,
        val cook: String
)

@Entity
data class Recipe(
        @Id @GeneratedValue
        val id: Int? = null,
        val name: String,
        @ManyToMany
        @JoinTable(name = "recipe_ingredients")
        val ingredients: List<Ingredient>
)

@Entity
data class Ingredient(
        @Id @GeneratedValue
        val id: Int? = null,
        val name: String,
        val amount: Int
)
