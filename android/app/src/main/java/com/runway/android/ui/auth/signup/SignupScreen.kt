package com.runway.android.ui.auth.signup

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.credential.RunwayPasswordManager
import com.runway.android.ui.components.RunwayErrorText
import com.runway.android.ui.components.runwayCardFrame
import com.runway.android.ui.theme.DisplayFontFamily
import com.runway.android.ui.theme.PillShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator

private val BgColor    = Color(0xFF080C0B)
private val CardColor  = Color(0xFF0B100E)
private val GridColor  = Color(0xFF173028)
private val NeonGreen  = Color(0xFFB8FF00)

@Composable
fun SignupScreen(
    onNavigateBack: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val passwordManager = remember(context) { RunwayPasswordManager(context) }
    LaunchedEffect(Unit) {
        viewModel.navigateToLogin.collect { credentials ->
            runCatching {
                passwordManager.save(context, credentials.email, credentials.password)
            }
            onSignupSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        SignupBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
        ) {
            // 뒤로가기
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = NeonGreen)
                }
            }

            Spacer(Modifier.height(4.dp))

            // 로고 워드마크
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "Run",
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = DisplayFontFamily),
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen,
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
            Spacer(Modifier.height(4.dp))
            Text(
                "Create Account",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.35f),
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(28.dp))

            // ── 입력 폼 카드 ──────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(NeonGreen.copy(0.70f), NeonGreen.copy(0.22f), NeonGreen.copy(0.08f)),
                        ),
                        MaterialTheme.shapes.extraLarge,
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                color = CardColor,
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                    Text(
                        "SIGN UP",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DisplayFontFamily),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp).height(2.5.dp)
                            .background(NeonGreen, RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.height(24.dp))

                    SignupInputField(
                        value = viewModel.email,
                        onChange = viewModel::onEmailChange,
                        label = "EMAIL",
                        placeholder = "your@email.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = viewModel.error != null,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::requestEmailCode,
                        enabled = !viewModel.isEmailVerificationLoading &&
                            !viewModel.isEmailVerified,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.14f),
                            contentColor = NeonGreen,
                            disabledContainerColor = NeonGreen.copy(alpha = 0.07f),
                            disabledContentColor = NeonGreen.copy(alpha = 0.45f),
                        ),
                    ) {
                        Text(
                            if (viewModel.isEmailVerified) "이메일 인증 완료"
                            else if (viewModel.isEmailCodeSent) "인증번호 다시 받기"
                            else "인증번호 받기",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (viewModel.isEmailCodeSent && !viewModel.isEmailVerified) {
                        Spacer(Modifier.height(12.dp))
                        SignupInputField(
                            value = viewModel.emailCode,
                            onChange = viewModel::onEmailCodeChange,
                            label = "VERIFICATION CODE",
                            placeholder = "6자리 인증번호",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = viewModel.error != null,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = viewModel::verifyEmailCode,
                            enabled = !viewModel.isEmailVerificationLoading &&
                                viewModel.emailCode.length == 6,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = BgColor,
                            ),
                        ) {
                            Text("인증번호 확인", fontWeight = FontWeight.Bold)
                        }
                    }
                    viewModel.emailVerificationMessage?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen.copy(alpha = 0.85f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    SignupInputField(
                        value = viewModel.password,
                        onChange = viewModel::onPasswordChange,
                        label = "PASSWORD",
                        placeholder = "10자 이상",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = viewModel.error != null,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "10자 이상으로 입력하고 이메일·닉네임이나 흔한 비밀번호는 피해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                    Spacer(Modifier.height(14.dp))
                    SignupInputField(
                        value = viewModel.passwordConfirm,
                        onChange = viewModel::onPasswordConfirmChange,
                        label = "CONFIRM PASSWORD",
                        placeholder = "비밀번호 다시 입력",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = viewModel.error != null,
                    )
                    Spacer(Modifier.height(14.dp))
                    SignupInputField(
                        value = viewModel.nickname,
                        onChange = viewModel::onNicknameChange,
                        label = "NICKNAME",
                        placeholder = "러너 이름",
                        isError = viewModel.error != null,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::checkNickname,
                        enabled = !viewModel.isNicknameCheckLoading &&
                            viewModel.nickname.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.14f),
                            contentColor = NeonGreen,
                            disabledContainerColor = NeonGreen.copy(alpha = 0.07f),
                            disabledContentColor = NeonGreen.copy(alpha = 0.45f),
                        ),
                    ) {
                        if (viewModel.isNicknameCheckLoading) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                color = NeonGreen,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                if (viewModel.isNicknameChecked) "닉네임 확인 완료" else "닉네임 중복 확인",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    viewModel.nicknameCheckMessage?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewModel.isNicknameChecked) {
                                NeonGreen.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }

                    viewModel.error?.let {
                        Spacer(Modifier.height(8.dp))
                        RunwayErrorText(message = it, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── 약관 동의 카드 ────────────────────────────────────────────────
            var termsSheet by remember { mutableStateOf<TermsType?>(null) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(NeonGreen.copy(0.70f), NeonGreen.copy(0.22f), NeonGreen.copy(0.08f)),
                        ),
                        MaterialTheme.shapes.extraLarge,
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                color = CardColor,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    // 전체 동의
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleAll() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NeonCheckbox(checked = viewModel.agreeAll)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "전체 동의",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.agreeAll) NeonGreen else Color.White,
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = NeonGreen.copy(alpha = 0.12f))
                    Spacer(Modifier.height(14.dp))

                    AgreeRow(viewModel.agreeTerms,    required = true,  "서비스 이용약관",       viewModel::toggleTerms)    { termsSheet = TermsType.TERMS }
                    Spacer(Modifier.height(12.dp))
                    AgreeRow(viewModel.agreePrivacy,  required = true,  "개인정보 처리방침",      viewModel::togglePrivacy)  { termsSheet = TermsType.PRIVACY }
                    Spacer(Modifier.height(12.dp))
                    AgreeRow(viewModel.agreeMarketing, required = false, "마케팅 정보 수신 동의", viewModel::toggleMarketing) { termsSheet = TermsType.MARKETING }
                }
            }

            termsSheet?.let { type ->
                TermsSheet(type = type, onDismiss = { termsSheet = null })
            }

            Spacer(Modifier.height(20.dp))

            // 가입 버튼
            Button(
                onClick = viewModel::signup,
                enabled = !viewModel.isLoading &&
                    viewModel.isEmailVerified &&
                    viewModel.isNicknameChecked,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor   = BgColor,
                    disabledContainerColor = NeonGreen.copy(alpha = 0.5f),
                    disabledContentColor   = BgColor,
                ),
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = BgColor, strokeWidth = 2.dp)
                } else {
                    Text(
                        "JOIN RUNWAY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ── 배경 ─────────────────────────────────────────────────────────────────────

@Composable
private fun SignupBackground() {
    val pulse = rememberInfiniteTransition(label = "g")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.07f, targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(2800, easing = EaseInOut), RepeatMode.Reverse),
        label = "ga",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grid = 34.dp.toPx()
        var x = 0f
        while (x <= size.width) { drawLine(GridColor.copy(0.36f), Offset(x, 0f), Offset(x, size.height), 0.8f); x += grid }
        var y = 0f
        while (y <= size.height) { drawLine(GridColor.copy(0.36f), Offset(0f, y), Offset(size.width, y), 0.8f); y += grid }

        val center = Offset(size.width * 0.3f, size.height * 0.15f)
        drawCircle(
            Brush.radialGradient(
                listOf(NeonGreen.copy(glowAlpha * 0.6f), NeonGreen.copy(glowAlpha * 0.18f), Color.Transparent),
                center, size.width * 0.75f,
            ),
            size.width * 0.75f, center,
        )
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, BgColor), size.height * 0.40f, size.height),
        )
    }
}

// ── 약관 행 ──────────────────────────────────────────────────────────────────

@Composable
private fun AgreeRow(
    checked: Boolean,
    required: Boolean,
    label: String,
    onToggle: () -> Unit,
    onView: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 체크박스 영역 — 토글
        Row(
            modifier = Modifier.weight(1f).clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonCheckbox(checked = checked, small = true)
            Spacer(Modifier.width(10.dp))
            Text(
                if (required) "[필수] " else "[선택] ",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (required) FontWeight.SemiBold else FontWeight.Normal,
                color = if (required) NeonGreen.copy(0.80f) else Color.White.copy(0.38f),
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = if (checked) Color.White else Color.White.copy(0.55f),
            )
        }
        // 화살표 — 내용 보기
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "내용 보기",
            modifier = Modifier.size(18.dp).clickable { onView() },
            tint = NeonGreen.copy(0.50f),
        )
    }
}

@Composable
private fun NeonCheckbox(checked: Boolean, small: Boolean = false) {
    val size = if (small) 20.dp else 24.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) NeonGreen else Color.Transparent)
            .border(1.5.dp, if (checked) NeonGreen else Color.White.copy(0.30f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, null, Modifier.size(if (small) 12.dp else 14.dp), tint = BgColor)
        }
    }
}

// ── 입력 필드 ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignupInputField(
    value: String,
    onChange: (String) -> Unit,
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
            color = NeonGreen.copy(0.65f),
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(NeonGreen.copy(if (isError) 0f else 0.55f), NeonGreen.copy(0.15f), NeonGreen.copy(0.05f)),
                    ),
                    MaterialTheme.shapes.large,
                ),
            shape = MaterialTheme.shapes.large,
            color = Color.White.copy(alpha = 0.04f),
        ) {
            TextField(
                value = value,
                onValueChange = onChange,
                placeholder = {
                    Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.28f))
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
                    cursorColor = NeonGreen,
                    errorCursorColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

// ── 약관 바텀시트 ──────────────────────────────────────────────────────────────

enum class TermsType { TERMS, PRIVACY, MARKETING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsSheet(type: TermsType, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (title, content) = when (type) {
        TermsType.TERMS     -> "서비스 이용약관" to TERMS_CONTENT
        TermsType.PRIVACY   -> "개인정보 처리방침" to PRIVACY_CONTENT
        TermsType.MARKETING -> "마케팅 정보 수신 동의" to MARKETING_CONTENT
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(32.dp).height(2.dp).background(NeonGreen, RoundedCornerShape(2.dp)))
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = NeonGreen.copy(0.12f))
            Spacer(Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.75f),
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private val TERMS_CONTENT = """
제1조 (목적)
본 약관은 RunWay(이하 "회사")가 제공하는 러닝 코스 공유 서비스(이하 "서비스")의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.

제2조 (정의)
① "서비스"란 회사가 제공하는 GPS 기반 러닝 코스 기록, 공유, 경쟁 플랫폼을 의미합니다.
② "이용자"란 본 약관에 따라 서비스를 이용하는 회원을 말합니다.
③ "코스"란 이용자가 GPS로 기록한 러닝 경로를 의미합니다.

제3조 (약관의 효력 및 변경)
① 본 약관은 서비스를 이용하고자 하는 모든 이용자에게 적용됩니다.
② 회사는 필요한 경우 본 약관을 변경할 수 있으며, 변경 시 앱 내 공지를 통해 안내합니다.

제4조 (서비스의 제공)
① 회사는 GPS 기반 러닝 코스 기록 및 공유 서비스를 제공합니다.
② 회사는 서비스 내 코스 탐색, 도전, 리더보드 기능을 제공합니다.
③ 서비스는 연중무휴 24시간 제공을 원칙으로 하되, 시스템 점검 등으로 일시 중단될 수 있습니다.

제5조 (이용자의 의무)
① 이용자는 타인의 권리를 침해하는 코스를 등록해서는 안 됩니다.
② 이용자는 허위 정보를 기재하거나 타인을 사칭해서는 안 됩니다.
③ 이용자는 법령 또는 공서양속에 반하는 행위를 해서는 안 됩니다.

제6조 (지적재산권)
이용자가 등록한 코스 및 콘텐츠의 지식재산권은 해당 이용자에게 귀속됩니다. 단, 이용자는 회사에 서비스 내 코스 표시 및 공유에 필요한 범위의 이용 허락을 부여합니다.

제7조 (면책)
① 회사는 천재지변, 불가항력 등으로 인한 서비스 장애에 대해 책임지지 않습니다.
② 러닝 중 발생하는 부상, 사고 등에 대해 회사는 책임지지 않습니다.

제8조 (준거법 및 관할)
본 약관과 관련한 분쟁은 대한민국 법을 준거법으로 하며, 관할 법원은 서울중앙지방법원으로 합니다.

시행일: 2026년 1월 1일
""".trimIndent()

private val PRIVACY_CONTENT = """
RunWay(이하 "회사")는 개인정보보호법에 따라 이용자의 개인정보를 보호하기 위해 다음과 같이 개인정보 처리방침을 수립·공개합니다.

1. 수집하는 개인정보 항목
• 필수: 이메일 주소, 비밀번호(암호화 저장), 닉네임
• 자동 수집: GPS 위치 정보(러닝 중), 서비스 이용 기록, 기기 정보
• 선택: 프로필 사진

2. 개인정보의 수집 및 이용 목적
• 회원 가입 및 서비스 제공
• 러닝 코스 기록 및 공유 기능 제공
• 서비스 개선 및 신규 기능 개발
• 불법·부정 이용 방지

3. 개인정보의 보유 및 이용 기간
• 회원 탈퇴 시까지 보유하며, 탈퇴 후 즉시 파기합니다.
• 단, 관련 법령에 따라 보존이 필요한 경우 해당 기간 동안 보관합니다.

4. GPS 위치 정보 처리
• 러닝 시작 시에만 GPS 정보를 수집합니다.
• 수집된 GPS 정보는 코스 기록 및 주변 코스 탐색에 활용됩니다.
• GPS 정보는 이용자의 동의 없이 제3자에게 제공되지 않습니다.

5. 개인정보의 제3자 제공
회사는 이용자의 개인정보를 원칙적으로 외부에 제공하지 않습니다. 단, 이용자의 동의가 있거나 법령에 의한 경우는 예외로 합니다.

6. 개인정보의 파기
개인정보는 보유 기간 종료 후 지체 없이 파기합니다. 전자적 파일은 복구 불가능한 방법으로, 종이 문서는 분쇄 또는 소각합니다.

7. 이용자의 권리
이용자는 언제든지 자신의 개인정보를 조회, 수정, 삭제할 수 있습니다. 앱 내 프로필 > 설정에서 처리하거나 아래 이메일로 문의하실 수 있습니다.

8. 개인정보 보호 책임자
• 책임자: RunWay 개인정보보호팀
• 이메일: privacy@runway-app.kr

시행일: 2026년 1월 1일
""".trimIndent()

private val MARKETING_CONTENT = """
마케팅 정보 수신 동의 (선택)

RunWay는 아래와 같은 마케팅 정보를 발송할 수 있습니다.

수신 채널
• 앱 푸시 알림
• 이메일

발송 내용
• 신규 기능 안내 및 업데이트 소식
• 주변 신규 코스 추천
• 러닝 챌린지 및 이벤트 안내
• 맞춤형 러닝 팁 및 통계 리포트

동의 철회
마케팅 수신에 동의하신 경우에도 언제든지 앱 설정 > 알림 설정에서 수신을 거부하실 수 있습니다.

본 동의는 선택사항이며, 동의하지 않으셔도 RunWay의 핵심 서비스를 정상적으로 이용하실 수 있습니다.
""".trimIndent()
