package com.bsuir.Activity_tracker_server.finance

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class FinanceApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `income expense accounts report and primary account on first record`() {
        val accessToken = registerUser("finance").accessToken

        val incomeCategoryId =
            mockMvc.perform(
                post("/api/me/finance/income/categories")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Salary"}"""),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.operationType").value("INCOME"))
                .andReturn().response.contentAsString
                .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(get("/api/me/finance/income/categories").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(incomeCategoryId))

        val expenseCategoryId =
            mockMvc.perform(
                post("/api/me/finance/expense/categories")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Food"}"""),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.operationType").value("EXPENSE"))
                .andReturn().response.contentAsString
                .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(get("/api/me/finance/accounts").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        mockMvc.perform(
            post("/api/me/finance/income/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":"$incomeCategoryId","amount":1000.00,"note":"May"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.account.name").value("Основной"))
            .andExpect(jsonPath("$.amount").value(1000.00))

        mockMvc.perform(get("/api/me/finance/accounts").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].balance").value(1000.00))
            .andExpect(jsonPath("$[0].isPrimary").value(true))

        mockMvc.perform(
            post("/api/me/finance/expense/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":"$expenseCategoryId","amount":200.50}"""),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(get("/api/me/finance/accounts").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].balance").value(799.50))

        val today = LocalDate.now().toString()
        mockMvc.perform(
            get("/api/me/finance/report")
                .withBearer(accessToken)
                .param("from", today)
                .param("to", today),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome").value(1000.00))
            .andExpect(jsonPath("$.totalExpense").value(200.50))
            .andExpect(jsonPath("$.net").value(799.50))
            .andExpect(jsonPath("$.incomeRecords.length()").value(1))
            .andExpect(jsonPath("$.expenseRecords.length()").value(1))
            .andExpect(jsonPath("$.byCategory.length()").value(2))

        mockMvc.perform(
            post("/api/me/finance/accounts")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Накопления","setAsPrimary":false}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.isPrimary").value(false))

        val accountsJson = mockMvc.perform(get("/api/me/finance/accounts").withBearer(accessToken))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
            .let { objectMapper.readTree(it) }

        val savingsId = accountsJson[1].path("id").asText()

        mockMvc.perform(
            post("/api/me/finance/income/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":"$incomeCategoryId","accountId":"$savingsId","amount":300}"""),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(get("/api/me/finance/accounts").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].balance").value(799.50))
            .andExpect(jsonPath("$[1].balance").value(300.00))

        mockMvc.perform(
            get("/api/me/finance/report")
                .withBearer(accessToken)
                .param("from", today)
                .param("to", today)
                .param("accountId", savingsId),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome").value(300.00))
            .andExpect(jsonPath("$.totalExpense").value(0))
    }

    @Test
    fun `delete empty non primary account succeeds`() {
        val accessToken = registerUser("finance-del").accessToken

        mockMvc.perform(
            post("/api/me/finance/accounts")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"A"}"""),
        ).andExpect(status().isCreated)

        val extraId =
            mockMvc.perform(
                post("/api/me/finance/accounts")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"B"}"""),
            )
                .andExpect(status().isCreated)
                .andReturn().response.contentAsString
                .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(delete("/api/me/finance/accounts/$extraId").withBearer(accessToken))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `expense branch rejects income category on create`() {
        val accessToken = registerUser("finance-br").accessToken

        val incomeCat =
            mockMvc.perform(
                post("/api/me/finance/income/categories")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"X"}"""),
            )
                .andExpect(status().isCreated)
                .andReturn().response.contentAsString
                .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(
            post("/api/me/finance/expense/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":"$incomeCat","amount":10}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update record adjusts account balance when amount changes`() {
        val accessToken = registerUser("finance-upd").accessToken

        val cat =
            mockMvc.perform(
                post("/api/me/finance/expense/categories")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Shop"}"""),
            ).andExpect(status().isCreated)
                .andReturn().response.contentAsString
                .let { objectMapper.readTree(it).path("id").asText() }

        val recordId =
            mockMvc.perform(
                post("/api/me/finance/expense/records")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"categoryId":"$cat","amount":50}"""),
            )
                .andExpect(status().isCreated)
                .andReturn().response.contentAsString
                .let { objectMapper.readTree(it).path("id").asText() }

        val occurredAt =
            mockMvc.perform(get("/api/me/finance/expense/records/$recordId").withBearer(accessToken))
                .andExpect(status().isOk)
                .andReturn().response.contentAsString
                .let { objectMapper.readTree(it).path("occurredAt").asText() }

        mockMvc.perform(
            put("/api/me/finance/expense/records/$recordId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":"$cat","amount":25,"occurredAt":"$occurredAt"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/me/finance/accounts").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].balance").value(-25))
    }
}
