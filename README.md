# Poker

A full-stack, real-time multiplayer poker application built with **Kotlin Multiplatform** and **Compose Multiplatform**. Runs on Android, iOS, Web (Wasm/JS), and Desktop from a single codebase, backed by a Ktor WebSocket server.

## Features

- **3 Variants** — Texas Hold'em, Omaha, Omaha Hi-Lo (8-or-better)
- **Betting Structures** — No Limit, Pot Limit, Fixed Limit
- **Real-time Multiplayer** — WebSocket-based with instant state sync
- **Rake System** — Configurable percentage and cap, no-flop-no-drop
- **Antes & Blinds** — Fully configurable per table
- **Side Pots** — Automatic calculation for all-in scenarios
- **Sit Out** — Players can sit out and return between hands
- **Spectator Mode** — Watch games without a seat
- **Crash Recovery** — Server persists game state to disk
- **Cross-Platform** — Single Compose UI shared across Android, iOS, Web, and Desktop

## Quick Start

### Prerequisites

- JDK 21+
- Android SDK (for Android builds)
- Xcode (for iOS builds)

### Run the Server

```bash
./gradlew :server:run
```

The server starts on port 8080 (configurable via `PORT` env var).

### Run a Client

```bash
# Desktop (JVM)
./gradlew :composeApp:run

# Web (Wasm — recommended)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web (JS — legacy browser support)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Android APK
./gradlew :composeApp:assembleDebug

# iOS — open iosApp/iosApp.xcodeproj in Xcode
```

## Architecture

Four Gradle modules with strict separation of concerns:

```
poker/
├── kPoker/       # Pure Kotlin poker engine (no UI deps)
├── shared/       # Client ↔ Server message protocol
├── server/       # Ktor WebSocket server
└── composeApp/   # Compose Multiplatform UI
```

### kPoker

The core engine — pure Kotlin Multiplatform with zero UI dependencies. Handles game state, hand evaluation, betting logic, pot management, and variant rules.

- **Immutable state** — `GameState` uses copy-on-write; transitions create new instances
- **Event-driven** — `GameEvent` emissions for all state changes
- **Strategy pattern** — `PokerVariant` interface for pluggable game rules
- **4 hand evaluators** — Standard, Omaha, Lo, and Omaha Hi-Lo

### shared

Defines `ClientMessage` and `ServerMessage` sealed classes — the WebSocket protocol contract between client and server. Serialized with `kotlinx.serialization`.

### server

Ktor server (Netty) managing rooms, player connections, and game lifecycle. Uses `Mutex` for thread-safe room operations and broadcasts state updates via `ConnectionManager`. Persists game state to `/data/rooms/` for crash recovery.

### composeApp

Shared UI layer using Compose Multiplatform with platform-specific entry points. Follows **MVI** (Model-View-Intent) architecture with `ViewModel`, `Intent`, and `UiState` patterns. Connects to the server via Ktor WebSocket client.

**Platform targets:** Android, iOS (arm64 + simulator), Desktop (JVM), Web (Wasm), Web (JS)

## Testing

```bash
# All tests
./gradlew test

# Engine tests only
./gradlew :kPoker:test

# Specific test class
./gradlew :kPoker:test --tests "com.aaronchancey.poker.kpoker.HandEvaluatorTest"
```

## Deployment

Docker multi-stage build targeting Google Cloud Run:

```bash
docker build -t gcr.io/kpoker-483701/poker-server .
docker push gcr.io/kpoker-483701/poker-server
gcloud run deploy poker-server \
  --image gcr.io/kpoker-483701/poker-server \
  --platform managed \
  --allow-unauthenticated
```

The Docker image bundles both the Ktor server JAR and the Wasm/JS frontend static files, served together on port 8080.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3 (Multiplatform) |
| UI | Compose Multiplatform 1.10 |
| Server | Ktor 3.4 (Netty) |
| Networking | Ktor WebSockets + kotlinx.serialization |
| DI | Koin 4.2 |
| Navigation | Jetpack Navigation3 (Multiplatform) |
| Build | Gradle (Kotlin DSL) + Shadow plugin |
| Deploy | Docker → Google Cloud Run |

## License

[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) — Non-commercial use with attribution and share-alike.
