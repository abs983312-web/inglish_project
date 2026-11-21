package uz.inglish.app.feature.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.core.network.dto.VocabularyItemDto
import uz.inglish.app.core.network.dto.VocabularyStatsDto
import uz.inglish.app.ui.components.EmptyState
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.LoadingState
import uz.inglish.app.ui.components.StatusPill

@Composable
fun VocabularyScreen(
    state: VocabularyUiState,
    onRetry: () -> Unit,
    onSetTab: (Int) -> Unit,
    onReveal: () -> Unit,
    onRate: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectSet: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onClearError: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Lug'at", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InglishInk)
            Text(
                "FSRS takrorlash tizimi bilan so'zlarni eslab qoling.",
                fontSize = 13.sp,
                color = InglishMuted,
            )
            Spacer(Modifier.height(8.dp))
            state.stats?.let { StatsRow(it) }
        }

        if (state.error != null) {
            item { ErrorCard(state.error, onClearError) }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.tab == 0) {
                    Button(onClick = { onSetTab(0) }, shape = RoundedCornerShape(50)) {
                        Text("Takrorlash", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(onClick = { onSetTab(0) }, shape = RoundedCornerShape(50)) {
                        Text("Takrorlash", fontSize = 12.sp, color = InglishGreen)
                    }
                }
                if (state.tab == 1) {
                    Button(onClick = { onSetTab(1) }, shape = RoundedCornerShape(50)) {
                        Text("Lug'at ro'yxati", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(onClick = { onSetTab(1) }, shape = RoundedCornerShape(50)) {
                        Text("Lug'at ro'yxati", fontSize = 12.sp, color = InglishGreen)
                    }
                }
            }
        }

        if (state.loading) {
            item { LoadingState("So'zlar yuklanmoqda…") }
        } else if (state.tab == 0) {
            if (state.sessionDone) {
                item {
                    EmptyState(
                        title = if (state.reviewedCount > 0) "Takrorlash tugadi 🎉" else "Takrorlash uchun so'z yo'q",
                        message = if (state.reviewedCount > 0) {
                            "Bugun ${state.reviewedCount} ta so'zni takrorladingiz. Yangi so'zlar keyingi sessiyada qo'shiladi."
                        } else {
                            "Barcha so'zlar rejalashtirilgan. Keyinroq qaytib tekshiring yoki lug'at ro'yxatini ko'ring."
                        },
                    )
                }
                item {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Yangilash")
                    }
                }
            } else {
                val current = state.current
                if (current != null) {
                    item {
                        Text(
                            text = "${state.index + 1} / ${state.due.size}",
                            fontSize = 12.sp,
                            color = InglishMuted,
                        )
                    }
                    item {
                        ReviewCard(
                            item = current,
                            revealed = state.revealed,
                            reviewing = state.reviewing,
                            onReveal = onReveal,
                            onRate = onRate,
                        )
                    }
                }
            }
        } else {
            item {
                OutlinedTextField(
                    value = state.listQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("So'z qidirish") },
                    singleLine = true,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        SetChip(
                            label = "Barchasi",
                            selected = state.selectedSet == null,
                            onClick = { onSelectSet(null) },
                        )
                    }
                    items(state.sets, key = { it.slug }) { set ->
                        SetChip(
                            label = "${set.title} (${set.count})",
                            selected = state.selectedSet == set.tag,
                            onClick = { onSelectSet(set.tag) },
                        )
                    }
                }
            }
            if (state.listLoading && state.list.isEmpty()) {
                item { LoadingState("Qidirilmoqda…") }
            } else if (state.list.isEmpty()) {
                item {
                    EmptyState(
                        title = "So'z topilmadi",
                        message = "Boshqa so'z yoki bo'lim tanlab ko'ring.",
                    )
                }
            } else {
                item {
                    Text(
                        text = "${state.list.size} / ${state.listTotal} so'z",
                        fontSize = 12.sp,
                        color = InglishMuted,
                    )
                }
                items(state.list, key = { it.id }) { item ->
                    DictionaryRow(item)
                }
                if (state.listHasMore) {
                    item {
                        OutlinedButton(
                            onClick = onLoadMore,
                            enabled = !state.listLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(if (state.listLoading) "Yuklanmoqda…" else "Ko'proq yuklash")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun StatsRow(stats: VocabularyStatsDto) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBox("Jami", stats.total, Modifier.weight(1f))
        StatBox("Takrorlash", stats.due, Modifier.weight(1f))
        StatBox("O'rganish", stats.learning, Modifier.weight(1f))
        StatBox("Yangi", stats.newCount, Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InglishGreen)
            Text(label, fontSize = 9.sp, color = InglishMuted, maxLines = 1)
        }
    }
}

@Composable
private fun ReviewCard(
    item: VocabularyItemDto,
    revealed: Boolean,
    reviewing: Boolean,
    onReveal: () -> Unit,
    onRate: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.cefr?.let { StatusPill(it) }
                if (!item.pos.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    StatusPill(item.pos, color = Color(0xFF6B5B95))
                }
                Spacer(Modifier.weight(1f))
                StatusPill(stateLabel(item.state.state), color = stateColor(item.state.state))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = item.word,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = InglishInk,
            )

            if (!revealed) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onReveal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Ma'nosini ko'rsatish")
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(item.definitionUz, fontSize = 15.sp, color = InglishInk)
                if (!item.definitionEn.isNullOrBlank()) {
                    Text(item.definitionEn, fontSize = 13.sp, color = InglishMuted)
                }
                if (!item.exampleEn.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("“${item.exampleEn}”", fontSize = 13.sp, color = InglishGreen)
                }
                if (!item.exampleUz.isNullOrBlank()) {
                    Text(item.exampleUz, fontSize = 12.sp, color = InglishMuted)
                }
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(item.tags.joinToString(" · "), fontSize = 10.sp, color = InglishMuted)
                }

                Spacer(Modifier.height(18.dp))
                Text("Qanchalik oson esladingiz?", fontSize = 12.sp, color = InglishMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatingButton("Qiyin", !reviewing, Modifier.weight(1f)) { onRate(1) }
                    RatingButton("O'rtacha", !reviewing, Modifier.weight(1f)) { onRate(2) }
                    RatingButton("Yaxshi", !reviewing, Modifier.weight(1f), filled = true) { onRate(3) }
                    RatingButton("Oson", !reviewing, Modifier.weight(1f)) { onRate(4) }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(label, fontSize = 10.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(label, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(label, fontSize = 11.sp, maxLines = 1, color = InglishGreen)
        }
    }
}

@Composable
private fun DictionaryRow(item: VocabularyItemDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.word, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InglishInk)
                    item.cefr?.let {
                        Spacer(Modifier.width(6.dp))
                        StatusPill(it)
                    }
                }
                Text(item.definitionUz, fontSize = 12.sp, color = InglishMuted)
            }
            StatusPill(stateLabel(item.state.state), color = stateColor(item.state.state))
        }
    }
}

private fun stateLabel(state: String): String = when (state) {
    "NEW" -> "Yangi"
    "LEARNING" -> "O'rganish"
    "RELEARNING" -> "Qayta"
    "REVIEW" -> "Takror"
    else -> state
}

private fun stateColor(state: String): Color = when (state) {
    "LEARNING", "RELEARNING" -> Color(0xFF9A6B00)
    "REVIEW" -> Color(0xFF1F4E79)
    else -> InglishGreen
}
