# Mobile module

Compose Multiplatform client targeting Android, iOS, and a desktop component gallery.
Read [AGENTS.md](AGENTS.md) for current functionality and implementation contracts.

```bash
./gradlew test detekt :androidApp:assembleDebug :androidApp:assembleRelease
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :composeApp:verifySqlDelightMigration
./gradlew :composeApp:hotRunDesktop --mainClass com.example.myproject.gallery.ComponentGalleryKt
```

`androidApp` produces the APK. `composeApp` owns shared UI/domain/data and platform
actuals. Runtime feature modules are selected from capabilities in `app.properties`;
navigation only exposes routes whose capability is enabled (`NavigationFeatures`).

Key entry points: `di/AppModule.kt`, `navigation/AppNavigation.kt`,
`presentation/app/AppViewModel.kt`, `data/network/ApiHttpClient.kt`, `data/auth`,
`data/task`, `presentation/tasks`, `commonMain/sqldelight`, and the desktop gallery.
