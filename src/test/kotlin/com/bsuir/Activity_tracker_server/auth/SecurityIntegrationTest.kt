package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType

class SecurityIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `protected endpoints reject anonymous requests`() {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/me/profile"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/foods/search").param("query", "apple"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `user can fetch own user record`() {
        val user = registerUser("selfget")
        mockMvc.perform(get("/api/users/${user.userId}").withBearer(user.accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.userId.toString()))
    }

    @Test
    fun `user cannot fetch another user record`() {
        val userA = registerUser("usera")
        val userB = registerUser("userb")
        mockMvc.perform(get("/api/users/${userB.userId}").withBearer(userA.accessToken))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `protected endpoints allow bearer token`() {
        val accessToken = registerUser("security").accessToken

        mockMvc.perform(get("/api/me/settings").withBearer(accessToken))
            .andExpect(status().isOk)
    }

    @Test
    fun `non admin cannot access user management`() {
        val accessToken = registerUser("noadmin").accessToken

        mockMvc.perform(get("/api/users").withBearer(accessToken))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `user can change own password`() {
        val user = registerUser("pwdchg")
        mockMvc.perform(
            put("/api/me/password")
                .withBearer(user.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"password123","newPassword":"newpassword456"}"""),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            put("/api/me/password")
                .withBearer(user.accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"password123","newPassword":"another789012"}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `admin can access user management`() {
        val accessToken = registerAdminUser("adm").accessToken

        mockMvc.perform(get("/api/users").withBearer(accessToken))
            .andExpect(status().isOk)
    }
}

