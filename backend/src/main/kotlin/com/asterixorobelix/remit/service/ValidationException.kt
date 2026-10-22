package com.asterixorobelix.remit.service

/**
 * A deliberate client input error. Its message is safe to return to clients (HTTP 400);
 * every other exception is reported as a generic 500.
 */
class ValidationException(message: String) : RuntimeException(message)
