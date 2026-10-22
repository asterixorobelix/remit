package com.asterixorobelix.remit.plugins

import com.asterixorobelix.remit.dto.ErrorResponse
import com.asterixorobelix.remit.service.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

/** Generic bodies for error responses that were sent without one (unknown route, 401, 429...). */
private val DEFAULT_ERROR_MESSAGES = mapOf(
    HttpStatusCode.BadRequest to "Bad request",
    HttpStatusCode.Unauthorized to "Authentication required",
    HttpStatusCode.Forbidden to "Forbidden",
    HttpStatusCode.NotFound to "Not found",
    HttpStatusCode.MethodNotAllowed to "Method not allowed",
    HttpStatusCode.PayloadTooLarge to "Request body too large",
    HttpStatusCode.UnsupportedMediaType to "Unsupported media type; send application/json",
    HttpStatusCode.TooManyRequests to "Too many requests; retry later",
)

/**
 * Maps failures to the single [ErrorResponse] shape. Only [ValidationException] messages reach
 * clients; everything unexpected becomes a generic 500 and is logged with its stack trace.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request"))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Malformed or missing request body"))
        }
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respond(HttpStatusCode.UnsupportedMediaType, ErrorResponse(unsupportedMediaTypeMessage()))
        }
        exception<CannotTransformContentToTypeException> { call, _ ->
            call.respond(HttpStatusCode.UnsupportedMediaType, ErrorResponse(unsupportedMediaTypeMessage()))
        }
        exception<PayloadTooLargeException> { call, _ ->
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("Request body too large"))
        }
        exception<NotFoundException> { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Not found"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }

        // Only fill in empty responses so deliberate bodies such as "User not found" are preserved.
        DEFAULT_ERROR_MESSAGES.forEach { (code, message) ->
            status(code) { status ->
                if (content.isEmptyBody()) call.respond(status, ErrorResponse(message))
            }
        }
    }
}

private fun unsupportedMediaTypeMessage() = DEFAULT_ERROR_MESSAGES.getValue(HttpStatusCode.UnsupportedMediaType)

private fun OutgoingContent.isEmptyBody(): Boolean = this is OutgoingContent.NoContent || contentLength == 0L
