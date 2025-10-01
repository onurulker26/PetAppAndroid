<div align="center">
  <img src="app/src/main/res/drawable/ic_pet_logo.xml" alt="PetApp Logo" width="120" height="120"/>
  
  # 🐾 PetApp - Yapay Zeka Destekli Evcil Hayvan Yönetim Sistemi
  
  *Evcil hayvan sahipleri için geliştirilmiş, AI destekli kapsamlı sağlık ve bakım platformu*
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)
  [![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow.svg)](https://firebase.google.com)
  [![TensorFlow Lite](https://img.shields.io/badge/AI-TensorFlow%20Lite-red.svg)](https://www.tensorflow.org/lite)
</div>

---

## 🚀 Proje Hakkında

PetApp, evcil hayvan sahiplerinin hayvanlarının sağlık ve bakım süreçlerini dijital olarak yönetmelerine yardımcı olan, yapay zeka destekli bir mobil uygulamadır. Uygulama, modern teknolojiler kullanılarak geliştirilmiş olup, evcil hayvan bakımında devrim niteliğinde özellikler sunmaktadır.

### 🎯 Ana Özellikler

- 🤖 **AI Destekli Hastalık Analizi**: TensorFlow Lite ile kedi ve köpeklerde deri hastalıklarının erken teşhisi
- 📱 **Modern UI/UX**: Jetpack Compose ile geliştirilmiş kullanıcı dostu arayüz
- 🗺️ **Veteriner Bulucu**: GPS tabanlı en yakın veteriner klinik bulma
- 💉 **Aşı Takibi**: Otomatik hatırlatmalar ile aşı takvimi yönetimi
- 🏥 **Tedavi Kayıtları**: Dijital sağlık geçmişi ve tedavi takibi
- 👥 **Sosyal Forum**: Evcil hayvan sahipleri için bilgi paylaşım platformu
- 🔐 **Güvenli Veri**: Firebase ile güvenli bulut tabanlı veri depolama

---

## 🛠 Teknolojik Altyapı

### Frontend & UI
- **Kotlin**: Modern ve güvenli programlama dili
- **Jetpack Compose**: Deklaratif UI geliştirme kiti
- **Material Design 3**: Google'ın modern tasarım standartları
- **Coil**: Asenkron görüntü yükleme ve önbellekleme

### Backend & Veritabanı
- **Firebase Authentication**: Güvenli kullanıcı kimlik doğrulama
- **Cloud Firestore**: NoSQL veritabanı ile gerçek zamanlı veri senkronizasyonu
- **Firebase Storage**: Medya dosyalarının güvenli depolanması
- **Firebase Analytics**: Kullanıcı davranış analizi

### Yapay Zeka & ML
- **TensorFlow Lite**: Mobil cihazlarda AI model çalıştırma
- **Özel Eğitilmiş Modeller**: 
  - Kedi hastalık sınıflandırması (4 kategori)
  - Köpek hastalık sınıflandırması (4 kategori)

### Harita & Konum
- **Google Maps API**: Interaktif harita görüntüleme
- **Google Places API**: Mekan arama ve detayları
- **FusedLocationProvider**: Optimized konum servisleri

---

## 📋 Jetpack Compose Geçişi

Bu projede, PetApp uygulamasının XML tabanlı UI yapısından Jetpack Compose'a geçişi yapılmıştır. 

### ✅ Tamamlanan Geçişler

- `VaccinationsScreen.kt`: Aşı takibi için Compose ekranı
- `AddPetScreen.kt`: Evcil hayvan ekleme/düzenleme için Compose ekranı
- `VetClinicsScreen.kt`: Veteriner klinik arama ve harita ekranı
- `DiseaseAnalysisScreen.kt`: AI destekli hastalık analizi ekranı
- `ProfileScreen.kt`: Kullanıcı profil yönetimi ekranı

### 🔧 Aktivite Güncellemeleri

Aşağıdaki aktiviteler Jetpack Compose kullanacak şekilde modernize edilmiştir:

- `AddPetActivity.kt`: XML layout yerine Compose ekranını kullanacak şekilde güncellendi
- `VaccinationsActivity.kt`: Compose tabanlı reaktif arayüz
- `TreatmentActivity.kt`: Modern Compose ekranı ile değiştirildi
- `ProfileActivity.kt`: Tamamen Compose'a geçirildi

### 🎨 UI/UX İyileştirmeleri

- **Reactive Data Flow**: `StateFlow` ve Compose'un state yönetimi ile reaktif veri akışı
- **Modern Animations**: Smooth geçişler ve etkileşimler
- **Material 3 Components**: En güncel tasarım bileşenleri
- **Dark Theme Support**: Otomatik karanlık tema desteği

---

## 🔮 Gelecek Planları

### 🚧 Devam Eden Geliştirmeler
- Tüm ekranların Compose'a geçişinin tamamlanması
- Offline-first data synchronization
- Advanced AI models (ses analizi, hareket analizi)
- Web dashboard for veterinarians

### 📱 Gelecek Özellikler
- **Giyilebilir Cihaz Entegrasyonu**: Akıllı tasma sensörleri
- **Telemedicine**: Video görüşme ile veteriner danışmanlığı
- **IoT Integration**: Akıllı mama/su kabı takibi
- **Blockchain**: NFT tabanlı evcil hayvan kimlik sistemi

---

## 🏗️ Kurulum ve Çalıştırma

```bash
# Repo'yu klonlayın
git clone https://github.com/username/PetAppjcSongibi2.git

# Android Studio'da projeyi açın
cd PetAppjcSongibi2

# Firebase yapılandırması
# google-services.json dosyasını app/ dizinine ekleyin

# Google Maps API Key
# local.properties dosyasına MAPS_API_KEY ekleyin

# Projeyi build edin ve çalıştırın
./gradlew assembleDebug
```

### 🔑 Gerekli API Keys
- Firebase Project Configuration
- Google Maps API Key
- Google Places API Key

---

## 📸 Ekran Görüntüleri

| Ana Sayfa | Hastalık Analizi | Veteriner Haritası | Aşı Takibi |
|-----------|------------------|---------------------|-------------|
| *Ana ekran görüntüsü* | *AI analiz ekranı* | *Harita görünümü* | *Aşı takvimi* |

---

## 🤝 Katkıda Bulunma

Bu proje açık kaynaklı olup, katkılarınızı bekliyoruz:

1. Fork edin
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit edin (`git commit -m 'Add amazing feature'`)
4. Push edin (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

---

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakınız.

---

## 📞 İletişim

**Proje Sahibi**: Onur Ülker,Tamer Yurdakul
- 📧 Email: [onurulker10@gmail.com]
- 💼 LinkedIn: [[linkedin/in](https://www.linkedin.com/in/onurulker)]
- 🐦 Twitter: [onurulker26]

---

<div align="center">
  <strong>🐾 Evcil hayvan dostlarımız için daha iyi bir yaşam! 🐾</strong>
  
  ⭐ Projeyi beğendiyseniz yıldızlamayı unutmayın!
</div> 
