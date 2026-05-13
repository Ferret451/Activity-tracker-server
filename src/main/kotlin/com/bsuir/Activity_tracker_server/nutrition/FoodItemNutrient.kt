package com.bsuir.Activity_tracker_server.nutrition

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "food_item_nutrients")
class FoodItemNutrient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_item_id", nullable = false)
    var foodItem: FoodItem,

    @Column(name = "nutrient_number", length = 16)
    var nutrientNumber: String? = null,

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "unit_name", length = 32)
    var unitName: String? = null,

    @Column(name = "amount_per_100g", nullable = false)
    var amountPer100g: BigDecimal,
)
