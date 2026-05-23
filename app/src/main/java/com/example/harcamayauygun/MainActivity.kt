package com.example.harcamayauygun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harcamayauygun.ui.theme.HarcamayaUygunTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class CreditCard(val name: String, val cutOffDay: Int, val paymentDaysAfterCutoff: Int, val weekendShift: Boolean)

data class CardSummary(
    val card: CreditCard,
    val estimatedPaymentDate: LocalDate,
    val daysUntilDue: Int,
    val daysUntilStatement: Int,
    val inNewCycle: Boolean
)

object CardCalculator {
    fun calculateCardStatus(today: LocalDate, card: CreditCard): CardSummary {
        val currentStatement = statementDate(today, card.cutOffDay, card.weekendShift)
        val lastStatement = if (today.isBefore(currentStatement)) {
            statementDate(today.minusMonths(1), card.cutOffDay, card.weekendShift)
        } else {
            currentStatement
        }
        val nextStatement = statementDate(lastStatement.plusMonths(1), card.cutOffDay, card.weekendShift)
        val newCycleStart = lastStatement.plusDays(1)
        val inNewCycle = !today.isBefore(newCycleStart)

        val effectiveStatement = if (inNewCycle) nextStatement else currentStatement
        val dueDate = shiftToBusinessDay(effectiveStatement.plusDays(card.paymentDaysAfterCutoff.toLong()))

        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()
        val targetStatement = if (inNewCycle) nextStatement else currentStatement
        val daysUntilStatement = ChronoUnit.DAYS.between(today, targetStatement).toInt()

        return CardSummary(
            card = card,
            estimatedPaymentDate = dueDate,
            daysUntilDue = maxOf(0, daysUntilDue),
            daysUntilStatement = maxOf(0, daysUntilStatement),
            inNewCycle = inNewCycle
        )
    }

    fun selectBestCard(summaries: List<CardSummary>): CardSummary? {
        if (summaries.isEmpty()) return null
        val newCycleCards = summaries.filter { it.inNewCycle }
        val candidates = newCycleCards.ifEmpty { summaries }
        return candidates.maxByOrNull { it.daysUntilDue }
    }

    private fun statementDate(date: LocalDate, day: Int, weekendShift: Boolean): LocalDate {
        val actualDay = minOf(day, YearMonth.from(date).lengthOfMonth())
        val stDate = LocalDate.of(date.year, date.month, actualDay)
        return if (weekendShift) shiftToBusinessDay(stDate) else stDate
    }

    private fun shiftToBusinessDay(date: LocalDate): LocalDate {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.plusDays(2)
            DayOfWeek.SUNDAY -> date.plusDays(1)
            else -> date
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HarcamayaUygunTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                        MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var cards by remember {
        mutableStateOf(
            listOf(
                CreditCard("Örnek Kart A", 10, 40, true),
                CreditCard("Örnek Kart B", 20, 40, true)
            )
        )
    }
    var showDialog by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val summaries = cards.map { CardCalculator.calculateCardStatus(today, it) }
    val bestCardSummary = CardCalculator.selectBestCard(summaries)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Harcamaya Uygun", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Kart Ekle")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OfflineNoticeCard()

                if (bestCardSummary != null) {
                    Text("Bugün Harcama Yapılması Gereken Kart", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HeroCardItem(summary = bestCardSummary)
                    
                    val others = summaries.filter { it.card.name != bestCardSummary.card.name }
                    if (others.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Diğer Kartlarınızın Ekstre Bilgisi", fontWeight = FontWeight.SemiBold)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(others) { summary ->
                                CardItem(summary)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Henüz kart eklemediniz. Sağ alttan ekleyebilirsiniz.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddCardDialog(
            onDismiss = { showDialog = false },
            onAddCard = { name, cutOff, paymentDays, weekendShift ->
                cards = cards + CreditCard(name, cutOff, paymentDays, weekendShift)
                showDialog = false
            }
        )
    }
}

@Composable
fun OfflineNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gizlilik Odaklı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verileriniz yalnızca bu cihazda saklanır. Ağ bağlantısı veya sunucu iletişimi kullanılmaz.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun HeroCardItem(summary: CardSummary) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = summary.card.name, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Tahmini Son Ödeme: ${summary.estimatedPaymentDate.format(formatter)}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Bu kartla bugün harcama yaparsanız yaklaşık ${summary.daysUntilDue} gün vade sağlarsınız.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Ekstre kesim günü: ${summary.card.cutOffDay}. Son ödeme süresi: ${summary.card.paymentDaysAfterCutoff} gün.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun CardItem(summary: CardSummary) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("tr"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = summary.card.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Sonraki Ekstre: ${summary.daysUntilStatement} gün kaldı", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Son Ödeme", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(text = summary.estimatedPaymentDate.format(formatter), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AddCardDialog(onDismiss: () -> Unit, onAddCard: (String, Int, Int, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cutOffDay by remember { mutableStateOf("") }
    var paymentDays by remember { mutableStateOf("") }
    var weekendShift by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Kart Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kart Adı (Örn: Maximum)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = cutOffDay,
                    onValueChange = { cutOffDay = it },
                    label = { Text("Hesap Kesim Günü (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = paymentDays,
                    onValueChange = { paymentDays = it },
                    label = { Text("Son Ödeme (Ekstreden kaç gün sonra)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = weekendShift, onCheckedChange = { weekendShift = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hafta sonuna gelirse sonraki iş gününe ertele", fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cutOff = cutOffDay.toIntOrNull() ?: 1
                    val payment = paymentDays.toIntOrNull() ?: 1
                    onAddCard(name, cutOff, payment, weekendShift)
                },
                enabled = name.isNotBlank() && cutOffDay.isNotBlank() && paymentDays.isNotBlank()
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
