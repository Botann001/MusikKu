# MusikKu 🎵

**MusikKu** adalah aplikasi pemutar musik modern dan elegan untuk Android, dibangun menggunakan **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)**, dan arsitektur MVVM yang bersih. Aplikasi ini mendukung pemutaran koleksi audio lokal di perangkat dan penjelajahan/streaming/unduhan musik gratis dan legal dari **Jamendo API**.

---

## 🌟 Fitur Utama

1. **Pemindaian & Pustaka Lagu Lokal (Tahap 1)**
   - Deteksi izin penyimpanan adaptif (`READ_MEDIA_AUDIO` untuk Android 13+ dan `READ_EXTERNAL_STORAGE` untuk Android 12 ke bawah).
   - Pemindaian audio otomatis via `MediaStore` dan penyimpanan data lokal via **Room Database**.
   - Menampilkan cover album lokal via **Coil**, daftar lagu, dan pencarian cepat.

2. **Pemutar Latar Belakang & Layanan Media3 (Tahap 2)**
   - `MusicService` berbasis `MediaSessionService` dan `ExoPlayer`.
   - Tetap berjalan di latar belakang (Foreground Service `mediaPlayback`) saat layar mati atau aplikasi diminimalkan.
   - Penanganan otomatis perubahan output audio (*Audio Becoming Noisy* / cabut headset) dan audio focus.
   - Notifikasi pemutar sistem dengan tombol Play, Pause, Next, dan Previous.

3. **Antarmuka Pemutar Interaktif (Tahap 3)**
   - **Mini Player**: Tampil mengambang di atas navigasi bawah saat lagu aktif dengan tombol play/pause dan progress.
   - **Now Playing Sheet**: Tampilan layar penuh dengan cover art besar, seekbar interaktif, repeat (off/all/one), shuffle, dan tombol favorit.
   - **Antrean Pemutaran (Queue)**: Lihat daftar lagu aktif, hapus antrean, atau seret untuk mengubah urutan pemutaran.

4. **Playlist & Favorit dengan Room (Tahap 4)**
   - Sistem Favorit instan dengan sinkronisasi satu sentuhan.
   - Manajemen playlist lengkap: Buat playlist, ubah nama, hapus playlist, dan tambah/hapus lagu.
   - Skema database Room terkelola dengan migrasi versi terstruktur (*non-destructive*).

5. **Integrasi Jamendo API (Tahap 5)**
   - Cari jutaan lagu gratis dan legal via Jamendo API (Retrofit + OkHttp).
   - Streaming online langsung dengan caching file audio otomatis (**Media3 SimpleCache**).
   - Unduh lagu ke penyimpanan lokal aplikasi via **WorkManager**.
   - Opsi pengaturan hemat kuota: **Unduh Hanya via Wi-Fi**.
   - Deteksi koneksi (*Network Callback*): saat offline, lagu streaming yang belum diunduh ditandai secara visual dan dicegah agar tidak memicu error pemutaran.

6. **Finalisasi & Sentuhan Premium (Tahap 6)**
   - **Tema Tampilan**: Pilihan mode Gelap (*Dark*), Terang (*Light*), atau Mengikuti Sistem (*System*).
   - **Sleep Timer**: Pengatur waktu tidur otomatis (15m, 30m, 45m, 60m) dengan countdown realtime dan penghentian pemutaran yang mulus.
   - **Ikon & Splash Screen**: Ikon aplikasi vektor kustom dan splash screen transisi awal yang halus.
   - **Penanganan Kasus Tepi (Edge Cases)**:
     - *Penyimpanan Penuh*: Memeriksa sisa kapasitas disk sebelum mengunduh lagu.
     - *Izin Ditolak*: Halaman status kosong dengan tombol langsung menuju Pengaturan Aplikasi Android.
     - *File Terhapus*: Pengecekan file fisik lokal sebelum pemutaran, notifikasi pesan ramah, dan transisi aman ke lagu berikutnya.
   - **Build Release Teroptimasi**: Konfigurasi R8/ProGuard dan resource shrinking aktif, menghasilkan ukuran APK sangat ringkas (~4 MB).

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa**: Kotlin (Coroutines, StateFlow, Flow)
- **UI Toolkit**: Jetpack Compose, Material Design 3
- **Audio Engine**: AndroidX Media3 (ExoPlayer 1.3.1, MediaSessionService)
- **Database**: Room Database (SQLite) dengan migrasi versi manual
- **Penyimpanan Preferensi**: Jetpack DataStore Preferences
- **Jaringan & API**: Retrofit 2, OkHttp, Gson
- **Background Tasks**: AndroidX WorkManager
- **Image Loading**: Coil Compose
- **Dependency Injection**: Manual DI via `AppContainer` (bersih, cepat, tanpa overhead)

---

## 🚀 Panduan Build & Menjalankan Proyek

### 1. Prasyarat
- Android Studio Ladybug / Iguana atau yang lebih baru.
- JDK 17 atau JDK 21.
- Android SDK dengan platform target API 35 (Android 15) dan min SDK 24 (Android 7.0).

### 2. Konfigurasi Jamendo API Key (local.properties)
Aplikasi ini membaca kredensial API secara aman melalui `BuildConfig`. **JANGAN PERNAH** menuliskan API key langsung di kode sumber atau meng-commit file kredensial ke Git.

Buka atau buat file `local.properties` di root folder proyek:
```properties
sdk.dir=C:\\Users\\<Username>\\AppData\\Local\\Android\\Sdk
JAMENDO_CLIENT_ID=your_jamendo_client_id_here
```
*(Jika tidak memiliki API key, aplikasi tetap dapat menggunakan key bawaan untuk pengujian non-komersial).*

### 3. Build APK Debug
Jalankan perintah berikut di terminal:
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
File APK debug berada di:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Build APK Release (Minified & Shrunk)
Untuk membangun APK versi rilis yang telah dioptimasi dengan R8:
```bash
# Windows
.\gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```
File APK rilis berada di:
`app/build/outputs/apk/release/app-release.apk`

---

## 📲 Cara Memasang APK ke Perangkat (Install)

### Menggunakan ADB (Android Debug Bridge):
1. Hubungkan HP ke laptop melalui kabel data USB dan aktifkan **USB Debugging** di Opsi Pengembang (*Developer Options*).
2. Jalankan perintah:
```bash
# Untuk Debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Untuk Release APK (setelah di-sign)
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Memasang Secara Manual:
1. Salin file `.apk` ke memori HP (melalui kabel USB, WhatsApp, Google Drive, dsb.).
2. Buka File Manager di HP, ketuk file `.apk`, dan izinkan "Pasang dari sumber tidak dikenal" bila diminta.

---

## 🔐 Panduan Membuat Keystore & Menghasilkan Signed Release APK

> ⚠️ **PERINGATAN KEAMANAN PENTING**:
> **JANGAN PERNAH** menyimpan atau meng-commit file keystore (`.jks` / `.keystore`) atau password keystore ke dalam repositori Git! Pastikan pola `*.jks` dan `*.keystore` ada di dalam `.gitignore`.

### Langkah A: Membuat Keystore Baru

#### Opsi 1: Menggunakan Perintah `keytool` (Terminal)
Jalankan perintah berikut di direktori aman di luar repositori Git (atau di folder lokal yang tidak di-commit):
```bash
keytool -genkeypair -v -keystore my-release-key.jks -alias musikku-key -keyalg RSA -keysize 2048 -validity 10000
```
- Anda akan diminta memasukkan kata sandi (password), nama lengkap, organisasi, dan negara.
- Simpan file `my-release-key.jks` dan catat password serta nama aliasnya di tempat yang aman.

#### Opsi 2: Menggunakan Android Studio GUI
1. Pada menu Android Studio, pilih **Build** > **Generate Signed Bundle / APK...**
2. Pilih opsi **APK**, lalu klik **Next**.
3. Di bawah kolom *Key store path*, klik **Create new...**
4. Tentukan lokasi penyimpanan file (misal di folder dokumen komputer Anda), masukkan password keystore, buat alias (contoh: `musikku-key`), dan masukkan password alias.
5. Klik **OK**.

---

### Langkah B: Menghasilkan Signed APK

#### Melalui Android Studio (GUI):
1. Buka menu **Build** > **Generate Signed Bundle / APK...**
2. Pilih **APK**, lalu klik **Next**.
3. Pilih file keystore yang telah dibuat, masukkan password keystore, alias, dan password alias.
4. Klik **Next**.
5. Pilih build variant **release**.
6. Centang signature versions (**V1** dan **V2 / Full APK Signature** bila tersedia).
7. Klik **Finish**.
8. File signed APK yang siap didistribusikan akan berada di folder `app/release/`.

#### Melalui File Konfigurasi Otomatis (Opsional & Aman):
Jika ingin melakukan build otomatis via terminal tanpa membocorkan kredensial:
1. Buat file `keystore.properties` di root folder proyek (**pastikan file ini terdaftar di `.gitignore`**):
   ```properties
   KEYSTORE_FILE=C:/path/ke/my-release-key.jks
   KEYSTORE_PASSWORD=rahasia_password_keystore
   KEY_ALIAS=musikku-key
   KEY_PASSWORD=rahasia_password_alias
   ```
2. Hubungkan di `app/build.gradle.kts`:
   ```kotlin
   val keystorePropsFile = rootProject.file("keystore.properties")
   val keystoreProps = Properties()
   if (keystorePropsFile.exists()) {
       keystoreProps.load(FileInputStream(keystorePropsFile))
   }

   android {
       signingConfigs {
           create("release") {
               if (keystorePropsFile.exists()) {
                   storeFile = file(keystoreProps.getProperty("KEYSTORE_FILE"))
                   storePassword = keystoreProps.getProperty("KEYSTORE_PASSWORD")
                   keyAlias = keystoreProps.getProperty("KEY_ALIAS")
                   keyPassword = keystoreProps.getProperty("KEY_PASSWORD")
               }
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               ...
           }
       }
   }
   ```
3. Jalankan `.\gradlew.bat assembleRelease` untuk menghasilkan APK rilis yang otomatis ditandatangani.

---

## 📄 Lisensi
Aplikasi ini dikembangkan untuk tujuan portofolio dan pemutaran musik pribadi. Lagu online bersumber dari katalog musik berlisensi Creative Commons melalui Jamendo API.
