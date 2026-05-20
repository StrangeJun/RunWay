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
                text = "함께 달려요.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "계정을 만들고 코스를 탐색해보세요.",
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
                label = "이메일",
                placeholder = "your@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = viewModel.error != null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            RunwayTextField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label = "비밀번호",
                placeholder = "••••••••",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = viewModel.error != null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            RunwayTextField(
                value = viewModel.nickname,
                onValueChange = viewModel::onNicknameChange,
                label = "닉네임",
                placeholder = "러너 이름을 입력하세요",
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
                text = "회원가입",
                onClick = viewModel::signup,
                isLoading = viewModel.isLoading,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "가입하면 서비스 이용약관 및 개인정보처리방침에 동의하게 됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
