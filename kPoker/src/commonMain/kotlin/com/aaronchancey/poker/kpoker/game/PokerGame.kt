package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.BettingManager
import com.aaronchancey.poker.kpoker.betting.BettingRound
import com.aaronchancey.poker.kpoker.betting.BettingRoundType
import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.betting.BlindType
import com.aaronchancey.poker.kpoker.core.Deck
import com.aaronchancey.poker.kpoker.dealing.CardDealer
import com.aaronchancey.poker.kpoker.dealing.StandardDealer
import com.aaronchancey.poker.kpoker.evaluation.HandEvaluator
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.PotManager
import com.aaronchancey.poker.kpoker.player.Table

abstract class PokerGame(
    protected val bettingStructure: BettingStructure,
    protected val handEvaluator: HandEvaluator,
    protected val cardDealer: CardDealer = StandardDealer(),
) {
    abstract val gameVariant: GameVariant

    protected var state: GameState = GameState(Table.create("1", "Default", 9))

    // Called after subclass init (careful with abstract props in init)
    // But we can't easily init state with abstract prop in var declaration.
    // Instead, we trust initialize() or we use an `init` block that might run before subclass prop is set?
    // No, init blocks run before subclass fields.
    // So we can't use gameVariant in the initial assignment if it's abstract.
    // However, `initialize` is the standard way to set up the game.
    // The default value above will use the default variant (Texas Holdem).
    // We can rely on `initialize` to overwrite it.

    protected val bettingManager = BettingManager(bettingStructure)
    protected val eventListeners = mutableListOf<(GameEvent) -> Unit>()

    val currentState: GameState get() = state

    fun addEventListener(listener: (GameEvent) -> Unit) {
        eventListeners.add(listener)
    }

    protected fun emit(event: GameEvent) {
        eventListeners.forEach { it(event) }
    }

    // Abstract methods for variant-specific behavior
    abstract val variantName: String
    abstract val holeCardCount: Int
    abstract val usesCommunityCards: Boolean

    abstract fun evaluateHands(): List<Winner>

    /**
     * Deal hole cards to all active players using the [CardDealer].
     * Override in subclasses only if variant needs custom dealing behavior.
     */
    open fun dealHoleCards() {
        state = state.withPhase(GamePhase.DEALING)

        val result = cardDealer.dealHoleCards(state, holeCardCount)
        state = result.updatedState

        // Emit events for each player's dealt cards
        result.dealtCards.forEach { (playerId, cards) ->
            emit(GameEvent.HoleCardsDealt(playerId, cards))
        }
    }

    // Common game flow
    open fun initialize(table: Table) {
        state = GameState(table = table, variant = gameVariant)
        emit(GameEvent.GameInitialized(state))
    }

    open fun restoreState(gameState: GameState) {
        // Ensure the restored state has the correct variant for this Game instance
        // This fixes issues where state is deserialized with default variant (Texas Hold'em)
        state = gameState.copy(variant = gameVariant)
        emit(GameEvent.GameInitialized(state))
    }

    open fun updateTable(table: Table) {
        state = state.withTable(table)
    }

    open fun startHand(): GameState {
        require(state.table.playerCount >= 2) { "Need at least 2 players" }
        require(!state.isHandInProgress) { "Hand already in progress" }

        state = state.copy(
            phase = GamePhase.STARTING,
            deck = Deck.standard(),
            communityCards = emptyList(),
            potManager = PotManager(),
            winners = emptyList(),
            handNumber = state.handNumber + 1,
        )

        advanceDealer()
        postBlinds()
        dealHoleCards()

        state = state.withPhase(GamePhase.PRE_FLOP)
        startBettingRound(BettingRoundType.PRE_FLOP)

        emit(GameEvent.HandStarted(state.handNumber, state.dealerSeatNumber))
        return state
    }

    protected open fun advanceDealer() {
        val occupiedSeats = state.table.occupiedSeats.map { it.number }.sorted()
        val currentIndex = occupiedSeats.indexOf(state.dealerSeatNumber)
        val nextIndex = (currentIndex + 1) % occupiedSeats.size
        val newDealerSeat = occupiedSeats[nextIndex]

        state = state.copy(dealerSeatNumber = newDealerSeat)

        // Update player states
        var table = state.table
        for (seat in state.table.seats) {
            if (seat.playerState != null) {
                table = table.updateSeat(seat.number) { s ->
                    s.updatePlayerState { ps ->
                        ps.copy(isDealer = s.number == newDealerSeat)
                    }
                }
            }
        }
        state = state.withTable(table)
    }

    protected open fun postBlinds() {
        val occupiedSeats = state.table.occupiedSeats.map { it.number }.sorted()
        val dealerIndex = occupiedSeats.indexOf(state.dealerSeatNumber)

        val (smallBlindIndex, bigBlindIndex) = if (occupiedSeats.size == 2) {
            // In Heads-Up, Dealer is SB, other player is BB
            dealerIndex to (dealerIndex + 1) % occupiedSeats.size
        } else {
            // Multi-player: SB is dealer + 1, BB is dealer + 2
            (dealerIndex + 1) % occupiedSeats.size to (dealerIndex + 2) % occupiedSeats.size
        }

        val smallBlindSeat = occupiedSeats[smallBlindIndex]
        val bigBlindSeat = occupiedSeats[bigBlindIndex]

        // Post small blind
        postBlind(smallBlindSeat, bettingStructure.smallBlind, BlindType.SMALL_BLIND)
        // Post big blind
        postBlind(bigBlindSeat, bettingStructure.bigBlind, BlindType.BIG_BLIND)

        state = state.withPhase(GamePhase.POSTING_BLINDS)
    }

    protected fun postBlind(seatNumber: Int, amount: ChipAmount, blindType: BlindType) {
        val seat = state.table.getSeat(seatNumber) ?: return
        val playerState = seat.playerState ?: return

        val actualAmount = minOf(amount, playerState.chips)
        val action = Action.PostBlind(playerState.player.id, actualAmount, blindType)

        val table = state.table.updateSeat(seatNumber) { s ->
            s.updatePlayerState { ps ->
                ps.copy(
                    chips = ps.chips - actualAmount,
                    currentBet = actualAmount,
                    totalBetThisRound = actualAmount, // Track blind as part of round bet
                    isSmallBlind = blindType == BlindType.SMALL_BLIND,
                    isBigBlind = blindType == BlindType.BIG_BLIND,
                    status = if (ps.chips - actualAmount == 0.0) PlayerStatus.ALL_IN else PlayerStatus.ACTIVE,
                )
            }
        }

        // Blind stays with player until end of round
        state = state.withTable(table).withLastAction(action)
        emit(GameEvent.BlindPosted(playerState.player.id, actualAmount, blindType))
    }

    protected fun startBettingRound(type: BettingRoundType) {
        val round = BettingRound(
            type = type,
            currentBet = if (type == BettingRoundType.PRE_FLOP) bettingStructure.bigBlind else 0.0,
            minimumRaise = bettingStructure.bigBlind,
            lastRaiseAmount = bettingStructure.bigBlind,
        )

        state = state.withBettingRound(round)
        setNextActor()
    }

    protected fun setNextActor() {
        val playersInHandCount = state.table.getPlayersInHand().size
        if (playersInHandCount <= 1) {
            endBettingRound()
            return
        }

        val playersInHand = state.table.occupiedSeats
            .filter { it.playerState?.status == PlayerStatus.ACTIVE }
            .map { it.number }
            .sorted()

        if (playersInHand.isEmpty()) {
            state = state.withCurrentActor(null)
            return
        }

        val currentSeat = state.currentActorSeatNumber ?: if (state.phase == GamePhase.PRE_FLOP) {
            state.table.occupiedSeats.find { it.playerState?.isBigBlind == true }?.number ?: state.dealerSeatNumber
        } else {
            state.dealerSeatNumber
        }
        val currentIndex = playersInHand.indexOf(currentSeat)

        // Find next player who hasn't acted or needs to respond to a raise
        for (i in 1..playersInHand.size) {
            val nextIndex = (currentIndex + i) % playersInHand.size
            val nextSeat = playersInHand[nextIndex]
            val playerState = state.table.getSeat(nextSeat)?.playerState

            if (playerState != null && playerState.status == PlayerStatus.ACTIVE && !playerState.hasActed) {
                state = state.withCurrentActor(nextSeat)
                emit(GameEvent.TurnChanged(playerState.player.id))
                return
            }
        }

        // All players have acted, end betting round
        endBettingRound()
    }

    open fun processAction(action: Action): GameState {
        val playerState = state.table.seats
            .mapNotNull { it.playerState }
            .find { it.player.id == action.playerId }
            ?: throw IllegalArgumentException("Player not found")

        require(state.currentActor?.player?.id == action.playerId) {
            "Not ${playerState.player.name}'s turn"
        }

        val round = state.bettingRound
            ?: throw IllegalStateException("No betting round in progress")

        require(bettingManager.validateAction(action, playerState, round.currentBet, round.minimumRaise)) {
            "Invalid action: $action. State: Bet=${round.currentBet}, MinRaise=${round.minimumRaise}, PlayerBet=${playerState.currentBet}, Chips=${playerState.chips}"
        }

        state = applyAction(action, playerState)
        emit(GameEvent.ActionTaken(action))

        setNextActor()
        return state
    }

    protected fun applyAction(action: Action, playerState: PlayerState): GameState {
        var table = state.table
        var round = state.bettingRound!!

        when (action) {
            is Action.Fold -> {
                table = table.updatePlayerState(action.playerId) {
                    it.withStatus(PlayerStatus.FOLDED).markActed()
                }
            }

            is Action.Check -> {
                table = table.updatePlayerState(action.playerId) { it.markActed() }
            }

            is Action.Call -> {
                table = table.updatePlayerState(action.playerId) {
                    it.copy(
                        chips = it.chips - action.amount,
                        currentBet = it.currentBet + action.amount,
                        totalBetThisRound = it.totalBetThisRound + action.amount,
                        hasActed = true,
                    )
                }
            }

            is Action.Bet -> {
                table = table.updatePlayerState(action.playerId) {
                    it.copy(
                        chips = it.chips - action.amount,
                        currentBet = action.amount,
                        totalBetThisRound = it.totalBetThisRound + action.amount,
                        hasActed = true,
                    )
                }
                round = round.copy(
                    currentBet = action.amount,
                    lastRaiseAmount = action.amount,
                    minimumRaise = action.amount,
                    lastAggressorId = action.playerId,
                )
                // Reset hasActed for other players
                table = resetOtherPlayersActed(table, action.playerId)
            }

            is Action.Raise -> {
                table = table.updatePlayerState(action.playerId) {
                    it.copy(
                        chips = it.chips - action.amount,
                        currentBet = action.totalBet,
                        totalBetThisRound = it.totalBetThisRound + action.amount,
                        hasActed = true,
                    )
                }
                val raiseAmount = action.totalBet - round.currentBet
                round = round.copy(
                    currentBet = action.totalBet,
                    lastRaiseAmount = raiseAmount,
                    minimumRaise = raiseAmount,
                    lastAggressorId = action.playerId,
                )
                table = resetOtherPlayersActed(table, action.playerId)
            }

            is Action.AllIn -> {
                table = table.updatePlayerState(action.playerId) {
                    it.copy(
                        chips = 0.0,
                        currentBet = it.currentBet + action.amount,
                        totalBetThisRound = it.totalBetThisRound + action.amount,
                        status = PlayerStatus.ALL_IN,
                        hasActed = true,
                    )
                }
                val newBet = playerState.currentBet + action.amount
                if (newBet > round.currentBet) {
                    round = round.copy(
                        currentBet = newBet,
                        lastRaiseAmount = newBet - round.currentBet,
                        lastAggressorId = action.playerId,
                    )
                    table = resetOtherPlayersActed(table, action.playerId)
                }
            }

            is Action.PostBlind -> {
                // Already handled in postBlind()
            }
        }

        val newRound = round.copy(actions = round.actions + action)
        return state.withTable(table).withBettingRound(newRound).withLastAction(action)
    }

    protected fun resetOtherPlayersActed(table: Table, exceptPlayerId: PlayerId): Table {
        var result = table
        for (seat in table.occupiedSeats) {
            if (seat.playerState?.player?.id != exceptPlayerId &&
                seat.playerState?.status == PlayerStatus.ACTIVE
            ) {
                result = result.updateSeat(seat.number) { s ->
                    s.updatePlayerState { it.copy(hasActed = false) }
                }
            }
        }
        return result
    }

    protected open fun endBettingRound() {
        // Collect bets into pot
        val playerBets = state.table.occupiedSeats
            .mapNotNull { it.playerState }
            .filter { it.totalBetThisRound > 0 }
            .associate { it.player.id to it.totalBetThisRound }

        val newPotManager = state.potManager.collectBets(playerBets)

        // Reset player bets for next round
        var table = state.table
        for (seat in table.occupiedSeats) {
            table = table.updateSeat(seat.number) { s ->
                s.updatePlayerState { it.resetForNewRound() }
            }
        }

        state = state.withTable(table)
            .withPotManager(newPotManager)
            .withBettingRound(null)
            .withCurrentActor(null)

        // Check if hand should continue
        val playersRemaining = state.table.getPlayersInHand()
        if (playersRemaining.size <= 1) {
            finishHand()
            return
        }

        // Advance to next phase
        advancePhase()
    }

    protected open fun advancePhase() {
        when (state.phase) {
            GamePhase.PRE_FLOP -> {
                if (usesCommunityCards) {
                    dealFlop()
                    state = state.withPhase(GamePhase.FLOP)
                    startBettingRound(BettingRoundType.FLOP)
                } else {
                    finishHand()
                }
            }

            GamePhase.FLOP -> {
                dealTurn()
                state = state.withPhase(GamePhase.TURN)
                startBettingRound(BettingRoundType.TURN)
            }

            GamePhase.TURN -> {
                dealRiver()
                state = state.withPhase(GamePhase.RIVER)
                startBettingRound(BettingRoundType.RIVER)
            }

            GamePhase.RIVER -> {
                finishHand()
            }

            else -> {}
        }
        emit(GameEvent.PhaseChanged(state.phase))
    }

    protected open fun dealFlop() {
        val result = cardDealer.dealCommunityCards(state, count = 3, burnFirst = true)
        state = result.updatedState
        emit(GameEvent.CommunityCardsDealt(result.cards))
    }

    protected open fun dealTurn() {
        val result = cardDealer.dealCommunityCards(state, count = 1, burnFirst = true)
        state = result.updatedState
        emit(GameEvent.CommunityCardsDealt(result.cards))
    }

    protected open fun dealRiver() {
        val result = cardDealer.dealCommunityCards(state, count = 1, burnFirst = true)
        state = result.updatedState
        emit(GameEvent.CommunityCardsDealt(result.cards))
    }

    protected open fun finishHand() {
        state = state.withPhase(GamePhase.SHOWDOWN)

        val winners = evaluateHands()
        state = state.withWinners(winners)

        // Award pots to winners
        var table = state.table
        for (winner in winners) {
            table = table.updatePlayerState(winner.playerId) {
                it.withChips(it.chips + winner.amount)
            }
        }

        state = state.withTable(table).withPhase(GamePhase.HAND_COMPLETE)
        emit(GameEvent.HandComplete(winners))
    }

    fun getActionRequest(): ActionRequest? {
        val actor = state.currentActor ?: return null
        val round = state.bettingRound ?: return null

        return bettingManager.getValidActions(
            playerState = actor,
            currentBet = round.currentBet,
            potSize = state.totalPot,
            minRaise = round.minimumRaise,
        )
    }
}
