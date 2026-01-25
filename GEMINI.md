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
    *   **Internal Structure:**
        *   `betting/`: Betting actions, rounds, and validation (`Action`, `BettingManager`).
        *   `core/`: Fundamental primitives (`Card`, `Deck`, `Rank`, `Suit`).
        *   `evaluation/`: Hand evaluation algorithms (`HandEvaluator`).
        *   `game/`: Game state and flow (`PokerGame`, `GameState`, `GamePhase`).
        *   `player/`: Player management (`Player`, `Table`, `PotManager`).
        *   `room/`: Multi-player room management (`Room`, `SeatManager`).
        *   `variants/`: Poker variants (`PokerVariant`, `TexasHoldem`).
    *   **Design Patterns:**
        *   **Immutable State:** `GameState` is immutable; transitions create new instances.
        *   **Event-Driven:** Uses `GameEvent` for state changes.
        *   **Strategy Pattern:** `PokerVariant` interface for pluggable rules.
    *   **Dependencies:** `kotlinx.serialization` for state persistence/networking.

2.  **`composeApp`** (`/composeApp`)
    *   **Purpose:** The client application and UI layer.
    *   **Technology:** Compose Multiplatform, Koin (DI), Material3 Expressive.
    *   **Architecture:** MVI (Model-View-Intent).
        *   **ViewModel:** Uses `Koin`'s `viewModel` and `koinViewModel()`.
        *   **State:** `StateFlow` exposing a UI State data class.
        *   **Intents:** Sealed interfaces/classes (e.g., `RoomIntent`) to capture user actions.
        *   **Effects:** Sealed interfaces/classes (e.g., `RoomEffect`) for side effects like Toasts/Navigation.
    *   **Contents:**
        *   `commonMain`: Shared UI code (`App.kt`), ViewModels, DI setup (`AppModule.kt`), and Ktor Client.
        *   `androidMain`, `iosMain`, `jvmMain`, `webMain`: Platform-specific entry points.
    *   **Dependencies:** `kPoker`, `shared`, `ktor-client`, `koin-compose`, `androidx-navigation3` (experimental).

3.  **`server`** (`/server`)
    *   **Purpose:** The backend game server.
    *   **Technology:** Ktor (Netty).
    *   **Contents:** Game server logic, WebSocket handling, routing, and persistence.
    *   **Dependencies:** `kPoker`, `shared`, `ktor-server`.

4.  **`shared`** (`/shared`)
    *   **Purpose:** Cross-cutting concerns and shared utilities between client and server.
    *   **Contents:** Platform abstractions (`Platform.kt`), constants, and shared data models.

## Key Technologies & Versions

*   **Language:** Kotlin v2.3.20-Beta1
*   **UI:** Compose Multiplatform v1.10.0 (Material3 Expressive Theme)
*   **Dependency Injection:** Koin v4.2.0-beta4
*   **Networking:** Ktor v3.4.0 (Client & Server)
*   **Serialization:** Kotlinx Serialization v1.10.0
*   **Coroutines:** Kotlinx Coroutines v1.10.2
*   **Settings:** Multiplatform Settings v1.3.0
*   **Testing:** MockK v1.14.7, `kotlin.test`
*   **Build System:** Gradle (Kotlin DSL) v8.12.0 (AGP)

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
2.  **Dependency Injection:**
    *   Use **Koin** for all DI.
    *   Define modules in `composeApp/src/commonMain/kotlin/com/aaronchancey/poker/di/AppModule.kt`.
    *   Use `koinViewModel<MyViewModel>()` in Composables.
    *   Pass parameters to ViewModels using `parametersOf()`.
3.  **State Management:**
    *   Game state in `kPoker` is **immutable**.
    *   UI State should be exposed as `StateFlow`.
    *   Use `Event` wrapper or `Channel`/`Flow` for one-off UI effects (toasts, navigation).
4.  **UI Components:**
    *   Use `MaterialExpressiveTheme`.
    *   Place `Modifier` as the first parameter of Composables.
5.  **Testing:**
    *   Write unit tests for all game logic in `kPoker/src/commonTest`.
    *   Use `kotlin.test` assertions.
    *   **Card Notation:** Tests use shorthand strings like `"As"` (Ace of Spades), `"Kh"` (King of Hearts), `"10d"` (Ten of Diamonds) via `Card.fromString()`. Valid suits are `s, h, d, c`.

## Directory Shortcuts

*   `composeApp/src/commonMain/kotlin/com/aaronchancey/poker/` -> Shared UI code.
*   `kPoker/src/commonMain/kotlin/com/aaronchancey/poker/` -> Core Game Logic.
*   `server/src/main/kotlin/com/aaronchancey/poker/` -> Server Logic.