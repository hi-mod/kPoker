package poker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.aaronchancey.poker.App
import com.russhwolf.settings.StorageSettings

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    val settings = StorageSettings()
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        App(settings = settings)
    }
}
