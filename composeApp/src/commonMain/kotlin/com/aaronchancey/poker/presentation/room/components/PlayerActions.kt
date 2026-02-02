package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ActionType
import com.aaronchancey.poker.kpoker.betting.BettingType
import com.aaronchancey.poker.kpoker.betting.ShowdownActionType
import com.aaronchancey.poker.kpoker.betting.ShowdownRequest
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import poker.composeapp.generated.resources.Res
import poker.composeapp.generated.resources.showdownActionCollect
import poker.composeapp.generated.resources.showdownActionMuck
import poker.composeapp.generated.resources.showdownActionShow
import poker.composeapp.generated.resources.wagerActionAllIn
import poker.composeapp.generated.resources.wagerActionBet
import poker.composeapp.generated.resources.wagerActionCall
import poker.composeapp.generated.resources.wagerActionCheck
import poker.composeapp.generated.resources.wagerActionFold
import poker.composeapp.generated.resources.wagerActionPot
import poker.composeapp.generated.resources.wagerActionRaise

@Composable
internal fun PlayerActions(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    uiState: RoomUiState,
    onIntent: (RoomIntent) -> Unit,
) {
    val showdown = uiState.showdown
    val actions = uiState.availableActions

    if (showdown != null && showdown.playerId == playerState.player.id) {
        Showdown(
            modifier = modifier,
            playerId = playerState.player.id,
            showdownRequest = showdown,
            onIntent = onIntent,
        )
        return
    }

    if (playerState.hasActed || actions?.playerId != playerState.player.id) return

    ActionButtons(
        modifier = modifier,
        playerId = playerState.player.id,
        playerChips = playerState.chips,
        availableActions = actions,
        isLoading = uiState.isLoading,
        onIntent = onIntent,
    )
}

@Composable
private fun ActionButtons(
    modifier: Modifier,
    playerId: PlayerId,
    playerChips: ChipAmount,
    availableActions: ActionRequest,
    isLoading: Boolean,
    onIntent: (RoomIntent) -> Unit,
) {
    var betAmount by remember(availableActions) {
        mutableDoubleStateOf(calculateInitialBetAmount(availableActions))
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        availableActions.validActions.forEach { actionType ->
            ActionButton(
                playerId = playerId,
                playerChips = playerChips,
                actionType = actionType,
                availableActions = availableActions,
                betAmount = betAmount,
                enabled = !isLoading,
                onIntent = onIntent,
            )

            if (actionType.isVariableAmount()) {
                BetAmountInput(
                    minDenomination = availableActions.minimumDenomination,
                    rangeStart = calculateMinBetOrRaise(availableActions),
                    maximumBet = availableActions.maximumBet,
                    currentAmount = betAmount,
                    onAmountChange = { betAmount = it },
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    playerId: PlayerId,
    playerChips: ChipAmount,
    actionType: ActionType,
    availableActions: ActionRequest,
    betAmount: ChipAmount,
    enabled: Boolean,
    onIntent: (RoomIntent) -> Unit,
) = Button(
    onClick = {
        val action = createAction(
            playerId = playerId,
            playerChips = playerChips,
            actionType = actionType,
            availableActions = availableActions,
            betAmount = betAmount,
        )
        onIntent(RoomIntent.PerformAction(action))
    },
    enabled = enabled,
) {
    val stringRes = when (actionType) {
        ActionType.FOLD -> Res.string.wagerActionFold
        ActionType.CHECK -> Res.string.wagerActionCheck
        ActionType.CALL -> Res.string.wagerActionCall
        ActionType.BET -> Res.string.wagerActionBet
        ActionType.RAISE -> Res.string.wagerActionRaise
        ActionType.ALL_IN -> {
            val isPotLimitCapped = availableActions.bettingType == BettingType.POT_LIMIT &&
                availableActions.maximumBet < playerChips
            if (isPotLimitCapped) Res.string.wagerActionPot else Res.string.wagerActionAllIn
        }
    }
    Text(stringResource(stringRes))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BetAmountInput(
    minDenomination: ChipAmount,
    rangeStart: ChipAmount,
    maximumBet: ChipAmount,
    currentAmount: ChipAmount,
    onAmountChange: (ChipAmount) -> Unit,
) = Column {
    val safeMinimum = minOf(rangeStart, maximumBet)
    val safeDenomination = if (minDenomination > 0.0) minDenomination else 0.1

    val steps = if (maximumBet > safeMinimum) {
        maxOf(0, ((maximumBet - safeMinimum) / safeDenomination).toInt() - 1)
    } else {
        0
    }

    val sliderState = rememberSliderState(
        value = currentAmount.toFloat(),
        steps = steps,
        valueRange = safeMinimum.toFloat()..maximumBet.toFloat(),
    )

    val textFieldState = rememberTextFieldState(currentAmount.toString())

    sliderState.onValueChange = {
        val snappedValue = (it / safeDenomination).roundToInt() * safeDenomination
        sliderState.value = snappedValue.toFloat()
        textFieldState.setTextAndPlaceCursorAtEnd(snappedValue.toString())
        onAmountChange(snappedValue)
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collectLatest { text ->
            val num = text.toFloatOrNull()
            if (num != null && num != sliderState.value) {
                if (num in sliderState.valueRange) {
                    sliderState.value = num
                    onAmountChange(num.toDouble())
                }
            }
        }
    }

    TextField(
        modifier = Modifier.widthIn(200.dp, 200.dp),
        state = textFieldState,
        label = { Text("Amount") },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    Slider(
        modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 25.dp, maxWidth = 200.dp, maxHeight = 25.dp),
        state = sliderState,
    )
}

@Composable
internal fun Showdown(
    modifier: Modifier = Modifier,
    playerId: PlayerId,
    showdownRequest: ShowdownRequest,
    onIntent: (RoomIntent) -> Unit,
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    showdownRequest.validActions.forEach { actionType ->
        Button(onClick = {
            val action = when (actionType) {
                ShowdownActionType.MUCK -> Action.Muck(playerId)
                ShowdownActionType.SHOW -> Action.Show(playerId)
                ShowdownActionType.COLLECT -> Action.Collect(playerId)
            }
            onIntent(RoomIntent.PerformAction(action))
        }) {
            Text(
                stringResource(
                    when (actionType) {
                        ShowdownActionType.COLLECT -> Res.string.showdownActionCollect
                        ShowdownActionType.MUCK -> Res.string.showdownActionMuck
                        ShowdownActionType.SHOW -> Res.string.showdownActionShow
                    },
                ),
            )
        }
    }
}

private fun ActionType.isVariableAmount(): Boolean = this == ActionType.BET || this == ActionType.RAISE

private fun calculateInitialBetAmount(availableActions: ActionRequest): ChipAmount {
    val minimum = calculateMinBetOrRaise(availableActions)
    return minOf(minimum, availableActions.maximumBet)
}

private fun calculateMinBetOrRaise(availableActions: ActionRequest): ChipAmount {
    val isBet = ActionType.BET in availableActions.validActions
    return if (isBet) availableActions.minimumBet else availableActions.minimumBet + availableActions.minimumRaise
}

private fun createAction(
    playerId: PlayerId,
    playerChips: ChipAmount,
    actionType: ActionType,
    availableActions: ActionRequest,
    betAmount: ChipAmount,
): Action = when (actionType) {
    ActionType.FOLD -> Action.Fold(playerId)
    ActionType.CHECK -> Action.Check(playerId)
    ActionType.CALL -> Action.Call(playerId, availableActions.amountToCall)
    ActionType.BET -> Action.Bet(playerId, betAmount)
    ActionType.RAISE -> Action.Raise(playerId, betAmount - availableActions.amountToCall, betAmount)
    ActionType.ALL_IN -> {
        val isPotBet = availableActions.bettingType == BettingType.POT_LIMIT &&
            availableActions.maximumBet < playerChips
        if (isPotBet) {
            // Betting the pot, not actually going all-in
            val potAmount = availableActions.maximumBet
            if (ActionType.BET in availableActions.validActions) {
                Action.Bet(playerId, potAmount)
            } else {
                val raiseAmount = potAmount - availableActions.amountToCall
                Action.Raise(playerId, raiseAmount, potAmount)
            }
        } else {
            Action.AllIn(playerId, playerChips)
        }
    }
}
