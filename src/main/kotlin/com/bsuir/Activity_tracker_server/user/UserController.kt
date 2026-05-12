package com.bsuir.Activity_tracker_server.user

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun getAll(): List<UserResponse> = userService.getAll()

    @GetMapping("/{id}")
    @PreAuthorize("@accessPolicy.isSelfOrAdmin(#id, authentication)")
    fun getById(@PathVariable id: UUID): UserResponse = userService.getById(id)

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateUserRequest): UserResponse =
        userService.create(request)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserResponse = userService.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) {
        userService.delete(id)
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun updateRole(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRoleRequest,
    ): UserResponse = userService.updateRole(id, request)
}
