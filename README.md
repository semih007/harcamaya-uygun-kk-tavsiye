# harcamaya-uygun-kk-tavsiye

Bu proje, hem Android Compose tabanlı bir scaffold hem de yeni isteğe göre Flutter cross-platform uygulama kodu içermektedir.

## Teknik tercih
- `flutter_app` dizini altında Flutter + Dart uygulaması
- `flutter_app/pubspec.yaml` ile `shared_preferences` ve `intl` kullanılıyor
- `flutter_app/lib/main.dart` içinde kredi kartı seçme algoritması ve ana ekran UI kodu hazırlandı
- Uygulama tamamen offline çalışacak şekilde veri cihazda saklanır

## Flutter uygulamasını nasıl çalıştırırsınız
1. `cd flutter_app`
2. `flutter pub get`
3. `flutter run`

## Android Studio tabanlı scaffold
- `app/src/main/java/com/example/harcamayauygun/MainActivity.kt` : basit Jetpack Compose başlangıç ekranı
- `app/src/main/res/values/strings.xml` : uygulama metinleri
- `app/src/main/res/values/themes.xml` : uygulama teması
- `app/src/main/AndroidManifest.xml` : manifest yapılandırması

## Sonraki adım
Uygulamanın kullanıcı akışını güncellediğinde, kart ekleme, bildirim hatırlatıcıları ve detaylı ödeme planı ekranlarını ekleyebilirim.
