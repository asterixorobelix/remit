# Testing Patterns

**Analysis Date:** 2026-01-15

## Test Framework

**Runner:**
- Backend: Kotest 6.0.7 with JUnit5 runner (`backend/build.gradle.kts`)
- Mobile: Kotlin Test with JUnit4 (`mobile/gradle/libs.versions.toml`)

**Assertion Library:**
- Kotlin Test built-in assertions (`assertEquals`, `assertTrue`)
- Kotest matchers available for backend

**Run Commands:**
```bash
# Backend
cd backend
./gradlew test                              # Run all tests
./gradlew test --tests "*.ApplicationTest"  # Single test class
./gradlew jacocoTestReport                  # Coverage report

# Mobile
cd mobile
./gradlew test                              # Run all tests
./gradlew :composeApp:testDebugUnitTest     # Android unit tests
./gradlew :composeApp:iosSimulatorArm64Test # iOS tests
```

## Test File Organization

**Location:**
- Backend: `backend/src/test/kotlin/com/example/myproject/`
- Mobile Common: `mobile/composeApp/src/commonTest/kotlin/com/example/myproject/`
- Mobile Android: `mobile/composeApp/src/androidTest/kotlin/com/example/myproject/`

**Naming:**
- Unit tests: `{Class}Test.kt` (`ApplicationTest.kt`, `ComposeAppCommonTest.kt`)
- Integration: Same pattern, test content determines scope
- Screenshot: `ScreenshotTests.kt` (Android instrumented)

**Structure:**
```
backend/
  src/test/kotlin/com/example/myproject/
    ApplicationTest.kt

mobile/
  composeApp/
    src/commonTest/kotlin/com/example/myproject/
      ComposeAppCommonTest.kt
    src/androidTest/kotlin/com/example/myproject/
      screenshots/ScreenshotTests.kt
```

## Test Structure

**Suite Organization:**
```kotlin
class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        // arrange - setup via testApplication

        // act
        client.get("/").apply {
            // assert
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            val json = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertEquals("healthy", json["status"]?.jsonPrimitive?.content)
        }
    }
}
```

**Patterns:**
- Use `testApplication { }` block for Ktor integration tests
- AAA pattern: Arrange, Act, Assert (documented in CLAUDE.md)
- One assertion focus per test

## Mocking

**Framework:**
- MockK 1.14.7 for backend (`backend/build.gradle.kts`)
- Not currently configured for mobile (opportunity for improvement)

**Patterns:**
```kotlin
import io.mockk.mockk
import io.mockk.every
import io.mockk.coEvery

// Mock creation
val repository = mockk<UserRepository>()

// Stubbing
every { repository.getUser(1) } returns User(1, "test@example.com")
coEvery { repository.refreshUsers() } returns Unit

// Verification
verify { repository.getUser(1) }
```

**What to Mock:**
- External APIs (HTTP clients)
- Database access
- Platform services (Logger, Analytics)

**What NOT to Mock:**
- Pure functions
- Data classes
- Simple utilities

## Fixtures and Factories

**Test Data:**
```kotlin
// Factory functions
fun createTestUser(
    id: Long = 1L,
    email: String = "test@example.com",
    name: String = "Test User"
): User = User(id, email, name)

// In tests
@Test
fun testUserCreation() {
    val user = createTestUser(email = "custom@example.com")
    // test with customized fixture
}
```

**Location:**
- Factory functions: Define in test file or shared test utils
- No dedicated fixtures directory currently

## Coverage

**Requirements:**
- Target: 80% line coverage (enforced in CI for backend)
- Enforcement: JaCoCo plugin with minimum threshold

**Configuration:**
- Backend: JaCoCo configured in `backend/build.gradle.kts`
- Reports: HTML and XML formats generated

**View Coverage:**
```bash
cd backend
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Test Types

**Unit Tests:**
- Scope: Single function/class in isolation
- Mocking: Mock all external dependencies
- Speed: <100ms per test
- Examples: `ApplicationTest.kt` tests endpoints

**Integration Tests:**
- Scope: Multiple modules together
- Mocking: Mock only external boundaries
- Setup: `testApplication { }` for Ktor
- Examples: API endpoint tests with real routing

**Screenshot Tests:**
- Framework: Android instrumented tests
- Scope: Visual regression for UI components
- Location: `mobile/composeApp/src/androidTest/.../screenshots/`
- Note: Not fully implemented (disabled test exists)

## Common Patterns

**Async Testing:**
```kotlin
@Test
fun testAsyncOperation() = testApplication {
    // testApplication handles coroutines
    client.get("/async").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}
```

**Error Testing:**
```kotlin
@Test
fun testNotFoundEndpoint() = testApplication {
    client.get("/nonexistent").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}
```

**JSON Response Testing:**
```kotlin
@Test
fun testHealthEndpoint() = testApplication {
    client.get("/health").apply {
        val json = Json.parseToJsonElement(bodyAsText()).jsonObject
        assertEquals("healthy", json["status"]?.jsonPrimitive?.content)
    }
}
```

**Snapshot Testing:**
- Not currently used in this codebase
- Consider for UI component regression testing

---

*Testing analysis: 2026-01-15*
*Update when test patterns change*
