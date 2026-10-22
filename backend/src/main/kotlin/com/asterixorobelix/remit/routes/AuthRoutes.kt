package com.asterixorobelix.remit.routes

import com.asterixorobelix.remit.dto.ErrorResponse
import com.asterixorobelix.remit.dto.LoginRequest
import com.asterixorobelix.remit.dto.LogoutRequest
import com.asterixorobelix.remit.dto.RefreshRequest
import com.asterixorobelix.remit.dto.RegisterRequest
import com.asterixorobelix.remit.dto.UpdateProfileRequest
import com.asterixorobelix.remit.plugins.AUTH_RATE_LIMIT
import com.asterixorobelix.remit.plugins.JWT_AUTH
import com.asterixorobelix.remit.plugins.REFRESH_RATE_LIMIT
import com.asterixorobelix.remit.plugins.rateLimited
import com.asterixorobelix.remit.plugins.userId
import com.asterixorobelix.remit.service.AuthService
import com.asterixorobelix.remit.service.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val USER_NOT_FOUND = ErrorResponse("User not found")

fun Route.authRoutes(authService: AuthService, rateLimitingEnabled: Boolean) {
    route("/api/v1/auth") {
        rateLimited(AUTH_RATE_LIMIT, rateLimitingEnabled) {
            post("/register") {
                val request = call.receive<RegisterRequest>()
                val tokens = authService.register(request.email, request.password, request.displayName)
                if (tokens == null) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("Email already registered"))
                } else {
                    call.respond(HttpStatusCode.Created, tokens)
                }
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                val tokens = authService.login(request.email, request.password)
                if (tokens == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid email or password"))
                } else {
                    call.respond(tokens)
                }
            }
        }

        rateLimited(REFRESH_RATE_LIMIT, rateLimitingEnabled) {
            post("/refresh") {
                val request = call.receive<RefreshRequest>()
                val tokens = authService.refresh(request.refreshToken)
                if (tokens == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired refresh token"))
                } else {
                    call.respond(tokens)
                }
            }

            post("/logout") {
                val request = call.receive<LogoutRequest>()
                authService.logout(request.refreshToken)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

fun Route.userRoutes(authService: AuthService) {
    authenticate(JWT_AUTH) {
        route("/api/v1/users/me") {
            get {
                val user = authService.findUserById(call.userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, USER_NOT_FOUND)
                } else {
                    call.respond(user.toResponse())
                }
            }

            put {
                val request = call.receive<UpdateProfileRequest>()
                val user = authService.updateProfile(call.userId, request.displayName)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, USER_NOT_FOUND)
                } else {
                    call.respond(user.toResponse())
                }
            }

            // Account deletion (App Store Review Guideline 5.1.1(v)); tasks and sessions cascade.
            delete {
                if (authService.deleteAccount(call.userId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, USER_NOT_FOUND)
                }
            }
        }
    }
}
