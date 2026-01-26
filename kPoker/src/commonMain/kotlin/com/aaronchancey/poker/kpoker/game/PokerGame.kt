package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.BettingManager
import com.aaronchancey.poker.kpoker.betting.BettingRound
import com.aaronchancey.poker.kpoker.betting.BettingRoundType
import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.betting.BlindType
import com.aaronchancey.poker.kpoker.betting.ShowdownActionType
import com.aaronchancey.poker.kpoker.betting.ShowdownRequest
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
import com.aaronchancey.poker.kpoker.player.ShowdownStatus
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

        // Reset all player states for the new hand (clears showdownStatus, hole cards, etc.)
        state = state.withTable(state.table.mapPlayerStates { it.resetForNewHand() })

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

    /**
     * Validates that the given player exists and is the current actor.
     * @throws IllegalArgumentException if player not found
     * @throws IllegalStateException if not player's turn
     */
    private fun requireCurrentActor(playerId: PlayerId): PlayerState {
        val playerState = state.table.seats
            .mapNotNull { it.playerState }
            .find { it.player.id == playerId }
            ?: throw IllegalArgumentException("Player not found")

        require(state.currentActor?.player?.id == playerId) {
            "Not ${playerState.player.name}'s turn"
        }

        return playerState
    }

    open fun processAction(action: Action): GameState {
        val playerState = requireCurrentActor(action.playerId)

        val round = state.bettingRound
            ?: throw IllegalStateException("No betting round in progress")

        require(bettingManager.validateAction(action, playerState, round.currentBet, state.effectivePot, round.minimumRaise)) {
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
                    // Calculate full amount: call + raise
                    val amountToAdd = action.totalBet - it.currentBet
                    it.copy(
                        chips = it.chips - amountToAdd,
                        currentBet = action.totalBet,
                        totalBetThisRound = it.totalBetThisRound + amountToAdd,
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

            is Action.Show, is Action.Muck, is Action.Collect -> {
                // Showdown actions are handled by processShowdownAction, not applyAction
                throw IllegalStateException("Showdown actions must use processShowdownAction")
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

        // Preserve last aggressor for showdown ordering (if this is the final round)
        val lastAggressor = state.bettingRound?.lastAggressorId

        // Reset player bets for next round
        state = state.withTable(state.table.mapPlayerStates { it.resetForNewRound() })
            .withPotManager(newPotManager)
            .withBettingRound(null)
            .withCurrentActor(null)
            .withShowdownAggressor(lastAggressor)

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

    /**
     * Deals community cards, burns one card first, and emits the dealt cards' event.
     */
    private fun dealCommunityCards(count: Int) {
        val result = cardDealer.dealCommunityCards(state, count = count, burnFirst = true)
        state = result.updatedState
        emit(GameEvent.CommunityCardsDealt(result.cards))
    }

    protected open fun dealFlop() = dealCommunityCards(count = 3)

    protected open fun dealTurn() = dealCommunityCards(count = 1)

    protected open fun dealRiver() = dealCommunityCards(count = 1)

    protected open fun finishHand() {
        val playersInHand = state.table.getPlayersInHand()

        // If only one player remains (everyone else folded), they win without showing
        if (playersInHand.size <= 1) {
            awardPotToLastPlayer()
            return
        }

        // Multiple players - enter showdown reveal phase
        state = state.withPhase(GamePhase.SHOWDOWN)

        // Initialize all players in hand with PENDING showdown status
        var table = state.table
        for (player in playersInHand) {
            table = table.updatePlayerState(player.player.id) {
                it.withShowdownStatus(ShowdownStatus.PENDING)
            }
        }
        state = state.withTable(table)

        // Determine first player to show and set them as actor
        val firstShowdownSeat = getFirstShowdownActorSeat()
        state = state.withCurrentActor(firstShowdownSeat)

        val actor = state.currentActor
        if (actor != null) {
            emit(GameEvent.TurnChanged(actor.player.id))
        }
    }

    /**
     * Award pot when only one player remains (all others folded).
     * No showdown needed - winner doesn't have to show cards.
     */
    private fun awardPotToLastPlayer() {
        state = state.withPhase(GamePhase.SHOWDOWN)
        state.table.getPlayersInHand().firstOrNull()?.let { lastPlayer ->
            awardPotToWinner(lastPlayer, "Last player standing")
        }
    }

    /**
     * Determines the seat number of the first player who must act in showdown.
     *
     * Poker showdown order rules:
     * - If there was a bet/raise on the river: last aggressor shows first (they must show)
     * - If river was checked down: first active player left of dealer shows first
     */
    protected open fun getFirstShowdownActorSeat(): Int {
        val playersInHand = state.table.occupiedSeats
            .filter { it.playerState?.status in listOf(PlayerStatus.ACTIVE, PlayerStatus.ALL_IN) }
            .sortedBy { it.number }

        if (state.showdownAggressorId != null) {
            return playersInHand
                .firstOrNull { it.playerState?.player?.id == state.showdownAggressorId }
                ?.number
                ?: playersInHand.first().number
        }

        val dealerIndex = playersInHand.indexOfFirst { it.number == state.dealerSeatNumber }
        val firstToActIndex = (dealerIndex + 1) % playersInHand.size
        return playersInHand[firstToActIndex].number
    }

    /**
     * Returns seats with players who haven't yet acted in showdown (PENDING status).
     */
    private fun getPendingShowdownSeats() = state.table.occupiedSeats
        .filter { it.playerState?.showdownStatus == ShowdownStatus.PENDING }

    /**
     * Returns seats with players who have shown their cards in showdown.
     */
    private fun getShownShowdownSeats() = state.table.occupiedSeats
        .filter { it.playerState?.showdownStatus == ShowdownStatus.SHOWN }

    /**
     * Returns true if exactly one player remains pending and no one has shown.
     * This player has won without needing to reveal cards.
     */
    private fun isLastPlayerStanding(): Boolean = getPendingShowdownSeats().size == 1 && getShownShowdownSeats().isEmpty()

    /**
     * Advances the turn to the next showdown actor and emits TurnChanged event.
     */
    private fun advanceToNextShowdownActor() {
        val nextSeat = getNextShowdownActorSeat()
        state = state.withCurrentActor(nextSeat)
        nextSeat?.let { seat ->
            state.table.getSeat(seat)?.playerState?.let { actor ->
                emit(GameEvent.TurnChanged(actor.player.id))
            }
        }
    }

    /**
     * Awards the entire pot to a single winner and completes the hand.
     */
    private fun awardPotToWinner(winner: PlayerState, handDescription: String) {
        val amount = state.totalPot
        val winners = listOf(
            Winner(
                playerId = winner.player.id,
                amount = amount,
                handDescription = handDescription,
            ),
        )
        state = state.withWinners(winners)

        val table = state.table.updatePlayerState(winner.player.id) {
            it.withChips(it.chips + amount)
        }
        state = state.withTable(table).withPhase(GamePhase.HAND_COMPLETE)
        emit(GameEvent.HandComplete(winners))
    }

    /**
     * Determines the next showdown actor seat after the current one.
     * Cycles clockwise through players who still have PENDING showdown status.
     */
    protected fun getNextShowdownActorSeat(): Int? {
        val pendingPlayers = getPendingShowdownSeats()
            .map { it.number }
            .sorted()

        if (pendingPlayers.isEmpty()) return null

        val currentSeat = state.currentActorSeatNumber ?: return pendingPlayers.first()
        val currentIndex = pendingPlayers.indexOf(currentSeat)

        // Find next player clockwise who hasn't acted
        for (i in 1..pendingPlayers.size) {
            val nextIndex = (currentIndex + i) % pendingPlayers.size
            val candidate = pendingPlayers[nextIndex]
            if (candidate != currentSeat) {
                return candidate
            }
        }

        return null
    }

    /**
     * Process a showdown action (Show, Muck, or Collect).
     */
    open fun processShowdownAction(action: Action): GameState {
        require(state.phase == GamePhase.SHOWDOWN) { "Not in showdown phase" }
        require(action is Action.Show || action is Action.Muck || action is Action.Collect) {
            "Invalid showdown action"
        }

        val playerState = requireCurrentActor(action.playerId)

        require(playerState.showdownStatus == ShowdownStatus.PENDING) {
            "Player has already acted in showdown"
        }

        // Check if this player is the last one standing (all others mucked)
        val isLastStanding = isLastPlayerStanding()

        // Validate action is allowed
        when (action) {
            is Action.Muck -> {
                val mustShow = state.showdownAggressorId == action.playerId
                require(!mustShow) { "Last aggressor must show cards when called" }
                require(!isLastStanding) { "Cannot muck when you've already won - use Show or Collect" }
            }

            is Action.Collect -> {
                require(isLastStanding) { "Can only collect when all others have mucked" }
            }

            else -> { /* Show is always valid */ }
        }

        // Handle Collect immediately - award pot without revealing
        if (action is Action.Collect) {
            state = state.withLastAction(action)
            emit(GameEvent.ActionTaken(action))
            awardPotToLastShowdownPlayer(playerState)
            return state
        }

        // Apply Show or Muck (Collect was handled above with early return)
        val newStatus = when (action) {
            is Action.Show -> ShowdownStatus.SHOWN
            is Action.Muck -> ShowdownStatus.MUCKED
        }

        val table = state.table.updatePlayerState(action.playerId) {
            it.withShowdownStatus(newStatus)
        }
        state = state.withTable(table).withLastAction(action)
        emit(GameEvent.ActionTaken(action))

        // Re-check after applying action
        if (getPendingShowdownSeats().isEmpty()) {
            // All players have acted - evaluate and complete
            completeShowdown()
        } else {
            // Move to next player (or give last standing player choice to show/collect)
            advanceToNextShowdownActor()
        }

        return state
    }

    /**
     * Awards pot to last remaining player in showdown when all others mucked.
     * Player wins without being required to show cards.
     */
    private fun awardPotToLastShowdownPlayer(winner: PlayerState) = awardPotToWinner(winner, "Others mucked")

    /**
     * Evaluates hands and awards pots after all showdown actions complete.
     */
    private fun completeShowdown() {
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

    /**
     * Gets the current showdown request for the active player.
     */
    fun getShowdownRequest(): ShowdownRequest? {
        if (state.phase != GamePhase.SHOWDOWN) return null
        val actor = state.currentActor ?: return null

        val isLastStanding = isLastPlayerStanding()
        val mustShow = state.showdownAggressorId == actor.player.id
        val validActions = when {
            // Last player standing: can show (optional reveal) or collect (take pot without showing)
            isLastStanding -> setOf(ShowdownActionType.SHOW, ShowdownActionType.COLLECT)

            // Aggressor must show
            mustShow -> setOf(ShowdownActionType.SHOW)

            // Others can show or muck
            else -> setOf(ShowdownActionType.SHOW, ShowdownActionType.MUCK)
        }

        return ShowdownRequest(
            playerId = actor.player.id,
            validActions = validActions,
            mustShow = mustShow,
            isLastPlayerStanding = isLastStanding,
        )
    }

    fun getActionRequest(): ActionRequest? {
        val actor = state.currentActor ?: return null
        val round = state.bettingRound ?: return null

        return bettingManager.getValidActions(
            playerState = actor,
            currentBet = round.currentBet,
            potSize = state.effectivePot,
            minRaise = round.minimumRaise,
        )
    }
}
