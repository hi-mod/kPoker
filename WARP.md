# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform** poker application supporting Android, iOS, Web (JS/Wasm), Desktop (JVM), and Server platforms. The project uses **Compose Multiplatform** for UI across all platforms and **Ktor** for the server.

## Common Commands

All commands use PowerShell on Windows. Use `.\gradlew.bat` instead of `./gradlew` on Windows.

### Build Commands
```powershell
# Build all targets
.\gradlew.bat build

# Build specific platforms
.\gradlew.bat :composeApp:assembleDebug        # Android
.\gradlew.bat :composeApp:packageDistributionForCurrentOS  # Desktop
.\gradlew.bat :composeApp:jsBrowserDevelopmentWebpack      # Web (JS)
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentWebpack  # Web (Wasm)
.\gradlew.bat :server:build                    # Server
```

### Run Commands
```powershell
# Run desktop application
.\gradlew.bat :composeApp:run

# Run server
.\gradlew.bat :server:run

# Run web application
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun  # Wasm (faster, modern browsers)
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun      # JS (slower, older browsers)
```

### Testing
```powershell
# Run all tests
.\gradlew.bat test

# Run tests for specific modules
.\gradlew.bat :kPoker:test          # Core poker logic tests
.\gradlew.bat :shared:test          # Shared code tests
.\gradlew.bat :composeApp:test      # UI tests
.\gradlew.bat :server:test          # Server tests

# Run specific test class
.\gradlew.bat :kPoker:test --tests "com.aaronchancey.poker.kpoker.HandEvaluatorTest"
```

### Clean
```powershell
.\gradlew.bat clean
```

## Architecture

### Module Structure

The project is organized into 4 main Gradle modules:

#### 1. **kPoker** (`kPoker/`)
The core poker engine library - a pure Kotlin Multiplatform module with **no UI dependencies**. Contains all game logic and rules.

**Key packages:**
- `betting/` - Betting actions, rounds, and validation (`Action`, `BettingManager`, `BettingRound`)
- `core/` - Fundamental poker primitives (`Card`, `Deck`, `Rank`, `Suit`, `HandRank`)
- `evaluation/` - Hand evaluation algorithms (`HandEvaluator`, `StandardHandEvaluator`, `LoHandEvaluator`)
- `game/` - Game state and flow (`PokerGame`, `GameState`, `GamePhase`)
- `player/` - Player management (`Player`, `PlayerState`, `Table`, `Pot`, `PotManager`)
- `room/` - Multi-player room management (`Room`, `RoomConfig`, `SeatManager`)
- `formats/` - Game format configurations (`CashGame`, `Tournament`, `TournamentConfig`)
- `variants/` - Poker variants (`PokerVariant`, `TexasHoldem`, `Omaha`)
- `events/` - Event system (`GameEvent`, `GameController`)

**Design patterns:**
- **Immutable state**: `GameState` and related classes use immutable data structures with copy-on-write semantics
- **Event-driven**: `GameEvent` emissions for state changes, listeners can subscribe via `addEventListener()`
- **Strategy pattern**: `PokerVariant` interface allows pluggable game variants (Texas Hold'em, Omaha, etc.)
- **Abstract template**: `PokerGame` abstract class provides common game flow, variants override specific behavior

#### 2. **composeApp** (`composeApp/`)
The **UI layer** using Compose Multiplatform. Contains platform-specific entry points and shared UI code.

**Platform entry points:**
- `androidMain/` - `MainActivity.kt`
- `jvmMain/` - `main.kt` (Desktop)
- `iosMain/` - `MainViewController.kt`
- `webMain/` - `Main.kt` (JS/Wasm)

**Main class**: `App.kt` in `commonMain/` - shared Compose UI across all platforms

#### 3. **server** (`server/`)
A **Ktor server** application. JVM-only module.

**Files:**
- `Application.kt` - Server entry point and routing configuration
- `ApplicationTest.kt` - Server integration tests

**Main class**: `com.aaronchancey.poker.ApplicationKt`

#### 4. **shared** (`shared/`)
Cross-cutting concerns and utilities shared between client and server.

**Files:**
- `Platform.kt` - Platform-specific implementations (with platform variants in `androidMain/`, `iosMain/`, `jvmMain/`, `jsMain/`, `wasmJsMain/`)
- `Constants.kt` - App-wide constants
- `Greeting.kt` - Example shared code

### Key Design Principles

1. **Multiplatform-first**: All core logic in `kPoker` is platform-agnostic and works across Android, iOS, Web, Desktop, and Server

2. **Separation of concerns**:
   - **kPoker** = Pure game logic (no UI, no networking)
   - **composeApp** = UI layer only (delegates to kPoker for game logic)
   - **server** = Network layer and server-side logic
   - **shared** = Cross-cutting utilities

3. **Game flow architecture**:
   - Game state is centralized in `GameState` (immutable)
   - State transitions happen through actions processed by `PokerGame.processAction()`
   - UI subscribes to `GameEvent` emissions to update displays
   - Room management (`Room` class) handles seating, spectators, and visibility

4. **Visibility and security**:
   - `Room.getVisibleGameState()` filters game state based on viewer (hide other players' hole cards)
   - Distinction between seated players and spectators

## iOS Development

For iOS builds, open the `iosApp/` directory in Xcode. The Compose Multiplatform UI is integrated via the framework generated in `composeApp`.

## Platform Targets

- **Android**: Min SDK 24, Target SDK and Compile SDK defined in `libs.versions.toml`
- **iOS**: Arm64 (device) and Simulator Arm64
- **Desktop**: JVM 11+ (cross-platform via Compose Desktop)
- **Web**: JavaScript and WebAssembly (Wasm) builds
- **Server**: JVM 11+ (Ktor on Netty)

## Testing Strategy

Tests are colocated with source code in `commonTest/` directories. The test framework is **kotlin.test**.

**Example test files:**
- `HandEvaluatorTest.kt` - Tests hand evaluation (Royal Flush, Straight, etc.)
- `CardTest.kt` - Tests card parsing and comparison
- `RoomTest.kt` - Tests room management and seating

**Card notation**: Tests use shorthand notation like `"As"` (Ace of Spades), `"Kh"` (King of Hearts) via `Card.fromString()`

## Known Issues

The Gradle configuration shows deprecation warnings about Kotlin Multiplatform plugin compatibility with Android Gradle Plugin. These warnings indicate that AGP 9.0 will require structural changes (moving Android application config to separate subproject). This is currently a warning and doesn't affect functionality.
