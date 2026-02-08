# Copilot Instructions

Kotlin Multiplatform poker application targeting Android, iOS, Web (JS/Wasm), Desktop (JVM), and Server. Uses Compose Multiplatform for shared UI and Ktor for backend server with WebSocket-based real-time multiplayer.

## Build & Run

```bash
# Build all
./gradlew build

# Run desktop client
./gradlew :composeApp:run

# Run server (port 8080)
./gradlew :server:run

# Run web client (Wasm - recommended)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Run web client (JS - legacy browser support)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Build Android APK
./gradlew :composeApp:assembleDebug

# iOS: Open iosApp/iosApp.xcodeproj in Xcode
```

## Testing

```bash
# Run all tests
./gradlew test

# Run module-specific tests
./gradlew :kPoker:test
./gradlew :server:test

# Run specific test class
./gradlew :kPoker:test --tests "com.aaronchancey.poker.kpoker.HandEvaluatorTest"

# Run single test method
./gradlew :kPoker:test --tests "com.aaronchancey.poker.kpoker.HandEvaluatorTest.testRoyalFlush"
```

**Card notation in tests:** Use shorthand like `"As"` (Ace of Spades), `"Kh"` (King of Hearts), `"10d"` (Ten of Diamonds) via `Card.fromString()`. Valid suits: `s, h, d, c`.

## Architecture

Four Gradle modules with strict separation of concerns:

### kPoker (`/kPoker`)
Core poker engine - **pure Kotlin Multiplatform with NO UI dependencies**. All game logic lives here.

Key packages:
- `betting/` - Actions, rounds, validation (`Action`, `BettingManager`, `BettingRound`)
- `core/` - Primitives (`Card`, `Deck`, `Rank`, `Suit`, `HandRank`)
- `evaluation/` - Hand evaluation (`HandEvaluator`, `StandardHandEvaluator`, `LoHandEvaluator`, `OmahaHandEvaluator`)
- `game/` - State and flow (`PokerGame`, `GameState`, `GamePhase`, `GameVariant`)
- `player/` - Player management (`Player`, `PlayerState`, `Table`, `Pot`, `PotManager`)
- `room/` - Multiplayer rooms (`Room`, `RoomConfig`, `SeatManager`, `SeatSelection`)
- `variants/` - Poker variants (`PokerVariant`, `TexasHoldem`, `Omaha`)

Design patterns:
- **Immutable state:** `GameState` uses copy-on-write; transitions create new instances
- **Event-driven:** `GameEvent` emissions for state changes
- **Strategy pattern:** `PokerVariant` interface for pluggable game rules

### composeApp (`/composeApp`)
UI layer using Compose Multiplatform with MVI architecture. Platform entry points in `androidMain/`, `iosMain/`, `jvmMain/`, `wasmJsMain/`.

- Uses Koin for DI (`koinViewModel<T>()`, modules in `di/AppModule.kt`)
- State exposed via `StateFlow`, intents via sealed interfaces
- Effects (toasts, navigation) via `Channel`/`Flow`

### server (`/server`)
Ktor server (Netty) with WebSocket support.

- `plugins/` - Ktor configuration (CORS, routing, serialization, WebSockets)
- `room/` - `RoomManager`, `ServerRoom`
- `routes/` - REST and WebSocket endpoints
- `persistence/` - Game state persistence for crash recovery

### shared (`/shared`)
Cross-cutting message models (`ClientMessage`, `ServerMessage`) shared between client and server.

## Key Conventions

1. **Module boundaries:** Keep game logic strictly in `kPoker`. Never import Compose/Android libraries there. UI code belongs in `composeApp`.

2. **State visibility:** Use `Room.getVisibleGameState(playerId)` to filter sensitive info (e.g., hide opponents' hole cards) before sending to clients.

3. **Dependencies:** All managed in `gradle/libs.versions.toml`. Add new deps there first, then reference in `build.gradle.kts`.

4. **Kotlin style:**
   - Prefer `callbackFlow`, `suspendCoroutine`, or `suspendCancellableCoroutine` over callback-based APIs
   - `Modifier` parameter is ALWAYS first in Composable function declarations and calls
   - Always use named parameters when putting parameters on separate lines
   - Avoid init blocks, especially on ViewModels (they make testing difficult)
   - Use KDoc for public APIs and non-trivial functions

5. **Testing:** Write unit tests for game logic in `kPoker/src/commonTest`. Use `kotlin.test` assertions.

## Deployment

Docker deployment to Google Cloud Run:
```bash
docker build -t gcr.io/kpoker-483701/poker-server .
docker push gcr.io/kpoker-483701/poker-server
gcloud run deploy poker-server --image gcr.io/kpoker-483701/poker-server --platform managed --allow-unauthenticated
```

Server runs on port 8080 (configurable via `PORT` env var). Game state persists to `/data/rooms/` for crash recovery.
