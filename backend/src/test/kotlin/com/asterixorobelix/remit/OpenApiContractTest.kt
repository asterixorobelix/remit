package com.asterixorobelix.remit

import io.ktor.server.application.plugin
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.PathSegmentConstantRouteSelector
import io.ktor.server.routing.PathSegmentParameterRouteSelector
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Keeps `resources/openapi/documentation.yaml` in step with the routes the application serves, so
 * a route added, removed, or renamed without updating the spec (or the reverse) fails the build.
 */
class OpenApiContractTest {
    @Test
    fun `every served route is documented and every documented route is served`() = testApplication {
        var served: Set<String> = emptySet()
        application {
            // Swagger UI serves the spec itself and is not part of the API surface.
            module(testConfig(swaggerEnabled = false))
            served = plugin(RoutingRoot).getAllRoutes().mapNotNull(::operationOf).toSet()
        }
        startApplication()

        val documented = documentedOperations()
        assertEquals(
            documented.sorted(),
            served.sorted(),
            "Routes and openapi/documentation.yaml differ. Undocumented: ${served - documented}; " +
                "documented but not served: ${documented - served}",
        )
    }

    /** "get /api/v1/tasks" for a route with a handler, or null for intermediate nodes. */
    private fun operationOf(route: RoutingNode): String? {
        val segments = mutableListOf<String>()
        var method: String? = null
        var node: RoutingNode? = route
        while (node != null) {
            when (val selector = node.selector) {
                is HttpMethodRouteSelector -> method = selector.method.value.lowercase()

                is PathSegmentConstantRouteSelector -> segments += selector.value

                is PathSegmentParameterRouteSelector -> segments += "{${selector.name}}"

                // Authentication, rate limiting, and similar selectors do not change the path.
                else -> Unit
            }
            node = node.parent
        }
        return method?.let { "$it /" + segments.asReversed().joinToString("/") }
    }

    /** Operations under `paths:` in the spec, in the same "method /path" form. */
    private fun documentedOperations(): Set<String> {
        val spec = assertNotNull(javaClass.getResource("/openapi/documentation.yaml")).readText()
        val pathLine = Regex("""^ {2}(/\S*):\s*$""")
        val methodLine = Regex("""^ {4}(get|put|post|delete|patch|head|options):\s*$""")
        var inPaths = false
        var path: String? = null
        val operations = mutableSetOf<String>()
        spec.lineSequence().forEach { line ->
            if (line.isNotEmpty() && !line.first().isWhitespace()) inPaths = line.startsWith("paths:")
            if (!inPaths) return@forEach
            pathLine.find(line)?.let { path = it.groupValues[1] }
            methodLine.find(line)?.let { match -> path?.let { operations += "${match.groupValues[1]} $it" } }
        }
        return operations
    }
}
