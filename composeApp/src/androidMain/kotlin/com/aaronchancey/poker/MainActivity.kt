package com.aaronchancey.poker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.russhwolf.settings.SharedPreferencesSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val settings = SharedPreferencesSettings(getSharedPreferences("poker_prefs", MODE_PRIVATE))

        setContent {
            App(settings = settings)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
