package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.aaronchancey.poker.kpoker.core.Card

/**
 * Callback providing measurements needed to calculate the pot center position.
 *
 * @param columnHeight Total height of the TableCenter column
 * @param chipOffsetInColumn Y position of ChipStacks within the column
 * @param chipHeight Height of the ChipStacks component
 */
data class TableCenterMeasurements(
    val columnHeight: Float,
    val chipOffsetInColumn: Float,
    val chipHeight: Float,
)

/**
 * Displays the table center content: community cards and pot chips.
 *
 * Reports measurements via [onMeasured] so the parent can calculate
 * the pot center position for chip animations.
 *
 * @param communityCards List of community cards to display
 * @param displayedPot Amount to show in the pot (adjusted for animations)
 * @param onMeasured Callback with layout measurements for positioning
 */
@Composable
fun TableCenter(
    modifier: Modifier = Modifier,
    communityCards: List<Card>,
    displayedPot: Double,
    onMeasured: (TableCenterMeasurements) -> Unit,
) {
    var columnHeight = 0f
    var chipOffsetInColumn = 0f
    var chipHeight = 0f

    fun reportMeasurements() {
        onMeasured(
            TableCenterMeasurements(
                columnHeight = columnHeight,
                chipOffsetInColumn = chipOffsetInColumn,
                chipHeight = chipHeight,
            ),
        )
    }

    Column(
        modifier = modifier.onGloballyPositioned { coords ->
            columnHeight = coords.size.height.toFloat()
            reportMeasurements()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CommunityCards(communityCards = communityCards)
        ChipStacks(
            modifier = Modifier.onGloballyPositioned { coords ->
                chipOffsetInColumn = coords.positionInParent().y
                chipHeight = coords.size.height.toFloat()
                reportMeasurements()
            },
            wager = maxOf(0.0, displayedPot),
        )
    }
}
