package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.BrowserViewModel

@Composable
fun JsDialogHost(
    dialogRequest: BrowserViewModel.JsDialogRequest?,
    httpAuthRequest: BrowserViewModel.HttpAuthRequest?,
    onDismissJsDialog: () -> Unit,
    onDismissHttpAuth: () -> Unit
) {
    if (dialogRequest != null) {
        when (dialogRequest) {
            is BrowserViewModel.JsDialogRequest.Alert -> {
                AlertDialog(
                    onDismissRequest = {
                        dialogRequest.onCancel()
                        onDismissJsDialog()
                    },
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Page Notice", fontWeight = FontWeight.Bold) },
                    text = { Text(dialogRequest.message) },
                    confirmButton = {
                        Button(
                            onClick = {
                                dialogRequest.onConfirm("")
                                onDismissJsDialog()
                            },
                            modifier = Modifier.testTag("js_alert_confirm_button")
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
            is BrowserViewModel.JsDialogRequest.Confirm -> {
                AlertDialog(
                    onDismissRequest = {
                        dialogRequest.onCancel()
                        onDismissJsDialog()
                    },
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Confirmation", fontWeight = FontWeight.Bold) },
                    text = { Text(dialogRequest.message) },
                    confirmButton = {
                        Button(
                            onClick = {
                                dialogRequest.onConfirm("")
                                onDismissJsDialog()
                            },
                            modifier = Modifier.testTag("js_confirm_ok_button")
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                dialogRequest.onCancel()
                                onDismissJsDialog()
                            },
                            modifier = Modifier.testTag("js_confirm_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            is BrowserViewModel.JsDialogRequest.Prompt -> {
                var promptText by remember { mutableStateOf(dialogRequest.defaultValue) }
                AlertDialog(
                    onDismissRequest = {
                        dialogRequest.onCancel()
                        onDismissJsDialog()
                    },
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Prompt", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(dialogRequest.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("js_prompt_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                dialogRequest.onConfirm(promptText)
                                onDismissJsDialog()
                            },
                            modifier = Modifier.testTag("js_prompt_ok_button")
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                dialogRequest.onCancel()
                                onDismissJsDialog()
                            },
                            modifier = Modifier.testTag("js_prompt_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            is BrowserViewModel.JsDialogRequest.BeforeUnload -> {
                AlertDialog(
                    onDismissRequest = {
                        dialogRequest.onCancel()
                        onDismissJsDialog()
                    },
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Leave site?", fontWeight = FontWeight.Bold) },
                    text = { Text(dialogRequest.message.ifBlank { "Changes you made may not be saved." }) },
                    confirmButton = {
                        Button(
                            onClick = {
                                dialogRequest.onConfirm("")
                                onDismissJsDialog()
                            },
                            modifier = Modifier.testTag("js_leave_button")
                        ) {
                            Text("Leave")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                dialogRequest.onCancel()
                                onDismissJsDialog()
                            },
                            modifier = Modifier.testTag("js_stay_button")
                        ) {
                            Text("Stay")
                        }
                    }
                )
            }
        }
    }

    if (httpAuthRequest != null) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                httpAuthRequest.onCancel()
                onDismissHttpAuth()
            },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Sign In Required", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${httpAuthRequest.host} says: ${httpAuthRequest.realm}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("http_auth_username")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("http_auth_password")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        httpAuthRequest.onSubmit(username, password)
                        onDismissHttpAuth()
                    },
                    modifier = Modifier.testTag("http_auth_submit_button")
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        httpAuthRequest.onCancel()
                        onDismissHttpAuth()
                    },
                    modifier = Modifier.testTag("http_auth_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
