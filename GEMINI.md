# GEMINI.md

This file serves as a context guide for the Gemini agent when working with the **Poker** project.

## Project Overview

This is a **Kotlin Multiplatform (KMP)** poker application targeting:
*   **Android**
*   **iOS**
*   **Web** (JS & Wasm)
*   **Desktop** (JVM)
*   **Server** (JVM/Ktor)

The project uses **Compose Multiplatform** for a shared UI across all client platforms and **Ktor** for the backend server and client networking.

## Architecture & Modules

The project is structured into four main Gradle modules:

1.  **`kPoker`** (`/kPoker`)
    *   **Purpose:** The core poker engine library.
    *   **Nature:** Pure Kotlin Multiplatform, **NO UI dependencies**.
    *   **Contents:** Game logic, rules, state management (`GameState`), hand evaluation (`HandEvaluator`), and room management.
    *   **Dependencies:** `kotlinx.serialization` for state persistence/networking.

2.  **`composeApp`** (`/composeApp`)
    *   **Purpose:** The client application and UI layer.
    *   **Technology:** Compose Multiplatform.
    *   **Contents:**
        *   `commonMain`: Shared UI code (`App.kt`), ViewModels (if any), and Ktor Client setup.
        *   `androidMain`, `iosMain`, `jvmMain`, `webMain`: Platform-specific entry points and configurations.
    *   **Dependencies:** `kPoker`, `shared`, `ktor-client`.

3.  **`server`** (`/server`)
    *   **Purpose:** The backend game server.
    *   **Technology:** Ktor (Netty).
    *   **Contents:** Game server logic, WebSocket handling, and routing.
    *   **Dependencies:** `kPoker`, `shared`, `ktor-server`.

4.  **`shared`** (`/shared`)
    *   **Purpose:** Cross-cutting concerns and shared utilities between client and server.
    *   **Contents:** Platform abstractions (`Platform.kt`), constants, and shared data models not specific to the game engine.

## Key Technologies & Versions

*   **Language:** Kotlin v2.3.0
*   **UI:** Compose Multiplatform v1.9.3
*   **Networking:** Ktor v3.3.3 (Client & Server)
*   **Serialization:** Kotlinx Serialization v1.9.0
*   **Coroutines:** Kotlinx Coroutines v1.10.2
*   **Build System:** Gradle (Kotlin DSL)

## Build & Run Instructions

**Note:** Commands are for Windows (`.\gradlew.bat`). For macOS/Linux, use `./gradlew`.

### Client Applications

*   **Android:**
    ```powershell
    .\gradlew.bat :composeApp:assembleDebug
    ```
*   **Desktop (JVM):**
    ```powershell
    .\gradlew.bat :composeApp:run
    ```
*   **Web (Wasm - Recommended/Faster):**
    ```powershell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
*   **Web (JS - Legacy/Broad Support):**
    ```powershell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```
*   **iOS:**
    Open `iosApp/iosApp.xcodeproj` in Xcode and run from there.

### Server

*   **Run Server:**
    ```powershell
    .\gradlew.bat :server:run
    ```

### Testing

*   **Run All Tests:**
    ```powershell
    .\gradlew.bat test
    ```
*   **Run Specific Module Tests:**
    ```powershell
    .\gradlew.bat :kPoker:test
    .\gradlew.bat :server:test
    ```
*   **Run Specific Test Class:**
    ```powershell
    .\gradlew.bat :kPoker:test --tests "com.aaronchancey.poker.kpoker.HandEvaluatorTest"
    ```

## Development Conventions

1.  **Code Separation:**
    *   **Strictly** keep game logic in `kPoker`. Do not import Compose or Android libraries in `kPoker`.
    *   UI code belongs in `composeApp`.
2.  **State Management:**
    *   Game state in `kPoker` is generally **immutable**.
    *   Use `copy()` to create modified state instances.
3.  **Dependency Management:**
    *   All dependencies are defined in `gradle/libs.versions.toml`.
    *   Add new dependencies there first, then reference them in `build.gradle.kts`.
4.  **Testing:**
    *   Write unit tests for all game logic in `kPoker/src/commonTest`.
    *   Use `kotlin.test` assertions.

## Directory Shortcuts

*   `composeApp/src/commonMain/kotlin/com/aaronchancey/poker/` -> Shared UI code.
*   `kPoker/src/commonMain/kotlin/com/aaronchancey/poker/` -> Core Game Logic.
*   `server/src/main/kotlin/com/aaronchancey/poker/` -> Server Logic.
