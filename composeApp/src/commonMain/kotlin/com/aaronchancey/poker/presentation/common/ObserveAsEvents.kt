package com.aaronchancey.poker.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/**
 * Observes a Flow of one-shot events and handles them via [onEvent].
 *
 * This is a Kotlin Multiplatform-compatible utility for handling side effects
 * from ViewModels. Unlike `collectAsState`, this doesn't hold state - it just
 * processes events as they arrive.
 *
 * @param flow The Flow of events to observe
 * @param key1 Optional key to restart collection when changed
 * @param onEvent Handler called for each emitted event
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = Unit,
    key2: Any? = Unit,
    onEvent: (T) -> Unit,
) {
    LaunchedEffect(flow, key1, key2) {
        flow.collect(onEvent)
    }
}
