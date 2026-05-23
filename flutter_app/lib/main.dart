import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const HarcamayaUygunApp());
}

class HarcamayaUygunApp extends StatefulWidget {
  const HarcamayaUygunApp({super.key});

  @override
  State<HarcamayaUygunApp> createState() => _HarcamayaUygunAppState();
}

class _HarcamayaUygunAppState extends State<HarcamayaUygunApp> {
  bool _darkMode = false;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Harcamaya Uygun',
      theme: ThemeData(
        brightness: Brightness.light,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal, brightness: Brightness.dark),
        useMaterial3: true,
      ),
      themeMode: _darkMode ? ThemeMode.dark : ThemeMode.light,
      home: HomePage(
        darkMode: _darkMode,
        onThemeToggle: () => setState(() => _darkMode = !_darkMode),
      ),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.darkMode, required this.onThemeToggle});

  final bool darkMode;
  final VoidCallback onThemeToggle;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<CreditCardModel> cards = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadCards();
  }

  Future<void> _loadCards() async {
    cards = await CardStorage.loadCards();
    if (cards.isEmpty) {
      cards = CardStorage.defaultCards();
      await CardStorage.saveCards(cards);
    }
    setState(() {
      loading = false;
    });
  }

  Future<void> _saveCards() async {
    await CardStorage.saveCards(cards);
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final summaries = cards
        .map((card) => CardCalculator.calculateCardStatus(now, card))
        .toList();

    final bestCard = CardCalculator.selectBestCard(summaries);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Harcamaya Uygun Kart Seçimi'),
        actions: [
          IconButton(
            icon: Icon(widget.darkMode ? Icons.wb_sunny : Icons.nightlight_round),
            onPressed: widget.onThemeToggle,
            tooltip: 'Tema Değiştir',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddCardDialog,
        child: const Icon(Icons.add),
        tooltip: 'Kart ekle',
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
              child: Column(
                children: [
                  if (bestCard != null) _buildHeroCard(bestCard),
                  const SizedBox(height: 20),
                  _buildSectionHeader('Diğer kartların ekstre bilgisi'),
                  Expanded(
                    child: ListView.builder(
                      itemCount: summaries.length,
                      itemBuilder: (context, index) {
                        final summary = summaries[index];
                        return _buildSummaryTile(summary, bestCard?.id == summary.id);
                      },
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildHeroCard(CardSummary summary) {
    final theme = Theme.of(context);
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: widget.darkMode
                ? [Colors.teal.shade200, Colors.teal.shade700]
                : [Colors.teal.shade300, Colors.teal.shade800],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(18),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Bugün Harcama Yapmanız Gereken En Avantajlı Kart',
                style: theme.textTheme.bodyLarge?.copyWith(color: Colors.white70)),
            const SizedBox(height: 14),
            Text(summary.cardName,
                style: theme.textTheme.headlineLarge?.copyWith(color: Colors.white, fontWeight: FontWeight.bold)),
            const SizedBox(height: 14),
            Text('Tahmini son ödeme tarihi: ${DateFormat('d MMMM y', 'tr').format(summary.estimatedPaymentDate)}',
                style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white70)),
            const SizedBox(height: 8),
            Text('Bu kartla bugün harcama yaparsanız yaklaşık ${summary.daysUntilDue} gün vade sağlarsınız.',
                style: theme.textTheme.bodyLarge?.copyWith(color: Colors.white)),
            const SizedBox(height: 8),
            Text('Ekstre kesim günü: ${summary.statementDay}. Son ödeme süresi: ${summary.dueDays} gün.',
                style: theme.textTheme.bodySmall?.copyWith(color: Colors.white70)),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12.0),
        child: Text(title, style: Theme.of(context).textTheme.titleMedium),
      ),
    );
  }

  Widget _buildSummaryTile(CardSummary summary, bool selected) {
    final label = selected ? 'Önerilen Kart' : 'Alternatif Kart';
    final remainingStatementDays = summary.daysUntilStatement;
    final progressValue = 1.0 - (remainingStatementDays / 31.0).clamp(0.0, 1.0);
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 8),
      elevation: selected ? 6 : 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: ListTile(
        tileColor: selected ? Colors.teal.shade50 : null,
        title: Text(summary.cardName, style: TextStyle(fontWeight: selected ? FontWeight.w700 : FontWeight.w600)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 8),
            Text('${summary.statementDay}. gün ekstre kesim gününe ${remainingStatementDays} gün kaldı.'),
            const SizedBox(height: 4),
            Text('Tahmini ödeme: ${DateFormat('d MMMM y', 'tr').format(summary.estimatedPaymentDate)}'),
            const SizedBox(height: 8),
            LinearProgressIndicator(value: progressValue, minHeight: 6),
          ],
        ),
        trailing: selected ? const Icon(Icons.star, color: Colors.amber) : null,
      ),
    );
  }

  Future<void> _showAddCardDialog() async {
    final result = await showDialog<CreditCardModel>(
      context: context,
      builder: (context) => const AddCardDialog(),
    );
    if (result != null) {
      setState(() {
        cards.add(result);
      });
      await _saveCards();
    }
  }
}

class AddCardDialog extends StatefulWidget {
  const AddCardDialog({super.key});

  @override
  State<AddCardDialog> createState() => _AddCardDialogState();
}

class _AddCardDialogState extends State<AddCardDialog> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _statementDayController = TextEditingController();
  final TextEditingController _dueDaysController = TextEditingController();
  bool _weekendShift = true;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Kart Ekle'),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Kart adı'),
                validator: (value) => value == null || value.isEmpty ? 'Kart adı girin' : null,
              ),
              TextFormField(
                controller: _statementDayController,
                decoration: const InputDecoration(labelText: 'Ekstre kesim günü'),
                keyboardType: TextInputType.number,
                validator: (value) {
                  final day = int.tryParse(value ?? '');
                  if (day == null || day < 1 || day > 31) {
                    return '1-31 arası bir gün girin';
                  }
                  return null;
                },
              ),
              TextFormField(
                controller: _dueDaysController,
                decoration: const InputDecoration(labelText: 'Son ödeme günü (ekstreden kaç gün sonra)'),
                keyboardType: TextInputType.number,
                validator: (value) {
                  final days = int.tryParse(value ?? '');
                  if (days == null || days < 1 || days > 70) {
                    return '1-70 arası bir sayı girin';
                  }
                  return null;
                },
              ),
              SwitchListTile(
                value: _weekendShift,
                onChanged: (value) => setState(() => _weekendShift = value),
                title: const Text('Hafta sonu ekstre kesimini sonraki iş gününe kaydır'),
                subtitle: const Text('True ise ekstre hafta sonuna denk gelirse bir sonraki iş günü 00:00’da kesilmiş kabul edilir.'),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('İptal')),
        ElevatedButton(
          onPressed: () {
            if (_formKey.currentState?.validate() ?? false) {
              final model = CreditCardModel(
                id: DateTime.now().millisecondsSinceEpoch.toString(),
                name: _nameController.text.trim(),
                statementDay: int.parse(_statementDayController.text.trim()),
                dueDays: int.parse(_dueDaysController.text.trim()),
                weekendShift: _weekendShift,
              );
              Navigator.of(context).pop(model);
            }
          },
          child: const Text('Kaydet'),
        ),
      ],
    );
  }
}

class CreditCardModel {
  CreditCardModel({
    required this.id,
    required this.name,
    required this.statementDay,
    required this.dueDays,
    required this.weekendShift,
  });

  final String id;
  final String name;
  final int statementDay;
  final int dueDays;
  final bool weekendShift;

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'statementDay': statementDay,
        'dueDays': dueDays,
        'weekendShift': weekendShift,
      };

  factory CreditCardModel.fromJson(Map<String, dynamic> json) => CreditCardModel(
        id: json['id'] as String,
        name: json['name'] as String,
        statementDay: json['statementDay'] as int,
        dueDays: json['dueDays'] as int,
        weekendShift: json['weekendShift'] as bool,
      );
}

class CardSummary {
  CardSummary({
    required this.id,
    required this.cardName,
    required this.statementDay,
    required this.dueDays,
    required this.estimatedPaymentDate,
    required this.daysUntilDue,
    required this.daysUntilStatement,
    required this.inNewCycle,
  });

  final String id;
  final String cardName;
  final int statementDay;
  final int dueDays;
  final DateTime estimatedPaymentDate;
  final int daysUntilDue;
  final int daysUntilStatement;
  final bool inNewCycle;
}

class CardCalculator {
  static CardSummary calculateCardStatus(DateTime now, CreditCardModel card) {
    final today = now;
    final currentStatement = _statementDateForMonth(today.year, today.month, card.statementDay, card.weekendShift);
    final lastStatement = today.isBefore(currentStatement)
        ? _statementDateForMonth(today.year, today.month - 1, card.statementDay, card.weekendShift)
        : currentStatement;
    final nextStatement = _statementDateForMonth(lastStatement.year, lastStatement.month + 1, card.statementDay, card.weekendShift);
    final newCycleStart = _startOfNewCycle(lastStatement);
    final inNewCycle = !today.isBefore(newCycleStart);

    final effectiveStatement = inNewCycle ? nextStatement : currentStatement;
    final dueDate = _shiftToBusinessDay(effectiveStatement.add(Duration(days: card.dueDays)));

    final daysUntilDue = dueDate.difference(today).inDays;
    final targetStatement = inNewCycle ? nextStatement : currentStatement;
    final daysUntilStatement = targetStatement.difference(today).inDays;

    return CardSummary(
      id: card.id,
      cardName: card.name,
      statementDay: card.statementDay,
      dueDays: card.dueDays,
      estimatedPaymentDate: dueDate,
      daysUntilDue: daysUntilDue < 0 ? 0 : daysUntilDue,
      daysUntilStatement: daysUntilStatement < 0 ? 0 : daysUntilStatement,
      inNewCycle: inNewCycle,
    );
  }

  static CardSummary? selectBestCard(List<CardSummary> summaries) {
    if (summaries.isEmpty) return null;
    final newCycleCards = summaries.where((s) => s.inNewCycle).toList();
    final candidates = newCycleCards.isNotEmpty ? newCycleCards : summaries;
    candidates.sort((a, b) => b.daysUntilDue.compareTo(a.daysUntilDue));
    return candidates.first;
  }

  static DateTime _statementDateForMonth(int year, int month, int day, bool weekendShift) {
    final lastDayOfMonth = DateTime(year, month + 1, 0).day;
    final actualDay = day > lastDayOfMonth ? lastDayOfMonth : day;
    final normalized = DateTime(year, month, actualDay);
    if (!weekendShift) return normalized;
    return _shiftToBusinessDay(normalized);
  }

  static DateTime _shiftToBusinessDay(DateTime date) {
    if (date.weekday == DateTime.saturday) {
      return date.add(const Duration(days: 2));
    }
    if (date.weekday == DateTime.sunday) {
      return date.add(const Duration(days: 1));
    }
    return date;
  }

  /// Yeni dönem, ekstre kesim gününü takip eden günün 00:00'ında başlar.
  /// Örneğin kesim günü 10 ise yeni dönem 11. gün 00:00'da aktif olur.
  static DateTime _startOfNewCycle(DateTime statementDate) {
    final nextDay = DateTime(statementDate.year, statementDate.month, statementDate.day).add(const Duration(days: 1));
    return DateTime(nextDay.year, nextDay.month, nextDay.day);
  }
}

class CardStorage {
  static const _prefsKey = 'saved_cards';

  static Future<List<CreditCardModel>> loadCards() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_prefsKey);
    if (raw == null || raw.isEmpty) return [];
    final data = jsonDecode(raw) as List<dynamic>;
    return data.map((item) => CreditCardModel.fromJson(item as Map<String, dynamic>)).toList();
  }

  static Future<void> saveCards(List<CreditCardModel> cards) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = jsonEncode(cards.map((card) => card.toJson()).toList());
    await prefs.setString(_prefsKey, raw);
  }

  static List<CreditCardModel> defaultCards() {
    return [
      CreditCardModel(
        id: 'A',
        name: 'Kart A',
        statementDay: 10,
        dueDays: 40,
        weekendShift: true,
      ),
      CreditCardModel(
        id: 'B',
        name: 'Kart B',
        statementDay: 20,
        dueDays: 40,
        weekendShift: true,
      ),
    ];
  }
}
