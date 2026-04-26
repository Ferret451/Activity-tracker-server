package com.bsuir.Activity_tracker_server.food

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
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
    fun searchFoodsGet(
        @RequestParam @NotBlank query: String,
        @RequestParam(defaultValue = "1") @Min(1) pageNumber: Int,
        @RequestParam(defaultValue = "25") @Min(1) @Max(200) pageSize: Int,
        @RequestParam(required = false) dataType: List<String>?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) @Pattern(regexp = "(?i)asc|desc") sortOrder: String?,
        @RequestParam(required = false) brandOwner: String?,
    ): FoodSearchResponse =
        foodService.searchGet(
            query = query,
            pageNumber = pageNumber,
            pageSize = pageSize,
            dataTypes = dataType.orEmpty(),
            sortBy = sortBy,
            sortOrder = sortOrder,
            brandOwner = brandOwner,
        )

    @PostMapping("/search")
    fun searchFoodsPost(@Valid @RequestBody request: FoodSearchRequest): FoodSearchResponse =
        foodService.searchPost(
            query = request.query,
            pageNumber = request.pageNumber ?: 1,
            pageSize = request.pageSize ?: 25,
            dataTypes = request.dataType.orEmpty(),
            sortBy = request.sortBy,
            sortOrder = request.sortOrder,
            brandOwner = request.brandOwner,
        )

    @GetMapping("/list")
    fun listFoodsGet(
        @RequestParam(defaultValue = "1") @Min(1) pageNumber: Int,
        @RequestParam(defaultValue = "25") @Min(1) @Max(200) pageSize: Int,
        @RequestParam(required = false) dataType: List<String>?,
        @RequestParam(required = false) sortBy: String?,
    ): List<FoodSummaryResponse> =
        foodService.listGet(
            pageNumber = pageNumber,
            pageSize = pageSize,
            dataTypes = dataType.orEmpty(),
            sortBy = sortBy,
        )

    @PostMapping("/list")
    fun listFoodsPost(@Valid @RequestBody request: FoodListRequest): List<FoodSummaryResponse> =
        foodService.listPost(
            pageNumber = request.pageNumber ?: 1,
            pageSize = request.pageSize ?: 25,
            dataTypes = request.dataType.orEmpty(),
            sortBy = request.sortBy,
        )

    @GetMapping("/{fdcId}")
    fun getFoodById(
        @PathVariable @Positive fdcId: Long,
        @RequestParam(required = false) @Pattern(regexp = "(?i)abridged|full") format: String?,
        @RequestParam(required = false) @Size(max = 100) nutrients: List<@Positive Long>?,
    ): FoodDetailsResponse =
        foodService.getById(
            fdcId = fdcId,
            format = format,
            nutrients = nutrients.orEmpty(),
        )

    @GetMapping
    fun lookupFoodsGet(
        @RequestParam @Size(min = 1, max = 50) fdcIds: List<@Positive Long>,
        @RequestParam(required = false) @Pattern(regexp = "(?i)abridged|full") format: String?,
        @RequestParam(required = false) @Size(max = 100) nutrients: List<@Positive Long>?,
    ): List<FoodDetailsResponse> =
        foodService.lookupByIdsGet(
            fdcIds = fdcIds,
            format = format,
            nutrients = nutrients.orEmpty(),
        )

    @PostMapping("/lookup")
    fun lookupFoodsPost(@Valid @RequestBody request: FoodLookupRequest): List<FoodDetailsResponse> =
        foodService.lookupByIdsPost(
            fdcIds = request.fdcIds,
            format = request.format,
            nutrients = request.nutrients.orEmpty(),
        )

    @PostMapping
    fun lookupFoodsPostAlias(@Valid @RequestBody request: FoodLookupRequest): List<FoodDetailsResponse> =
        foodService.lookupByIdsPost(
            fdcIds = request.fdcIds,
            format = request.format,
            nutrients = request.nutrients.orEmpty(),
        )
}
