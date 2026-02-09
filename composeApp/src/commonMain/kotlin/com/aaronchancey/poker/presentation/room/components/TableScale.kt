package com.aaronchancey.poker.presentation.room.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Scale-dependent sizes for responsive poker table layout.
 *
 * Provided via [LocalTableScale] and computed from available constraints in [PokerTableScene].
 * Components read these values instead of hardcoding dp/sp, allowing the entire table
 * to scale correctly across desktop and mobile viewports.
 */
data class TableScale(
    /** Fraction of min(width, height) used for card height. */
    val cardHeightFraction: Float,
    /** Diameter of individual poker chip circles. */
    val chipSize: Dp,
    /** Font size for chip value text. */
    val chipTextSize: TextUnit,
    /** Font size for player name/chips nameplate. */
    val nameplateTextSize: TextUnit,
    /** Padding around the table image. */
    val tablePadding: Dp,
    /** Minimum height of the bottom action bar. */
    val actionBarMinHeight: Dp,
    /** Width constraint for bet input field and slider. */
    val betInputWidth: Dp,
    /** Spacing between action buttons. */
    val buttonSpacing: Dp,
    /** Pixel offset between fanned hole cards. */
    val holeCardOffset: Int,
    /** Rotation degrees per hole card in fan layout. */
    val holeCardRotation: Float,
    /** Vertical offset per stacked chip. */
    val chipStackOffset: Dp,
    /** Whether this is a mobile-sized viewport. */
    val isMobile: Boolean,
) {
    companion object {
        /** Desktop/large screen preset — matches current hardcoded values. */
        val Desktop = TableScale(
            cardHeightFraction = 0.125f,
            chipSize = 35.dp,
            chipTextSize = 15.sp,
            nameplateTextSize = 16.sp,
            tablePadding = 16.dp,
            actionBarMinHeight = 105.dp,
            betInputWidth = 200.dp,
            buttonSpacing = 8.dp,
            holeCardOffset = 8,
            holeCardRotation = 10f,
            chipStackOffset = 4.dp,
            isMobile = false,
        )

        /** Mobile/small screen preset — tighter spacing, smaller elements. */
        val Mobile = TableScale(
            cardHeightFraction = 0.07f,
            chipSize = 22.dp,
            chipTextSize = 9.sp,
            nameplateTextSize = 10.sp,
            tablePadding = 8.dp,
            actionBarMinHeight = 56.dp,
            betInputWidth = 120.dp,
            buttonSpacing = 4.dp,
            holeCardOffset = 5,
            holeCardRotation = 8f,
            chipStackOffset = 2.dp,
            isMobile = true,
        )
    }
}

/**
 * CompositionLocal providing the current [TableScale] to descendant composables.
 * Defaults to [TableScale.Desktop] when not explicitly provided.
 */
val LocalTableScale = compositionLocalOf { TableScale.Desktop }
