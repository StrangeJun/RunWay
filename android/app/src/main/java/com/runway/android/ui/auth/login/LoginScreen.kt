package com.runway.android.ui.auth.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.runway.android.BuildConfig
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.R
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.runway.android.ui.components.RunwayErrorText
import com.runway.android.ui.theme.PillShape
import com.runway.android.ui.theme.DisplayFontFamily
import kotlinx.coroutines.delay

private val BgColor   = Color(0xFF080C0B)
private val CardColor = Color(0xFF0B100E)
private val GridColor = Color(0xFF173028)
private val LogoGreen = Color(0xFFB8FF00)

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    var showSuccessAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.navigateToHome.collect { showSuccessAnim = true } }

    val context = LocalContext.current
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d("GoogleSignIn", "idToken null=${idToken == null}, email=${account.email}")
            if (idToken != null) {
                viewModel.loginWithGoogle(idToken)
            } else {
                viewModel.setGoogleError("Google 토큰을 받지 못했습니다. Web Client ID를 확인하세요.")
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "ApiException code=${e.statusCode} msg=${e.message}")
            val msg = when (e.statusCode) {
                10   -> "설정 오류(10): SHA-1 지문 또는 Client ID를 확인하세요."
                12500 -> "로그인 실패(12500)"
                12501 -> null // 사용자 취소 — 무시
                else -> "Google 로그인 오류 (${e.statusCode})"
            }
            msg?.let { viewModel.setGoogleError(it) }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BgColor),
    ) {
        LoginBackground(accentColor = LogoGreen)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            LogoSection()
            Spacer(Modifier.height(52.dp))
            FormCard(
                viewModel = viewModel,
                onGoogleClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
            )
            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.09f))
                Text(
                    "  Don't have an account?  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.32f),
                )
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.09f))
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = onNavigateToSignup,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.White.copy(alpha = 0.05f),
            ) {
                Text(
                    "Sign Up",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = LogoGreen.copy(alpha = 0.80f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }

            Spacer(Modifier.height(48.dp))
        }

        if (showSuccessAnim) {
            LoginSuccessOverlay(onAnimationEnd = onLoginSuccess)
        }
    }
}

// ── 배경 ─────────────────────────────────────────────────────────────────────

@Composable
private fun LoginBackground(accentColor: Color) {
    val pulse = rememberInfiniteTransition(label = "glow")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.09f, targetValue = 0.17f,
        animationSpec = infiniteRepeatable(tween(2800, easing = EaseInOut), RepeatMode.Reverse),
        label = "g",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val grid = 34.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(GridColor.copy(alpha = 0.36f), Offset(x, 0f), Offset(x, size.height), 0.8f)
            x += grid
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(GridColor.copy(alpha = 0.36f), Offset(0f, y), Offset(size.width, y), 0.8f)
            y += grid
        }

        val center = Offset(size.width / 2f, size.height * 0.23f)
        // 넓은 헤일로
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(glowAlpha * 0.5f), accentColor.copy(glowAlpha * 0.18f), Color.Transparent),
                center = center, radius = size.width * 0.80f,
            ),
            radius = size.width * 0.80f, center = center,
        )
        // 코어 글로우
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(glowAlpha * 1.3f), Color.Transparent),
                center = center, radius = size.width * 0.28f,
            ),
            radius = size.width * 0.28f, center = center,
        )
        // 빛기둥
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, accentColor.copy(glowAlpha * 0.09f), accentColor.copy(glowAlpha * 0.14f), accentColor.copy(glowAlpha * 0.09f), Color.Transparent),
                startY = 0f, endY = size.height * 0.50f,
            ),
            topLeft = Offset(center.x - size.width * 0.05f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.10f, size.height * 0.50f),
        )
        // 하단 페이드
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, BgColor),
                startY = size.height * 0.42f, endY = size.height,
            ),
        )
    }
}

// ── 로고 섹션 ──────────────────────────────────────────────────────────────────

@Composable
private fun LogoSection() {
    Box(contentAlignment = Alignment.Center) {
        // 바깥 글로우
        Canvas(modifier = Modifier.size(156.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LogoGreen.copy(0.18f), LogoGreen.copy(0.06f), Color.Transparent),
                ),
            )
        }
        // 안쪽 글로우
        Canvas(modifier = Modifier.size(90.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LogoGreen.copy(0.40f), Color.Transparent),
                ),
            )
        }
        // 앱 로고
        Image(
            painter = painterResource(R.drawable.app_logo_mark),
            contentDescription = "RunWay",
            modifier = Modifier.size(72.dp),
        )
    }

    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "Run",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = DisplayFontFamily),
            fontWeight = FontWeight.ExtraBold,
            color = LogoGreen,
            letterSpacing = (-0.5).sp,
        )
        Text(
            "Way",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = DisplayFontFamily),
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = (-0.5).sp,
        )
    }

    Spacer(Modifier.height(8.dp))
    Text(
        "Explore · Run · Share",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.35f),
        letterSpacing = 2.sp,
    )

    Spacer(Modifier.height(18.dp))
    TrackDots()
}

@Composable
private fun TrackDots() {
    Canvas(modifier = Modifier.fillMaxWidth(0.50f).height(6.dp)) {
        val dotR = 2.5.dp.toPx()
        val gap  = 10.dp.toPx()
        val count = (size.width / (dotR * 2 + gap)).toInt()
        val totalW = count * (dotR * 2 + gap) - gap
        val sx = (size.width - totalW) / 2f
        repeat(count) { i ->
            val cx = sx + i * (dotR * 2 + gap) + dotR
            val alpha = (1f - kotlin.math.abs(i - count / 2f) / (count / 2f + 1)).coerceAtLeast(0.12f)
            drawCircle(LogoGreen.copy(alpha = alpha * 0.65f), dotR, Offset(cx, size.height / 2f))
        }
    }
}

// ── 폼 카드 ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormCard(viewModel: LoginViewModel, onGoogleClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = LogoGreen.copy(alpha = 0.14f),
                spotColor   = LogoGreen.copy(alpha = 0.20f),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(LogoGreen.copy(0.70f), LogoGreen.copy(0.22f), LogoGreen.copy(0.08f)),
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = CardColor,
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {

            // 헤더
            Column {
                Text(
                    "LOGIN",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DisplayFontFamily),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.5.dp)
                        .background(LogoGreen, RoundedCornerShape(2.dp)),
                )
            }

            Spacer(Modifier.height(28.dp))

            LoginField(
                value = viewModel.email,
                onValueChange = viewModel::onEmailChange,
                label = "EMAIL",
                placeholder = "your@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = viewModel.error != null,
            )
            Spacer(Modifier.height(14.dp))
            LoginField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label = "PASSWORD",
                placeholder = "••••••••",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = viewModel.error != null,
            )

            viewModel.error?.let {
                Spacer(Modifier.height(8.dp))
                RunwayErrorText(message = it, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::login,
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LogoGreen,
                    contentColor = Color(0xFF080C0B),
                    disabledContainerColor = LogoGreen.copy(alpha = 0.5f),
                    disabledContentColor = Color(0xFF080C0B),
                ),
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF080C0B),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        "LOGIN",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 구분선
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.10f))
                Text(
                    "  OR  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.30f),
                    letterSpacing = 1.sp,
                )
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.10f))
            }

            Spacer(Modifier.height(16.dp))

            // Google 버튼
            Surface(
                onClick = onGoogleClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Color.White.copy(0.22f), Color.White.copy(0.08f))),
                        MaterialTheme.shapes.extraLarge,
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.White.copy(alpha = 0.06f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Continue with Google",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = LogoGreen.copy(alpha = 0.65f),
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(LogoGreen.copy(0.55f), LogoGreen.copy(0.15f), LogoGreen.copy(0.05f)),
                    ),
                    shape = MaterialTheme.shapes.large,
                ),
            shape = MaterialTheme.shapes.large,
            color = Color.White.copy(alpha = 0.04f),
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(placeholder, style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.28f))
                },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                isError = isError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor     = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor     = Color.Transparent,
                    cursorColor = LogoGreen,
                    errorCursorColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

// ── 로그인 성공 달리기 애니메이션 ─────────────────────────────────────────────

@Composable
private fun LoginSuccessOverlay(onAnimationEnd: () -> Unit) {
    val screenWidthDp  = LocalConfiguration.current.screenWidthDp.dp
    val bgAlpha        = remember { Animatable(0f) }
    val runnerX        = remember { Animatable(-60f) }
    val runnerAlpha    = remember { Animatable(0f) }
    val fadeOut        = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        bgAlpha.animateTo(0.97f, tween(300, easing = EaseIn))
        runnerAlpha.animateTo(1f, tween(120))
        runnerX.animateTo(screenWidthDp.value + 60f, tween(680, easing = EaseOut))
        delay(60)
        fadeOut.animateTo(0f, tween(280))
        onAnimationEnd()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .graphicsLayer { alpha = fadeOut.value }
            .background(BgColor.copy(alpha = bgAlpha.value)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = runnerX.value.dp.toPx()
            val cy = size.height / 2f
            val a  = runnerAlpha.value
            listOf(0f to 180f, 16f to 130f, -16f to 130f, 32f to 80f, -32f to 80f, 50f to 44f, -50f to 44f)
                .forEach { (yOff, len) ->
                    drawLine(
                        color = LogoGreen.copy(alpha = 0.32f * a),
                        start = Offset(cx - 44.dp.toPx() - len, cy + yOff - 28f),
                        end   = Offset(cx - 44.dp.toPx(), cy + yOff - 28f),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
            drawLine(
                color = LogoGreen.copy(alpha = 0.10f * a),
                start = Offset(0f, cy + 44f),
                end   = Offset(cx - 20.dp.toPx(), cy + 44f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
            contentDescription = null,
            tint = LogoGreen,
            modifier = Modifier.size(76.dp).graphicsLayer {
                translationX = runnerX.value.dp.toPx() - (screenWidthDp.value / 2).dp.toPx()
                alpha = runnerAlpha.value
            },
        )
    }
}
