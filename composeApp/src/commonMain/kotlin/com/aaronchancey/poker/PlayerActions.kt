package com.aaronchancey.poker

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
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerActions(
    playerState: PlayerState,
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
) {
    if (playerState.hasActed || uiState.availableActions?.playerId != playerState.player.id) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.availableActions.validActions.forEach { actionType ->
            var betAmount: ChipAmount by remember {
                mutableDoubleStateOf(uiState.availableActions.minimumBet + uiState.availableActions.minimumRaise)
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
                    minimumBet = uiState.availableActions.minimumBet,
                    minimumRaise = uiState.availableActions.minimumRaise,
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
    minimumBet: ChipAmount,
    minimumRaise: ChipAmount,
    maximumBet: ChipAmount,
    onBetAmountChange: (ChipAmount) -> Unit,
) {
    Column {
        val sliderState = rememberSliderState(
            value = (minimumBet + minimumRaise).toFloat(),
            valueRange = (minimumBet + minimumRaise).toFloat()..maximumBet.toFloat(),
        )
        val textFieldState = rememberTextFieldState((minimumBet + minimumRaise).toString())
        sliderState.onValueChange = {
            val rounded = (it * 100).roundToInt() / 100.0
            sliderState.value = rounded.toFloat()
            textFieldState.setTextAndPlaceCursorAtEnd(rounded.toString())
            onBetAmountChange(it.toDouble())
        }
        LaunchedEffect(textFieldState) {
            snapshotFlow { textFieldState.text.toString() }.collectLatest {
                if (sliderState.value != it.toFloatOrNull()) {
                    sliderState.value = it.toFloatOrNull() ?: (minimumBet + minimumRaise).toFloat()
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
    onIntent: (GameIntent) -> Unit,
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
    onIntent(GameIntent.PerformAction(action))
}

@Preview
@Composable
private fun PlayerActionsPreview() {
    val playerState = PlayerState(
        player = Player(
            id = "player1",
            name = "Alice",
        ),
        chips = 1000.0,
        holeCards = emptyList(),
        isDealer = false,
        hasActed = false,
        status = PlayerStatus.ACTIVE,
    )
    val availableActions = ActionRequest(
        playerId = "player1",
        validActions = setOf(
            ActionType.FOLD,
            ActionType.CALL,
            ActionType.RAISE,
            ActionType.ALL_IN,
        ),
        amountToCall = 50.0,
        minimumBet = 100.0,
        minimumRaise = 50.0,
        maximumBet = 100.0,
    )
    val uiState = GameUiState(
        isLoading = false,
        availableActions = availableActions,
        gameState = null,
    )
    PlayerActions(
        playerState = playerState,
        uiState = uiState,
        onIntent = {},
    )
}
