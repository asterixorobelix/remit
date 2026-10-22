# Coding Conventions

**Analysis Date:** 2026-01-15

## Naming Patterns

**Files:**
- PascalCase for classes (`UserViewModel.kt`, `UserRepository.kt`)
- PascalCase for composables (`UserScreen.kt`, `App.kt`)
- lowercase for configs (`detekt.yml`, `strings.xml`)
- UPPERCASE for docs (`README.md`, `CLAUDE.md`)

**Functions:**
- camelCase for all functions (`loadUsers`, `refreshData`)
- Exception: PascalCase for @Composable functions (`UserScreen`, `EmptyState`)
- No special prefix for async/suspend functions

**Variables:**
- camelCase for variables (`userName`, `isLoading`)
- UPPER_SNAKE_CASE for top-level constants (`TOTAL_PAGES`, `API_BASE_URL`)
- Underscore prefix for private backing fields (`_users`, `_isLoading`)

**Types:**
- PascalCase for interfaces, no I prefix (`UserRepository`, not `IUserRepository`)
- PascalCase for type aliases (`CreateUserRequest`, `UpdateUserRequest`)
- UPPER_CASE for enum values (`Environment.DEVELOPMENT`, `Environment.PRODUCTION`)

## Code Style

**Formatting:**
- Detekt with autoCorrect enabled (`backend/detekt.yml`, `mobile/detekt-mobile.yml`)
- 4-space indentation
- 120 character line length maximum
- No semicolons (enforced by `NoSemicolons` rule)

**Linting:**
- Detekt configured with zero tolerance (`maxIssues: 0`)
- Run: `./gradlew detekt` for analysis
- Run: `./gradlew detektFormat` for auto-fix
- CI blocks on any Detekt violations

## Import Organization

**Order:**
1. Kotlin standard library
2. External packages (Ktor, Compose, Koin)
3. Internal modules (project packages)
4. Type imports last

**Grouping:**
- Blank line between groups
- No wildcard imports except for Compose (allowed in mobile config)

**Path Aliases:**
- None defined - use full package paths

## Error Handling

**Patterns:**
- Backend: Throw exceptions, catch in `StatusPages` plugin
- Mobile: Return `Result<T>`, handle with `.onSuccess`/`.onFailure`
- Errors logged with context before handling

**Error Types:**
- Throw on: Invalid input, missing dependencies, invariant violations
- Return Result on: Expected failures (network, API errors)
- Log before throw: `log.error("Context message", exception)`

## Logging

**Framework:**
- Backend: Logback via SLF4J (`Application.log.info()`)
- Mobile: Platform `Logger` abstraction via Koin

**Patterns:**
- Structured logging with context
- Log levels: DEBUG (dev), INFO (operations), WARN (issues), ERROR (failures)
- No `println` in committed code

## Comments

**When to Comment:**
- Explain "why" not "what"
- Document business rules and edge cases
- Use section dividers for file organization

**KDoc:**
- Required for public APIs in shared code
- Format: `/** Description */` with `@param`, `@return` tags

**Section Dividers:**
```kotlin
// ============================================
// Section Name
// ============================================
```

**TODO Comments:**
- Format: `// TODO: description`
- Note: ForbiddenComment rule flags TODO, FIXME, STOPSHIP in CI

## Function Design

**Size:**
- Keep under 60 lines (enforced by Detekt `LongMethod`)
- Extract helpers for complex logic

**Parameters:**
- Max 6 parameters (enforced by `LongParameterList`)
- Use data classes for 4+ related parameters
- Destructure in function signatures when appropriate

**Return Values:**
- Explicit return types for public functions
- Return early for guard clauses
- Use `Result<T>` for operations that can fail

## Module Design

**Exports:**
- Named exports preferred
- One class per file (matching filename)

**Barrel Files:**
- Not used - import from specific files
- DI modules aggregate dependencies

**Architecture Boundaries:**
- Presentation depends on Domain/Data
- Data layer depends on Platform
- Domain layer has no dependencies on other layers

---

*Convention analysis: 2026-01-15*
*Update when patterns change*
