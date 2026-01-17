# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack Kotlin Multiplatform poker application targeting Android, iOS, Web (JS/Wasm), Desktop (JVM), and Server. Uses **Compose Multiplatform** for shared UI and **Ktor** for backend server with WebSocket support for real-time multiplayer.

## Provably Fair Deck (Future Goal)

A core architectural goal is implementing **end-to-end card encryption** with a **provably fair deck** system. This ensures trustless gameplay where:

1. **No party sees cards prematurely** - Cards remain encrypted until legitimately revealed (deal, showdown)
2. **Server cannot cheat** - The server never has access to unencrypted card values
3. **Players can verify fairness** - Cryptographic proofs allow post-game verification that the deck was shuffled honestly
4. **Collusion-resistant** - Even colluding players cannot gain information about unrevealed cards

### Cryptographic Approach

The system will use **Mental Poker** protocols (e.g., SRA or similar commutative encryption schemes):

- Each player contributes to deck encryption with their own key
- Cards can only be decrypted when all required parties cooperate
- Shuffle verification via zero-knowledge proofs or commit-reveal schemes
- End-of-hand key revelation for full audit trail

### Design Considerations

- Encryption logic should live in `kPoker/` as platform-agnostic Kotlin
- Consider using Kotlin Multiplatform crypto libraries (e.g., `kotlinx-crypto` or bindings to libsodium)
- `EncryptedDeck` and `EncryptedCard` types to distinguish from plaintext `Deck`/`Card`
- Protocol messages in `shared/` for key exchange and encrypted card transfers
- Graceful degradation: support both trusted-server mode (current) and provably-fair mode

## Build & Run Commands

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
```

**Card notation in tests:** Use shorthand like `"As"` (Ace of Spades), `"Kh"` (King of Hearts), `"10d"` (Ten of Diamonds) via `Card.fromString()`. Valid suits: `s, h, d, c`.

## Architecture

Four Gradle modules with strict separation of concerns:

### kPoker (`/kPoker`)
Core poker engine - **pure Kotlin Multiplatform with NO UI dependencies**.

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
UI layer using Compose Multiplatform. Platform entry points in `androidMain/`, `iosMain/`, `jvmMain/`, `wasmJsMain/`.

Key directories:
- `network/` - Ktor client, WebSocket client, repository pattern
- `presentation/game/` - Game UI, ViewModel, intents, effects
- `presentation/room/` - Room selection UI

### server (`/server`)
Ktor server (Netty) with WebSocket support.

Key directories:
- `plugins/` - Ktor configuration (CORS, routing, serialization, WebSockets)
- `room/` - `RoomManager`, `ServerRoom`
- `routes/` - REST and WebSocket endpoints
- `persistence/` - Game state persistence for crash recovery

### shared (`/shared`)
Cross-cutting utilities and message models (`ClientMessage`, `ServerMessage`) shared between client and server.

## Development Conventions

1. **Code separation:** Keep game logic strictly in `kPoker`. Never import Compose/Android libraries there.

2. **State visibility:** Use `Room.getVisibleGameState(playerId)` to filter sensitive info (e.g., hide opponents' hole cards) before sending to clients.

3. **Dependencies:** All managed in `gradle/libs.versions.toml`. Add new deps there first, then reference in `build.gradle.kts`.

4. **Testing:** Write unit tests for game logic in `kPoker/src/commonTest`. Use `kotlin.test` assertions.

## Deployment

Docker deployment to Google Cloud Run:
```bash
docker build -t gcr.io/kpoker-483701/poker-server .
docker push gcr.io/kpoker-483701/poker-server
gcloud run deploy poker-server --image gcr.io/kpoker-483701/poker-server --platform managed --allow-unauthenticated
```

Server runs on port 8080 (configurable via `PORT` env var). Game state persists to `/data/rooms/` for crash recovery.

## Language & Documentation

- **Primary language:** Kotlin
- **Documentation:** Always use KDoc for public APIs and non-trivial functions
- **Testing:** Always write unit tests for new functionality

## Kotlin Conventions

- Prefer `callbackFlow`, `suspendCoroutine`, or `suspendCancellableCoroutine` over callback-based APIs
- Convert callback patterns to coroutine-friendly flows when possible
- `Modifier` parameter is ALWAYS first in Composable function declarations and calls
- Always use named parameters when putting parameters on separate lines.
- Generally try to avoid using init blocks
- Definitely avoid init blocks on view models as they make them very hard to test
