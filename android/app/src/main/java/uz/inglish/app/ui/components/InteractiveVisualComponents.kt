package uz.inglish.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.ui.theme.InglishTheme
import kotlin.math.sin

// ============================================================================
// 1. COUNTDOWN ARC (Kahoot uslubidagi dumaloq taymer)
// ============================================================================

/**
 * Kahoot uslubidagi aylanma orqaga hisoblash taymeri.
 *
 * @param remainingSeconds Qolgan soniya miqdori (masalan, 20f dan 0f gacha).
 * @param totalSeconds Umumiy berilgan vaqt (standart 20 soniya).
 * @param warningThresholdSeconds Qizil rang va pulsatsiya boshlanadigan chegara (standart 5s).
 * @param modifier Tashqi modifikator.
 * @param size Taymer aylanasi diametri (standart 84.dp).
 * @param strokeWidth Aylananing qalinligi (standart 8.dp).
 */
@Composable
fun CountdownArc(
    remainingSeconds: Float,
    totalSeconds: Float = 20f,
    warningThresholdSeconds: Float = 5f,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    strokeWidth: Dp = 8.dp,
    primaryColor: Color = Color(0xFF1A6FE8),
    warningColor: Color = Color(0xFFE53935),
    cautionColor: Color = Color(0xFFF9A825),
    trackColor: Color = Color(0xFFE8EDF5),
) {
    val progress = (remainingSeconds / totalSeconds.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val isWarning = remainingSeconds in 0.01f..warningThresholdSeconds

    // Oxirgi 5 soniyada qizil rangda shiddatli pulsatsiyalanish (scale va yaltirash)
    val infiniteTransition = rememberInfiniteTransition(label = "countdown_pulse")
    val animatedPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseScale = if (isWarning) animatedPulse else 1.0f

    val activeColor by animateColorAsState(
        targetValue = when {
            remainingSeconds <= warningThresholdSeconds -> warningColor
            remainingSeconds <= 10f -> cautionColor
            else -> primaryColor
        },
        animationSpec = tween(300),
        label = "arc_color",
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Orqa fondagi to'liq yordamchi halqa
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Qolgan vaqtni ifodalovchi yoy (soat 12 dan boshlanadi: -90f)
            if (progress > 0f) {
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        // Markaziy raqam (soniyalar ko'rsatkichi)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val displaySec = remainingSeconds.toInt().coerceAtLeast(0)
            Text(
                text = "$displaySec",
                fontSize = (size.value * 0.32f).sp,
                fontWeight = FontWeight.Bold,
                color = activeColor,
                textAlign = TextAlign.Center,
            )
            if (size >= 80.dp) {
                Text(
                    text = "sek",
                    fontSize = (size.value * 0.12f).sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280),
                )
            }
        }
    }
}

// ============================================================================
// 2. LIVE WAVEFORM (Speaking audio yozish animatsiyasi)
// ============================================================================

/**
 * Speaking paytida ovoz yozilayotganini ko'rsatuvchi 12 ta sakrovchi chastota chizig'i.
 *
 * @param isRecording Ovoz yozish faol yoki to'xtatilgan holati.
 * @param modifier Tashqi modifikator.
 * @param barCount Spektr chiziqlari soni (standart 12 ta).
 * @param activeColor Spektr rangi (Material Theme secondary yashil yoki primary).
 * @param idleColor Ovoz yozilmayotgandagi jimjit holat rangi.
 * @param maxBarHeight Chiziqlarning maksimal balandligi.
 * @param barWidth Har bir chiziqning eni.
 * @param barGap Chiziqlar orasidagi masofa.
 */
@Composable
fun LiveWaveform(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    activeColor: Color = Color(0xFF2ECC71),
    idleColor: Color = Color(0xFFCFD8DC),
    maxBarHeight: Dp = 44.dp,
    barWidth: Dp = 5.dp,
    barGap: Dp = 4.dp,
) {
    // 0f dan 2PI gacha cheksiz faza o'zgarishi
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase_loop",
    )

    // O'rtadagi barlarni chekkadagilarga nisbatan balandroq qilish uchun Gauss koeffitsientlari
    val bellCurve = remember(barCount) {
        FloatArray(barCount) { index ->
            val center = (barCount - 1) / 2.0
            val distance = (index - center) / center
            // O'rtada 1.0, chetlarda ~0.35
            (1.0 - 0.65 * (distance * distance)).toFloat().coerceIn(0.25f, 1.0f)
        }
    }

    Row(
        modifier = modifier
            .height(maxBarHeight)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(barGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(barCount) { index ->
            // Har bir bar uchun individual garmonika va tebranish balandligi
            val dynamicHeightFraction = if (isRecording) {
                val wave1 = sin(phase + index * 0.65f)
                val wave2 = sin(phase * 1.8f + index * 0.9f)
                val rawNormalized = ((wave1 * 0.6f + wave2 * 0.4f + 1f) / 2f).coerceIn(0f, 1f)
                (0.18f + rawNormalized * 0.82f) * bellCurve[index]
            } else {
                0.14f // Jimjit holat (minimal chiziqcha)
            }

            val animatedHeight by animateFloatAsState(
                targetValue = dynamicHeightFraction,
                animationSpec = if (isRecording) tween(60) else spring(dampingRatio = 0.8f),
                label = "bar_height_$index",
            )

            val animatedColor by animateColorAsState(
                targetValue = if (isRecording) activeColor else idleColor,
                animationSpec = tween(250),
                label = "bar_color_$index",
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxBarHeight * animatedHeight.coerceIn(0.12f, 1.0f))
                    .clip(CircleShape)
                    .background(animatedColor),
            )
        }
    }
}

// ============================================================================
// 3. PROGRESS RINGS (Bosh sahifadagi 3 ta konsentrik aylana)
// ============================================================================

/**
 * Writing, Speaking va Vocabulary kunlik maqsadlarini ifodalovchi 3 ta konsentrik halqa.
 *
 * @param writingProgress Writing progressi (0.0f..1.0f).
 * @param speakingProgress Speaking progressi (0.0f..1.0f).
 * @param vocabProgress Vocabulary progressi (0.0f..1.0f).
 * @param modifier Tashqi modifikator.
 * @param size Diametr (standart 160.dp).
 * @param ringStroke Halqalar qalinligi (standart 10.dp).
 * @param ringSpacing Halqalar orasidagi bo'shliq (standart 4.dp).
 */
@Composable
fun ProgressRings(
    writingProgress: Float,
    speakingProgress: Float,
    vocabProgress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    ringStroke: Dp = 10.dp,
    ringSpacing: Dp = 4.dp,
    writingColor: Color = Color(0xFF1A6FE8), // Tashqi halqa — Moviy
    speakingColor: Color = Color(0xFF2ECC71), // O'rta halqa — Yashil
    vocabColor: Color = Color(0xFFF39C12),   // Ichki halqa — Qahrabo/Sariq
    trackAlpha: Float = 0.18f,
    centerContent: (@Composable () -> Unit)? = null,
) {
    // Silliq yuklanish animatsiyasi
    val animWriting by animateFloatAsState(
        targetValue = writingProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "anim_writing",
    )
    val animSpeaking by animateFloatAsState(
        targetValue = speakingProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "anim_speaking",
    )
    val animVocab by animateFloatAsState(
        targetValue = vocabProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
        label = "anim_vocab",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = ringStroke.toPx()
            val spacingPx = ringSpacing.toPx()
            val step = strokePx + spacingPx

            // 1. Tashqi halqa: Writing
            drawConcentricRing(
                progress = animWriting,
                color = writingColor,
                trackAlpha = trackAlpha,
                strokePx = strokePx,
                insetPx = strokePx / 2f,
            )

            // 2. O'rta halqa: Speaking
            drawConcentricRing(
                progress = animSpeaking,
                color = speakingColor,
                trackAlpha = trackAlpha,
                strokePx = strokePx,
                insetPx = strokePx / 2f + step,
            )

            // 3. Ichki halqa: Vocabulary
            drawConcentricRing(
                progress = animVocab,
                color = vocabColor,
                trackAlpha = trackAlpha,
                strokePx = strokePx,
                insetPx = strokePx / 2f + step * 2,
            )
        }

        // Markaziy kontent (masalan umumiy foiz yoki olov ikonkasi)
        if (centerContent != null) {
            centerContent()
        } else {
            val avgPercentage = ((animWriting + animSpeaking + animVocab) / 3f * 100).toInt()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "🎯",
                    fontSize = 20.sp,
                )
                Text(
                    text = "$avgPercentage%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF14211B),
                )
            }
        }
    }
}

private fun DrawScope.drawConcentricRing(
    progress: Float,
    color: Color,
    trackAlpha: Float,
    strokePx: Float,
    insetPx: Float,
) {
    val ringDiameter = size.width - insetPx * 2
    if (ringDiameter <= 0f) return

    val topLeft = Offset(insetPx, insetPx)
    val arcSize = Size(ringDiameter, ringDiameter)

    // Track
    drawArc(
        color = color.copy(alpha = trackAlpha),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
    )

    // Active progress arc
    if (progress > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}

// ============================================================================
// 4. KAHOOT OPTION BUTTON (Battle savolidagi 4 ta yirik rangli tugma)
// ============================================================================

enum class KahootShape {
    TRIANGLE, // Variant A (Qizil)
    DIAMOND,  // Variant B (Moviy)
    CIRCLE,   // Variant C (Sariq)
    SQUARE,   // Variant D (Yashil)
}

/**
 * Kahoot uslubidagi 4 xil variant tugmasi:
 * A: Qizil (▲ Uchburchak)
 * B: Moviy (◆ Olmos)
 * C: Sariq (● Doira)
 * D: Yashil (■ Kvadrat)
 *
 * @param index Variant indeksi (0=A, 1=B, 2=C, 3=D).
 * @param text Variant matni.
 * @param onClick Bosilganda ishga tushadigan harakat.
 * @param modifier Modifikator.
 * @param selected Foydalanuvchi ushbu variantni tanlaganmi.
 * @param isCorrectReveal Javob ochilganda to'g'ri ekanligi (null bo'lsa hali ochilmagan).
 * @param isWrongReveal Foydalanuvchi tanlagan noto'g'ri variant.
 * @param enabled Tugma bosilishi mumkinmi.
 */
@Composable
fun KahootOptionButton(
    index: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    isCorrectReveal: Boolean? = null,
    isWrongReveal: Boolean = false,
    enabled: Boolean = true,
) {
    val (shapeType, baseColor, shapeSymbol) = when (index % 4) {
        0 -> Triple(KahootShape.TRIANGLE, Color(0xFFE53935), "▲") // Red A
        1 -> Triple(KahootShape.DIAMOND, Color(0xFF1565C0), "◆")  // Blue B
        2 -> Triple(KahootShape.CIRCLE, Color(0xFFF9A825), "●")   // Yellow C
        else -> Triple(KahootShape.SQUARE, Color(0xFF2E7D32), "■")// Green D
    }

    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Bosilganda sezilarli haptic va scale (kichrayish) effekti
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "btn_scale",
    )

    // Reveal yoki tanlangan holatdagi orqa fon
    val backgroundColor = when {
        isCorrectReveal == true -> Color(0xFF2E7D32)
        isWrongReveal -> Color(0xFFB3261E)
        !enabled && !selected -> baseColor.copy(alpha = 0.55f)
        else -> baseColor
    }

    val borderColor = when {
        isCorrectReveal == true -> Color(0xFFC8F7DC)
        selected -> Color.White
        else -> Color.Transparent
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .scale(animatedScale)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = baseColor.copy(alpha = 0.4f),
                spotColor = baseColor.copy(alpha = 0.6f),
            )
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected || isCorrectReveal == true) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) {
                // Haptic feedback chaqiruvi
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Throwable) {}
                onClick()
            },
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Burchakdagi geometrik ikonka (Kahoot belgisi)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                KahootCornerIcon(
                    shape = shapeType,
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Savol varianti matni
            Text(
                text = text,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // Agar to'g'ri yoki xato bo'lsa natija belgisi
            if (isCorrectReveal == true) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            } else if (isWrongReveal) {
                Text(
                    text = "✕",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            } else if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.White, CircleShape),
                )
            }
        }
    }
}

/**
 * Kahoot geometrik ikonalarini Canvas yordamida aniq chizish.
 */
@Composable
private fun KahootCornerIcon(
    shape: KahootShape,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (shape) {
            KahootShape.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = path, color = color)
            }
            KahootShape.DIAMOND -> {
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h / 2f)
                    lineTo(w / 2f, h)
                    lineTo(0f, h / 2f)
                    close()
                }
                drawPath(path = path, color = color)
            }
            KahootShape.CIRCLE -> {
                drawCircle(color = color, radius = w / 2f)
            }
            KahootShape.SQUARE -> {
                drawRect(color = color, topLeft = Offset.Zero, size = Size(w, h))
            }
        }
    }
}

// ============================================================================
// PREVIEWS (Android Studio da darhol ko'rish uchun)
// ============================================================================

@Preview(name = "Countdown Arc Preview", showBackground = true)
@Composable
fun CountdownArcPreview() {
    InglishTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountdownArc(remainingSeconds = 15f, totalSeconds = 20f)
            CountdownArc(remainingSeconds = 4f, totalSeconds = 20f) // Qizil pulsatsiya
        }
    }
}

@Preview(name = "Live Waveform Preview", showBackground = true)
@Composable
fun LiveWaveformPreview() {
    InglishTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Gapirilayotganda (Recording):", fontSize = 12.sp, color = Color.Gray)
            LiveWaveform(isRecording = true)
            Text("To'xtatilganda (Idle):", fontSize = 12.sp, color = Color.Gray)
            LiveWaveform(isRecording = false)
        }
    }
}

@Preview(name = "Progress Rings Preview", showBackground = true)
@Composable
fun ProgressRingsPreview() {
    InglishTheme {
        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            ProgressRings(
                writingProgress = 0.80f,
                speakingProgress = 0.50f,
                vocabProgress = 0.70f,
            )
        }
    }
}

@Preview(name = "Kahoot Option Buttons Preview", showBackground = true)
@Composable
fun KahootOptionButtonPreview() {
    InglishTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KahootOptionButton(index = 0, text = "Everywhere, universal", onClick = {})
            KahootOptionButton(index = 1, text = "Very expensive and rare", onClick = {}, selected = true)
            KahootOptionButton(index = 2, text = "Temporary and fleeting", onClick = {})
            KahootOptionButton(index = 3, text = "Beautiful and aesthetic", onClick = {}, isCorrectReveal = true)
        }
    }
}
