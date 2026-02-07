# Poker MVP Gap Analysis & Task Plan

## Context
The poker codebase has a solid core engine (~95%), working server (~75%), and partial client UI (~50%). The goal is to reach a **playable MVP** - a game that feels complete end-to-end for real players. Long-term target is real-money play, so architectural decisions should account for future security/fairness needs.

## Priority 1: Core Gameplay Gaps (Must-have for MVP)

### 1.1 Implement Sit-Out Logic & Pre-Action Checkboxes
**Sit-Out:**
- **Why:** Players need to step away without leaving the table
- **Where:** `kPoker/src/commonMain/.../player/PlayerState.kt`, `kPoker/src/commonMain/.../game/PokerGame.kt`, client UI
- **What:** Add `SITTING_OUT` player status, skip sitting-out players in dealing/betting, auto-post blinds or miss-blind tracking, client UI toggle button
- **Tests:** Unit tests for sit-out during hand, blind skip, return-to-play

**Pre-Action Checkboxes (when it's NOT your turn):**
- **Why:** Essential UX for real poker - players decide actions in advance to speed up play
- **Where:** Client-side only (composeApp presentation layer) - these are local UI state, not server messages
- **Checkboxes:**
  - `Check/Fold` - auto-check if possible, otherwise fold
  - `Check Any` - auto-check (unchecks if a bet comes in)
  - `Call` - auto-call the current bet amount
  - `Call Any` - auto-call regardless of bet size
  - `Raise` - auto-raise (to a pre-set amount or min raise)
  - `Raise Any` - auto-raise regardless
  - `All-In` / `Pot` - auto all-in or pot-sized bet
- **Behavior:**
  - Show checkboxes only when it's NOT your turn and you're still in the hand
  - When it becomes your turn, if a checkbox is selected AND still valid given current action, auto-submit that action
  - If the game state changed (e.g., someone raised and you had "Call" checked for a smaller amount), invalidate and show normal action buttons
  - Clear all checkboxes after action is taken or hand ends
- **Where stored:** Client ViewModel state only - never sent to server until it's your turn
- **Tests:** Unit tests on ViewModel for checkbox state management and auto-action resolution

### 1.2 Apply Rake Calculation
- **Why:** Currently stubbed with `TODO` in `CashGame.kt` ~line 137
- **Where:** `kPoker/src/commonMain/.../game/CashGame.kt`
- **What:** Apply rake to pot winners, configurable rake percentage and cap, show rake in UI
- **Tests:** Unit tests for rake deduction, cap enforcement, split pot rake

### 1.3 Implement Ante Posting
- **Why:** `BlindLevel` has `ante` field but antes are never actually collected
- **Where:** `kPoker/src/commonMain/.../betting/BettingManager.kt`, game flow
- **What:** Post antes from all players before dealing, include in pot
- **Tests:** Unit tests for ante collection, broke players, ante + blinds interaction

### 1.4 Rebuy/Top-Up UI
- **Why:** Engine supports rebuy but client has no way to trigger it
- **Where:** `composeApp/src/commonMain/.../presentation/game/`
- **What:** Add rebuy button when below max buy-in, confirmation dialog, wire to server message
- **Tests:** Manual verification; unit test for rebuy message handling

## Priority 2: Server Reliability (Required for multiplayer)

### 2.1 Player Reconnection
- **Why:** If a WebSocket drops, the player loses their seat - unacceptable for real play
- **Where:** `server/src/main/kotlin/.../room/ServerRoom.kt`, `ConnectionManager.kt`
- **What:** Session tokens, reconnect window (e.g., 60s), restore player state on reconnect, timeout -> auto-fold/sit-out
- **Shared messages:** Add `Reconnect` client message, `ReconnectResult` server message
- **Tests:** Integration tests for disconnect/reconnect scenarios

### 2.2 Action Timeout
- **Why:** Players can stall the game indefinitely - no time bank or auto-action
- **Where:** Server-side timer in `ServerRoom.kt`, client-side countdown UI
- **What:** Configurable action timeout (e.g., 30s), auto-fold on timeout, time bank for tournaments
- **Tests:** Unit tests for timeout triggering auto-fold

### 2.3 Chat Implementation
- **Why:** `SendChat` message type exists but isn't routed - half-built feature
- **Where:** `server/src/main/kotlin/.../routes/`, `composeApp/` UI
- **What:** Route chat messages through server, broadcast to room, display in client UI
- **Tests:** Manual verification

## Priority 3: Client UX Polish

### 3.1 Settings Screen
- **Why:** No way to toggle sounds, adjust preferences
- **Where:** `composeApp/src/commonMain/.../presentation/`
- **What:** Sound toggle, animation toggle, display name, server URL config

### 3.2 Hand History
- **Why:** Players need to review past hands - essential for learning/dispute resolution, and critical for provably-fair verification later
- **Where:** New `kPoker` logging system + client viewer
- **What:** Record actions per hand, store locally, display in scrollable history
- **Architecture note:** Design the hand history format to accommodate future cryptographic proofs

### 3.3 Pot Display & Side Pot UI
- **Why:** Players need to see pot amounts and understand side pots
- **Where:** `composeApp/` game screen
- **What:** Main pot + side pot labels with amounts, highlight eligible players

## Priority 4: Testing & Stability

### 4.1 Server Test Suite
- **Why:** Zero server tests - `ServerRoom`, `RoomManager`, `ConnectionManager` are all untested
- **Where:** `server/src/test/`
- **What:** Unit tests for room lifecycle, player join/leave, game state broadcasting, persistence load/save

### 4.2 Tournament Tests
- **Why:** Tournament logic exists but is completely untested
- **Where:** `kPoker/src/commonTest/`
- **What:** Tests for registration, blind level progression, elimination, payouts

### 4.3 Integration Tests
- **Why:** No end-to-end test that a full hand can be played through the server
- **Where:** `server/src/test/`
- **What:** WebSocket client test that joins room, takes seat, plays a hand

## Priority 5: Future Architecture (Real-Money Prep)

### 5.1 Authentication System
- **Why:** Currently players are just a name string - no identity verification
- **Where:** Server middleware, shared auth models
- **What:** JWT-based auth, user registration/login, session management
- **Note:** Block on this before any real-money features

### 5.2 Provably Fair Deck (Mental Poker Protocol)
- **Why:** Core architectural goal per CLAUDE.md - zero progress so far
- **Where:** `kPoker/` new crypto package
- **What:** `EncryptedDeck`/`EncryptedCard` types, SRA commutative encryption, key exchange protocol, verification system
- **Dependency:** Hand history (3.2) should be designed with this in mind

### 5.3 Rate Limiting & Anti-Abuse
- **Why:** No protection against spam, rapid join/leave, or action flooding
- **Where:** Server middleware
- **What:** Per-connection rate limits, action throttling, IP-based limits

## Verification Plan
After implementing each priority group:
1. **P1:** Run `./gradlew :kPoker:test` - all existing + new tests pass
2. **P2:** Manual test: connect two clients, disconnect one, reconnect, verify state restored
3. **P3:** Manual test: play full hand, check settings persist, review hand history
4. **P4:** Run `./gradlew test` - full test suite passes including new server tests
5. **P5:** Design review before implementation (crypto protocol needs careful design)

## Suggested Implementation Order
Start with **1.1 (Sit-Out)** -> **1.2 (Rake)** -> **2.1 (Reconnection)** -> **2.2 (Action Timeout)** -> **1.4 (Rebuy UI)** -> **2.3 (Chat)** -> **4.1 (Server Tests)** -> remaining items

## First Task: 1.1 Sit-Out Logic & Pre-Action Checkboxes
Begin implementation with:
1. Sit-out logic in the engine + server + client UI toggle
2. Pre-action checkboxes in the client (Check/Fold, Check Any, Call, Call Any, Raise, Raise Any, All-In/Pot)
