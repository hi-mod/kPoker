package com.aaronchancey.poker.presentation.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerActions(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    uiState: RoomUiState,
    onIntent: (RoomIntent) -> Unit,
) {
    if (playerState.hasActed || uiState.availableActions?.playerId != playerState.player.id) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uiState.availableActions.validActions.forEach { actionType ->
            var betAmount: ChipAmount by remember {
                mutableDoubleStateOf(
                    if (uiState.availableActions.validActions.contains(ActionType.BET)) {
                        uiState.availableActions.minimumBet
                    } else {
                        uiState.availableActions.minimumBet + uiState.availableActions.minimumRaise
                    },
                )
            }
            Button(
                onClick = actionClick(
                    playerId = playerState.player.id,
                    actionType = actionType,
                    availableActions = uiState.availableActions,
                    betAmount = betAmount,
                    onIntent = onIntent,
                ),
                enabled = !uiState.isLoading,
            ) {
                Text(actionType.name)
            }
            if (actionType == ActionType.BET || actionType == ActionType.RAISE) {
                BetActionContent(
                    minDenomination = uiState.availableActions.minimumDenomination,
                    minimumBet = betAmount,
                    maximumBet = uiState.availableActions.maximumBet,
                    onBetAmountChange = { betAmount = it },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BetActionContent(
    minDenomination: ChipAmount,
    minimumBet: ChipAmount,
    maximumBet: ChipAmount,
    onBetAmountChange: (ChipAmount) -> Unit,
) {
    Column {
        val rangeStart = minimumBet.toFloat()
        val rangeEnd = maximumBet.toFloat()
        val safeDenomination = if (minDenomination > 0.0) minDenomination else 0.1
        val steps = if (rangeEnd > rangeStart) {
            maxOf(0, ((rangeEnd - rangeStart) / safeDenomination).toInt() - 1)
        } else {
            0
        }

        val sliderState = rememberSliderState(
            value = rangeStart,
            steps = steps,
            valueRange = rangeStart..rangeEnd,
        )
        val textFieldState = rememberTextFieldState(rangeStart.toString())
        sliderState.onValueChange = {
            val snappedValue = (it / safeDenomination).roundToInt() * safeDenomination
            sliderState.value = snappedValue.toFloat()
            textFieldState.setTextAndPlaceCursorAtEnd(snappedValue.toString())
            onBetAmountChange(snappedValue)
        }
        LaunchedEffect(textFieldState) {
            snapshotFlow { textFieldState.text.toString() }.collectLatest {
                if (sliderState.value != it.toFloatOrNull()) {
                    sliderState.value = it.toFloatOrNull() ?: minimumBet.toFloat()
                    onBetAmountChange(sliderState.value.toDouble())
                }
            }
        }
        TextField(
            modifier = Modifier.widthIn(200.dp, 200.dp),
            state = textFieldState,
            label = { Text("Bet Amount") },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Slider(
            modifier = Modifier.widthIn(200.dp, 200.dp),
            state = sliderState,
        )
    }
}

private fun actionClick(
    playerId: PlayerId,
    actionType: ActionType,
    availableActions: ActionRequest,
    onIntent: (RoomIntent) -> Unit,
    betAmount: ChipAmount,
): () -> Unit = {
    val action: Action = when (actionType) {
        ActionType.FOLD -> Action.Fold(playerId)
        ActionType.CHECK -> Action.Check(playerId)
        ActionType.CALL -> Action.Call(playerId, availableActions.amountToCall)
        ActionType.BET -> Action.Bet(playerId, availableActions.minimumBet)
        ActionType.RAISE -> Action.Raise(playerId, betAmount - availableActions.amountToCall, betAmount)
        ActionType.ALL_IN -> Action.AllIn(playerId, availableActions.maximumBet)
    }
    onIntent(RoomIntent.PerformAction(action))
}
