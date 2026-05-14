package com.runway.android.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RunwayErrorText
import com.runway.android.ui.components.RunwayLoadingButton
import com.runway.android.ui.components.RunwayTextField

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onLoginSuccess() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ─── Header ───
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 40.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "R",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome back.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Lace up and pick up where you left off.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ─── Fields ───
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            RunwayTextField(
                value = viewModel.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                placeholder = "your@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = viewModel.error != null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            RunwayTextField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                placeholder = "••••••••",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = viewModel.error != null,
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {},
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ─── Error ───
        if (viewModel.error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            RunwayErrorText(
                message = viewModel.error!!,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
        }

        // ─── Actions ───
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Spacer(modifier = Modifier.height(24.dp))

            RunwayLoadingButton(
                text = "Log in",
                onClick = viewModel::login,
                isLoading = viewModel.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToSignup,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("New here? ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Create an account")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
