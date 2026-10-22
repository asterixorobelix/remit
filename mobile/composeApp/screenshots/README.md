# Screenshot baselines

Roborazzi baselines for the desktop screenshot tests
(`src/desktopTest/kotlin/com/asterixorobelix/remit/screenshots/ScreenshotTest.kt`) live in `desktop/`.

CI records them on Linux. Fonts render differently on macOS and Windows, so never commit images
recorded on another OS: they would fail verification in CI.

| Command (from `mobile/`) | Effect |
|---|---|
| `./gradlew :composeApp:recordRoborazziDesktop` | Record or update the baselines (CI) |
| `./gradlew :composeApp:verifyRoborazziDesktop` | Fail on a missing or changed screenshot |
| `./gradlew :composeApp:compareRoborazziDesktop` | Report differences without failing |

Diff images (`*_compare.png`, `*_actual.png`) are written to `build/outputs/roborazzi/desktop/` and
the HTML report to `build/reports/roborazzi/desktop/index.html`. Plain `./gradlew test` only
composes the screens and never compares images.
