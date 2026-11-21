package uz.inglish.app.feature.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.inglish.app.core.network.dto.BillingProductDto
import uz.inglish.app.core.network.dto.EntitlementDto
import uz.inglish.app.ui.components.ErrorCard
import uz.inglish.app.ui.components.InglishGreen
import uz.inglish.app.ui.components.InglishInk
import uz.inglish.app.ui.components.InglishMuted
import uz.inglish.app.ui.components.LoadingState
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    state: PaywallUiState,
    onPurchase: (Activity, BillingProductDto) -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
    onClearMessages: () -> Unit,
) {
    val activity = LocalContext.current as? Activity

    Scaffold(
        containerColor = Color(0xFFF7F3EA),
        topBar = {
            TopAppBar(
                title = { Text("Obuna", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F3EA),
                    titleContentColor = InglishInk,
                    navigationIconContentColor = InglishInk,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CurrentPlanCard(state.entitlement)
            }

            if (state.error != null) {
                item { ErrorCard(state.error, onRetry = onClearMessages) }
            }

            if (state.message != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EE)),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                state.message,
                                color = InglishGreen,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onClearMessages) { Text("OK") }
                        }
                    }
                }
            }

            item {
                Text(
                    "Rejalar",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InglishInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            when {
                state.loading -> item { LoadingState("Rejalar yuklanmoqda…") }

                state.catalog.isEmpty() -> item {
                    Text(
                        "Hozircha rejalar mavjud emas.",
                        color = InglishMuted,
                        fontSize = 13.sp,
                    )
                }

                else -> items(state.catalog.size) { index ->
                    val product = state.catalog[index]
                    ProductCard(
                        product = product,
                        price = state.playProducts[product.productId]?.price,
                        purchasing = state.purchasingProductId == product.productId,
                        disabled = state.purchasingProductId != null && state.purchasingProductId != product.productId,
                        onPurchase = { activity?.let { onPurchase(it, product) } },
                    )
                }
            }

            item {
                BenefitsCard()
            }

            item {
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !state.restoring,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (state.restoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = InglishGreen,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Xaridlarni tiklash")
                }
            }

            if (state.mockBuild) {
                item {
                    Text(
                        "Debug rejimi: to'lov Play Billing orqali emas, serverning test tokeni bilan amalga oshiriladi.",
                        fontSize = 10.sp,
                        color = InglishMuted,
                    )
                }
            }

            item {
                Text(
                    "Obuna avtomatik yangilanadi. Bekor qilish uchun Google Play → To'lovlar va obunalar bo'limidan foydalaning. " +
                        "Bekor qilinsa ham to'langan davr oxirigacha kirish saqlanadi.",
                    fontSize = 10.sp,
                    color = InglishMuted,
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CurrentPlanCard(entitlement: EntitlementDto?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = InglishGreen),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Joriy reja", color = Color(0xFFBFD8CC), fontSize = 13.sp)
            Text(
                planLabel(entitlement?.plan),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )

            val expiresAt = entitlement?.expiresAt
            if (!expiresAt.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Amal qiladi: ${formatDate(expiresAt)}",
                    color = Color(0xFFE8F3ED),
                    fontSize = 13.sp,
                )
            }

            entitlement?.limits?.let { limits ->
                Spacer(Modifier.height(10.dp))
                Text(
                    "Speaking: ${limits.speakingPerDay}/kun · Writing: ${limits.writingPerMonth}/oy",
                    color = Color(0xFFE8F3ED),
                    fontSize = 12.sp,
                )
            }

            if (entitlement?.autoRenewing == true) {
                Spacer(Modifier.height(4.dp))
                Text("Avtomatik yangilanadi", color = Color(0xFFBFD8CC), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: BillingProductDto,
    price: String?,
    purchasing: Boolean,
    disabled: Boolean,
    onPurchase: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD7E8DF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = product.plan.take(1),
                        fontWeight = FontWeight.Bold,
                        color = InglishGreen,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        product.title.ifBlank { product.productId },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = InglishInk,
                    )
                    Text(product.description, color = InglishMuted, fontSize = 12.sp)
                }
                if (!price.isNullOrBlank()) {
                    Text(
                        price,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InglishGreen,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onPurchase,
                enabled = !purchasing && !disabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (purchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (purchasing) "Tasdiqlanmoqda…" else "Obuna bo'lish")
            }
        }
    }
}

@Composable
private fun BenefitsCard() {
    val benefits = listOf(
        "Speaking: 15 ta/kun (Pro) yoki 30 ta/kun (Teacher)",
        "Writing: 4 ta/oy (Pro) yoki 10 ta/oy (Teacher)",
        "Battle: 10 o'yinchi (Pro) yoki 30 o'yinchi (Teacher)",
        "Sinf xonalari va ustoz rejimi (Teacher)",
        "Reklamasiz va ustuvor AI baholash",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6DD)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Nima kiradi?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = InglishInk)
            benefits.forEach { benefit ->
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", color = InglishGreen, fontSize = 12.sp)
                    Text(benefit, fontSize = 12.sp, color = InglishMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEDE7DA))
            Spacer(Modifier.height(8.dp))
            Text(
                "To'lov Google Play orqali xavfsiz amalga oshiriladi.",
                fontSize = 10.sp,
                color = InglishMuted,
            )
        }
    }
}

private fun planLabel(plan: String?): String = when (plan) {
    "PRO" -> "Pro"
    "TEACHER" -> "Teacher"
    "FREE" -> "Bepul"
    else -> "Bepul"
}

private fun formatDate(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}.getOrElse { iso.take(10) }
