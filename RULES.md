# MusikKu — Aturan Proyek

> File ini adalah kontrak tetap untuk semua tahap pengembangan.
> Setiap tugas WAJIB mematuhi seluruh isi dokumen ini.

---

## 1. Tujuan

Aplikasi pemutar musik **pribadi**, offline-first, untuk dipasang via APK.

## 2. Stack & Library yang Diizinkan

| Kategori | Library / Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Arsitektur | MVVM + Repository |
| Media | Media3 (ExoPlayer + MediaSessionService) |
| Database | Room |
| Background work | WorkManager |
| Networking | Retrofit |
| Image loading | Coil |
| Preferences | DataStore |
| Build | Gradle version catalog (`libs.versions.toml`), versi stabil terbaru |

- **minSdk 26**
- Selalu gunakan **versi stabil terbaru** dari semua library.
- **Tidak boleh** menambahkan library di luar daftar ini tanpa izin eksplisit dari pemilik proyek.

## 3. Larangan

- ❌ Tanpa login / autentikasi.
- ❌ Tanpa backend sendiri.
- ❌ Tanpa Hilt — dependency injection dilakukan secara **manual** lewat satu class `AppContainer`.
- ❌ Tanpa library di luar daftar (lihat §2) tanpa izin.

## 4. Gaya Kerja

1. **Ubah hanya file yang relevan** dengan tugas saat ini.
2. **Jangan hapus atau refactor** kode dari tahap sebelumnya kecuali diminta.
3. Setelah selesai, **jalankan** `.\gradlew assembleDebug` dan perbaiki error sampai **BUILD SUCCESSFUL**.
4. Setelah build berhasil, **jelaskan singkat** file apa saja yang berubah.
5. Beri **komentar singkat** di kode untuk bagian yang rumit.
