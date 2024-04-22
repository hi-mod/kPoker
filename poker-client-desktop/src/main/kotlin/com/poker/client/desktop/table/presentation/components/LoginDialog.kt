package com.poker.client.desktop.table.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.poker.client.desktop.table.presentation.PokerEvent
import com.poker.client.desktop.table.presentation.PokerState

@Composable
fun LoginDialog(
    state: PokerState,
    onEvent: (PokerEvent) -> Unit,
) {
    Dialog(
        onDismissRequest = { onEvent(PokerEvent.OnDismissLogin) },
    ) {
        Card(
            elevation = 1.dp,
        ) {
            val userFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                userFocusRequester.requestFocus()
            }

            var username by remember { mutableStateOf(state.username) }
            var password by remember { mutableStateOf(state.password) }
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(.7f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Login",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { onEvent(PokerEvent.OnDismissLogin) },
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black,
                        )
                    }
                }
                TextField(
                    modifier = Modifier.focusRequester(userFocusRequester),
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("User") },
                    singleLine = true,
/*
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
*/
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
/*
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onPrevious = { focusManager.moveFocus(FocusDirection.Up) },
                    ),
*/
                )
                Row {
                    Button(
                        onClick = {
                            onEvent(PokerEvent.SaveLogin(username, password))
                            onEvent(PokerEvent.OnLogin)
                            onEvent(PokerEvent.OnDismissLogin)
                        },
                    ) {
                        Text("Login")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = { onEvent(PokerEvent.OnDismissLogin) },
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
