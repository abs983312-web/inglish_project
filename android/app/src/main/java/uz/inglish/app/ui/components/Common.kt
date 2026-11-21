package uz.inglish.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.core.network.dto.CorrectionDto
import uz.inglish.app.core.network.dto.CriterionDto

val InglishGreen = Color(0xFF0F3D2E)
val InglishSand = Color(0xFFF7F3EA)
val InglishInk = Color(0xFF14211B)
val InglishMuted = Color(0xFF5A665F)
val InglishGold = Color(0xFFF4C95D)

@Composable
fun LoadingState(message: String = "Yuklanmoqda…") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(message, color = InglishMuted, fontSize = 13.sp)
    }
}

@Composable
fun ErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1EE)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Qayta urinish")
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InglishInk)
            Spacer(Modifier.height(4.dp))
            Text(message, color = InglishMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = InglishInk,
    )
}

@Composable
fun StatusPill(text: String, color: Color = InglishGreen) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

fun formatBand(value: Double?): String = uz.inglish.app.core.formatBand(value)

fun criterionLabel(name: String): String = uz.inglish.app.core.criterionLabel(name)

@Composable
fun AiFeedbackCard(
    band: Double?,
    confidenceLow: Double?,
    confidenceHigh: Double?,
    criteria: List<CriterionDto>,
    strengths: List<String>,
    improvements: List<String>,
    corrections: List<CorrectionDto>,
    nextStep: String,
    pronunciationNote: String? = null,
    disclaimer: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("AI bahosi", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = InglishInk)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatBand(band),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = InglishGreen,
                )
                Text(
                    text = " band",
                    fontSize = 14.sp,
                    color = InglishMuted,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
                if (confidenceLow != null && confidenceHigh != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "± ${formatBand(confidenceLow)}–${formatBand(confidenceHigh)}",
                        fontSize = 12.sp,
                        color = InglishMuted,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }

            if (criteria.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                criteria.forEach { criterion ->
                    HorizontalDivider(color = Color(0xFFEDE7DA))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                criterionLabel(criterion.name),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = InglishInk,
                            )
                            if (criterion.comment.isNotBlank()) {
                                Text(criterion.comment, fontSize = 12.sp, color = InglishMuted)
                            }
                        }
                        Text(
                            text = formatBand(criterion.band),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = InglishGreen,
                        )
                    }
                }
            }

            FeedbackList("Kuchli tomonlar", strengths)
            FeedbackList("Yaxshilash kerak", improvements)

            if (corrections.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Tuzatishlar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InglishInk)
                corrections.forEach { correction ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "“${correction.original}” → “${correction.corrected}”",
                        fontSize = 12.sp,
                        color = InglishInk,
                    )
                    Text(correction.explanation, fontSize = 12.sp, color = InglishMuted)
                }
            }

            if (nextStep.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EE)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Keyingi qadam", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InglishGreen)
                        Text(nextStep, fontSize = 12.sp, color = InglishInk)
                    }
                }
            }

            if (!pronunciationNote.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("Talaffuz", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InglishInk)
                Text(pronunciationNote, fontSize = 12.sp, color = InglishMuted)
            }

            if (!disclaimer.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(disclaimer, fontSize = 10.sp, color = InglishMuted)
            }
        }
    }
}

@Composable
private fun FeedbackList(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InglishInk)
    items.forEach { item ->
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("•", color = InglishGreen, fontSize = 12.sp)
            Text(item, fontSize = 12.sp, color = InglishMuted)
        }
    }
}
