package com.example.harcamayauygun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.util.UUID

data class CreditCard(val id: String = UUID.randomUUID().toString(), val name: String, val cutOffDay: Int, val dueDays: Int = 10, val weekendShift: Boolean)

data class CardSummary(
    val card: CreditCard,
    val daysUntilDue: Int,
    val daysUntilStatement: Int,
    val nextStatementDate: LocalDate,
    val dueDate: LocalDate
)

object CardCalculator {
    fun calculateCardStatus(today: LocalDate, card: CreditCard): CardSummary {
        val actualDay = minOf(card.cutOffDay, today.lengthOfMonth())
        val currentMonthCutOff = LocalDate.of(today.year, today.month, actualDay)

        // Yeni dönemin hangi gün başladığını hesaplıyoruz
        val cycleStart = calculateCycleStart(currentMonthCutOff, card.weekendShift)

        // Eğer bugün, yeni dönemin başlangıcından önceyse (veya eski dönem devam ediyorsa)
        val inNewCycle = !today.isBefore(cycleStart)

        val nextStatementDate = if (inNewCycle) {
            val nextMonth = today.plusMonths(1)
            val nextActualDay = minOf(card.cutOffDay, nextMonth.lengthOfMonth())
            LocalDate.of(nextMonth.year, nextMonth.month, nextActualDay)
        } else {
            currentMonthCutOff
        }

        val daysUntilStatement = ChronoUnit.DAYS.between(today, nextStatementDate).toInt()
        val dueDate = nextStatementDate.plusDays(card.dueDays.toLong())
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()

        return CardSummary(
            card = card,
            daysUntilDue = maxOf(0, daysUntilDue),
            daysUntilStatement = maxOf(0, daysUntilStatement),
            nextStatementDate = nextStatementDate,
            dueDate = dueDate
        )
    }

    fun selectBestCard(summaries: List<CardSummary>): CardSummary? {
        if (summaries.isEmpty()) return null
        // En avantajlı kart, bir sonraki ekstresinin kesilmesine en uzun süre olan karttır.
        return summaries.maxByOrNull { it.daysUntilStatement }
    }

    private fun calculateCycleStart(cutOffDate: LocalDate, weekendShift: Boolean): LocalDate {
        if (!weekendShift) return cutOffDate.plusDays(1) // Kaydırma kapalıysa ertesi gün başlar
        
        return when (cutOffDate.dayOfWeek) {
            DayOfWeek.SATURDAY -> cutOffDate.plusDays(2) // Cumartesi ise Pazartesi başlar
            DayOfWeek.SUNDAY -> cutOffDate.plusDays(1)   // Pazar ise Pazartesi başlar
            else -> cutOffDate.plusDays(1)               // Hafta içi ise ertesi gün başlar
        }
    }
}

object CardStorage {
    private const val PREFS_NAME = "harcamaya_uygun_prefs"
    private const val KEY_CARDS = "saved_cards"

    fun saveCards(context: Context, cards: List<CreditCard>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        cards.forEach { card ->
            val obj = JSONObject().apply {
                put("id", card.id)
                put("name", card.name)
                put("cutOffDay", card.cutOffDay)
                put("dueDays", card.dueDays)
                put("weekendShift", card.weekendShift)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CARDS, array.toString()).apply()
    }

    fun loadCards(context: Context): List<CreditCard> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_CARDS, null) ?: return emptyList()
        val cards = mutableListOf<CreditCard>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                    cards.add(CreditCard(id, obj.getString("name"), obj.getInt("cutOffDay"), obj.optInt("dueDays", 10), obj.getBoolean("weekendShift")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cards
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("is_dark", false)) }

            HarcamayaUygunTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            isDarkTheme = !isDarkTheme
                            prefs.edit().putBoolean("is_dark", isDarkTheme).apply()
                        }
                    )
                }
            }
        }
    }
}

enum class AppScreen { Dashboard, ManageCards }

@Composable
fun MainScreen(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val context = LocalContext.current
    var cards by remember { mutableStateOf(listOf<CreditCard>()) }
    var isLoaded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

    LaunchedEffect(Unit) {
        val savedCards = CardStorage.loadCards(context)
        if (savedCards.isEmpty()) {
            cards = listOf(CreditCard(name = "Örnek Kart A", cutOffDay = 10, dueDays = 10, weekendShift = true), CreditCard(name = "Örnek Kart B", cutOffDay = 20, dueDays = 15, weekendShift = true))
            CardStorage.saveCards(context, cards)
        } else {
            cards = savedCards
        }
        isLoaded = true
    }

    if (!isLoaded) return

    when (currentScreen) {
        AppScreen.Dashboard -> DashboardScreen(
            cards = cards,
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            onNavigateManage = { currentScreen = AppScreen.ManageCards }
        )
        AppScreen.ManageCards -> ManageCardsScreen(
            cards = cards,
            onBack = { currentScreen = AppScreen.Dashboard },
            onCardsUpdated = { newCards ->
                cards = newCards
                CardStorage.saveCards(context, newCards)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    cards: List<CreditCard>,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateManage: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val summaries = cards.map { CardCalculator.calculateCardStatus(today, it) }
    val bestCardSummary = CardCalculator.selectBestCard(summaries)
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tavsiye Kart", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Hakkında")
                    }
                    TextButton(onClick = onThemeToggle) {
                        Text(if (isDarkTheme) "☀️" else "🌙", fontSize = 20.sp)
                    }
                    IconButton(onClick = onNavigateManage) {
                        Icon(Icons.Filled.List, contentDescription = "Kartları Yönet")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
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

                if (bestCardSummary != null) {
                    Text("Bugün Harcama Yapılması Gereken Kart", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HeroCardItem(summary = bestCardSummary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateManage),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tüm kartları görüntüle ve düzenle", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Icon(Icons.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Henüz kart eklemediniz.", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onNavigateManage) {
                                Text("Kartları Yönet")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCardsScreen(
    cards: List<CreditCard>,
    onBack: () -> Unit,
    onCardsUpdated: (List<CreditCard>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    val today = remember { LocalDate.now() }
    val summaries = cards.map { CardCalculator.calculateCardStatus(today, it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kartlarım", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingCard = null; showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Kart Ekle")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summaries, key = { it.card.id }) { summary ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            onCardsUpdated(cards.filter { c -> c.id != summary.card.id })
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color, MaterialTheme.shapes.medium)
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.onError)
                        }
                    },
                    content = {
                        CardItem(summary = summary, onClick = {
                            editingCard = summary.card
                            showDialog = true
                        })
                    }
                )
            }
        }
    }

    if (showDialog) {
        CardFormDialog(
            cardToEdit = editingCard,
            onDismiss = { showDialog = false; editingCard = null },
            onSave = { name, cutOff, dueDays, weekendShift ->
                val newCards = if (editingCard != null) {
                    cards.map { if (it.id == editingCard!!.id) it.copy(name = name, cutOffDay = cutOff, dueDays = dueDays, weekendShift = weekendShift) else it }
                } else {
                    cards + CreditCard(name = name, cutOffDay = cutOff, dueDays = dueDays, weekendShift = weekendShift)
                }
                onCardsUpdated(newCards)
                showDialog = false
                editingCard = null
            }
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hakkında") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(
                        text = "Gizlilik Odaklı",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Verileriniz yalnızca bu cihazda saklanır. Ağ bağlantısı veya sunucu iletişimi kullanılmaz.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider()
                Text(
                    text = "Bu uygulama, kredi kartı ekstre tarihlerinizi hesaplayarak harcamalarınız için en uzun faizsiz öteleme süresini sağlayacak olan en avantajlı kartı size önerir.",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                Text(
                    text = "Github: Semih007",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@Composable
fun HeroCardItem(summary: CardSummary) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("tr"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = summary.card.name, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Sonraki Ekstre: ${summary.nextStatementDate.format(formatter)}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Son Ödeme Tarihi: ${summary.dueDate.format(formatter)}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun CardItem(summary: CardSummary, onClick: () -> Unit = {}) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("tr"))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Text(text = "Kesim Günü: ${summary.card.cutOffDay}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${summary.daysUntilStatement} gün kaldı", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = summary.nextStatementDate.format(formatter), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun CardFormDialog(
    cardToEdit: CreditCard?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(cardToEdit?.name ?: "") }
    var cutOffDay by remember { mutableStateOf(cardToEdit?.cutOffDay?.toString() ?: "") }
    var dueDays by remember { mutableStateOf(cardToEdit?.dueDays?.toString() ?: "") }
    var weekendShift by remember { mutableStateOf(cardToEdit?.weekendShift ?: true) }

    val title = if (cardToEdit != null) "Kartı Düzenle" else "Yeni Kart Ekle"
    val buttonText = if (cardToEdit != null) "Kaydet" else "Ekle"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                    value = dueDays,
                    onValueChange = { dueDays = it },
                    label = { Text("Son Ödeme Süresi (Gün)") },
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
                    val due = dueDays.toIntOrNull() ?: 10
                    onSave(name, cutOff, due, weekendShift)
                },
                enabled = name.isNotBlank() && cutOffDay.isNotBlank() && dueDays.isNotBlank()
            ) {
                Text(buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
