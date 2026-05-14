# Harcamaya Uygun Kredi Kartı Uygulaması

Flutter tabanlı bu uygulama, kullanıcının kart ekstre tarihlerini takip eder ve bugünkü harcama için en uzun faizsiz dönem sağlayacak kartı önerir.

## Özellikler
- Kart adı, ekstre kesim günü ve son ödeme günü bilgisi girilebilir.
- Bugünkü tarihe göre en avantajlı kart seçilir.
- Ekstre kesiminden bir gün sonra yeni dönem başlar; öğle 12:00 tamponu hesaba katılır.
- Hafta sonu kaymalarını hesaba katar.
- Veriler cihazda `SharedPreferences` ile saklanır.
- Minimalist Fintech tarzı ana ekran.

## Çalıştırma
1. `flutter pub get` çalıştırın.
2. `flutter run` ile projeyi başlatın.

## Kod Yapısı
- `lib/main.dart` : Uygulama mantığı, hesaplama ve arayüz.
- `pubspec.yaml` : Flutter bağımlılıkları.
