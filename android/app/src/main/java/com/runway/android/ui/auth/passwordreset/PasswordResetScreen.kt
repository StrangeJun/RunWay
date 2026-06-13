package com.runway.android.ui.auth.passwordreset

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RunwayErrorText
import com.runway.android.ui.theme.PillShape

private val ResetBackground = Color(0xFF080C0B)
private val ResetCard = Color(0xFF0B100E)
private val ResetGreen = Color(0xFFB8FF00)

@Composable
fun PasswordResetScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    viewModel: PasswordResetViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.completed.collect { onCompleted() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ResetBackground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = ResetGreen)
            }
            Text(
                "비밀번호 재설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            ResetGreen.copy(0.70f),
                            ResetGreen.copy(0.22f),
                            ResetGreen.copy(0.08f),
                        ),
                    ),
                    MaterialTheme.shapes.extraLarge,
                ),
            shape = MaterialTheme.shapes.extraLarge,
            color = ResetCard,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = when (viewModel.step) {
                        PasswordResetStep.EMAIL -> "가입한 이메일을 입력하세요"
                        PasswordResetStep.CODE -> "이메일로 받은 인증번호를 입력하세요"
                        PasswordResetStep.PASSWORD -> "새 비밀번호를 설정하세요"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                when (viewModel.step) {
                    PasswordResetStep.EMAIL -> ResetField(
                        value = viewModel.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "your@email.com",
                        keyboardType = KeyboardType.Email,
                    )
                    PasswordResetStep.CODE -> ResetField(
                        value = viewModel.code,
                        onValueChange = viewModel::onCodeChange,
                        placeholder = "6자리 인증번호",
                        keyboardType = KeyboardType.Number,
                    )
                    PasswordResetStep.PASSWORD -> {
                        ResetField(
                            value = viewModel.password,
                            onValueChange = viewModel::onPasswordChange,
                            placeholder = "새 비밀번호 8자 이상",
                            keyboardType = KeyboardType.Password,
                            password = true,
                        )
                        ResetField(
                            value = viewModel.passwordConfirm,
                            onValueChange = viewModel::onPasswordConfirmChange,
                            placeholder = "새 비밀번호 확인",
                            keyboardType = KeyboardType.Password,
                            password = true,
                        )
                    }
                }

                viewModel.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = ResetGreen)
                }
                viewModel.error?.let {
                    RunwayErrorText(message = it, modifier = Modifier.fillMaxWidth())
                }

                Button(
                    onClick = when (viewModel.step) {
                        PasswordResetStep.EMAIL -> viewModel::requestCode
                        PasswordResetStep.CODE -> viewModel::verifyCode
                        PasswordResetStep.PASSWORD -> viewModel::resetPassword
                    },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResetGreen,
                        contentColor = ResetBackground,
                    ),
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            color = ResetBackground,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            when (viewModel.step) {
                                PasswordResetStep.EMAIL -> "인증번호 받기"
                                PasswordResetStep.CODE -> "인증번호 확인"
                                PasswordResetStep.PASSWORD -> "비밀번호 변경"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    password: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = Color.White.copy(alpha = 0.30f))
        },
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = ResetGreen,
        ),
    )
}
