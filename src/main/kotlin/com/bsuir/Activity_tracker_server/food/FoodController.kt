package com.bsuir.Activity_tracker_server.food

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/foods")
@Validated
class FoodController(
    private val foodService: FoodService,
) {
    @GetMapping("/search")
    fun searchFoods(
        @RequestParam @NotBlank query: String,
        @RequestParam(defaultValue = "1") @Min(1) pageNumber: Int,
        @RequestParam(defaultValue = "25") @Min(1) @Max(200) pageSize: Int,
        @RequestParam(required = false) dataType: List<String>?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) @Pattern(regexp = "(?i)asc|desc") sortOrder: String?,
        @RequestParam(required = false) brandOwner: String?,
    ): FoodSearchResponse =
        foodService.search(
            query = query,
            pageNumber = pageNumber,
            pageSize = pageSize,
            dataTypes = dataType.orEmpty(),
            sortBy = sortBy,
            sortOrder = sortOrder,
            brandOwner = brandOwner,
        )

    @GetMapping("/{fdcId}")
    fun getFoodById(@PathVariable @Positive fdcId: Long): FoodDetailsResponse =
        foodService.getById(fdcId)

    @PostMapping("/lookup")
    fun lookupFoods(@Valid @RequestBody request: FoodLookupRequest): List<FoodDetailsResponse> =
        foodService.lookupByIds(request.fdcIds)
}


