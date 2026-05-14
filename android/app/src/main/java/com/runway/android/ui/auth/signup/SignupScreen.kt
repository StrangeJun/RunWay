package com.runway.android.ui.auth.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RunwayErrorText
import com.runway.android.ui.components.RunwayLoadingButton
import com.runway.android.ui.components.RunwayTextField

@Composable
fun SignupScreen(
    onNavigateBack: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.navigateToLogin.collect { onSignupSuccess() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ─── Top bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // ─── Header ───
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)) {
            Text(
                text = "Join the run.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your account and start exploring routes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ─── Fields ───
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            RunwayTextField(
                value = viewModel.nickname,
                onValueChange = viewModel::onNicknameChange,
                label = "Nickname",
                placeholder = "Your runner name",
                isError = viewModel.error != null,
            )
        }

        // ─── Error ───
        if (viewModel.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            RunwayErrorText(
                message = viewModel.error!!,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
        }

        // ─── Actions ───
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Spacer(modifier = Modifier.height(32.dp))

            RunwayLoadingButton(
                text = "Create account",
                onClick = viewModel::signup,
                isLoading = viewModel.isLoading,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "By creating an account, you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
