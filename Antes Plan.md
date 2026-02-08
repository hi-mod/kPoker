# Plan: Implement Ante Posting (Task 1.3)

## Context

`BlindLevel.ante`, `BettingStructure.ante`, `CashGameConfig.ante`, and `BlindType.ANTE` all exist but **nothing actually posts antes**. The `postBlinds()` method in `PokerGame.kt` only handles SB and BB. Tournament blind schedules already define ante amounts at higher levels — they're just silently ignored.

## Key Design Constraint: Antes Are Dead Money

Antes bypass `currentBet`/`totalBetThisRound` entirely. If we added antes to `currentBet`, the BB would show `currentBet = ante + bigBlind` while `BettingRound.currentBet = bigBlind`, breaking the betting logic. Antes go directly into the pot as dead money.

## Implementation

### 1. Add `addDeadMoney()` to PotManager
**File:** `kPoker/src/commonMain/.../player/Pot.kt`

Add a method that creates/adds to the main pot without going through `collectBets()`. Takes a total chip amount and the set of eligible player IDs.

### 2. Add `postAntes()` to PokerGame
**File:** `kPoker/src/commonMain/.../game/PokerGame.kt`

New `protected open fun postAntes()` method:
- Early return if `bettingStructure.ante <= 0.0`
- Iterate `seatsWithChips` (same eligibility filter as blinds/dealing)
- For each player: deduct `min(ante, chips)`, go ALL_IN if chips hit 0
- Does NOT set `currentBet` or `totalBetThisRound`
- Emit `GameEvent.BlindPosted` with `BlindType.ANTE` for each
- Add total antes to pot via `addDeadMoney()`

### 3. Wire into hand start flow
**File:** `kPoker/src/commonMain/.../game/PokerGame.kt` — `startHand()`

```
advanceDealer()
postAntes()    // ← NEW, before blinds
postBlinds()
dealHoleCards()
```

### 4. Tests
**File:** `kPoker/src/commonTest/.../game/AntePostingTest.kt` (NEW)

Key scenarios:
- Basic: 3 players post antes, verify chips deducted, pot has antes, `currentBet` is 0
- Ante + blinds: verify pot = antes + blinds, `currentBet` reflects only blind
- Partial ante all-in: player can't cover full ante, posts what they have
- All-in on ante then blind position: posts 0 for blind, stays all-in
- No ante configured (`ante = 0.0`): no-op, hand proceeds normally
- Sitting-out players excluded from ante posting

## Files Modified
| File | Change |
|------|--------|
| `kPoker/.../player/Pot.kt` | Add `addDeadMoney()` to `PotManager` |
| `kPoker/.../game/PokerGame.kt` | Add `postAntes()`, call it in `startHand()` |
| `kPoker/.../game/AntePostingTest.kt` | NEW — test suite |

## Human Contribution
Request human input on the `postAntes()` method — it's the core logic with design decisions around iteration order and event emission.

## Verification
1. `./gradlew :kPoker:jvmTest` — all existing + new tests pass
2. `./gradlew :kPoker:compileKotlinJvm` — compiles clean
