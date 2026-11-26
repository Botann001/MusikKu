# 🎵 MusikKu Desktop — Edisi "Dial"

Aplikasi pemutar musik desktop modern dengan desain **"Dial" Dark Emerald Glassmorphism**, dibangun dengan **Electron**, **HTML5/CSS3/JavaScript**, dan **Node.js**.

---

## ✨ Fitur & Pembaruan Desain "Dial"

- 🎛️ **Turntable Vinyl & Dial 440px**:
  - Piringan hitam berputar halus (`.spinning`) saat lagu diputar dan berhenti saat dijeda.
  - Cincin dial progres interaktif dengan knob penunjuk posisi lagu yang dapat diklik atau digeser untuk lompat waktu (*seek*).
  - Tampilan teks judul besar (*Bricolage Grotesque 800*), waktu presisi, tombol putar utama (80x80px), serta slider volume beraksen cool blue.
- 📻 **Penala Genre (Genre Tuner) di Jelajah Online**:
  - Desain pita penggaris (*ruler*) dengan jarum penunjuk (*needle*) yang menyala beraksen hijau elektrik.
  - Pilihan saluran genre: `Populer`, `Rock`, `Pop`, `EDM`, `Lofi`, `Piano`, `Akustik`, `Chill`, `Dance`, `Summer`.
  - Daftar track bernomor besar (`01`, `02`, dst.) dengan dot warna gradasi piringan unik dan lencana `ONLINE`.
- 📁 **Struktur Playlist Dua Layar**:
  - **Sub-layar A (Daftar Playlist)**: Grid kartu playlist dengan sampul gradasi dinamis dan piringan vinyl yang mengintip di belakang sampul serta bergeser dan berputar halus saat di-*hover*.
  - **Sub-layar B (Isi Playlist)**: Tampilan detail playlist dengan judul besar (80px), sampul sleeve 220px, daftar lagu, tombol putar semua, acak, ubah nama, hapus dengan konfirmasi aman, serta penambahan lagu dari perpustakaan.
  - **8 Pilihan Gradasi Sampul**: Emerald, Indigo, Amber, Rose, Cyan, Lime, Purple, Coral.
- 🎯 **Layar Favorit & "Piringan Kosong"**:
  - Status kosong estetik dengan lingkaran bertanda putus-putus konsentris (*target dial*) berikon hati aksen di tengah.
  - Akses cepat untuk jelajah musik online atau buka perpustakaan lokal.
- 💊 **Pemutar Mini Melayang (Bottom Capsule)**:
  - Tampil di bagian bawah layar saat menjelajah tab lain dengan piringan vinyl mini 54px berputar, informasi lagu, tombol putar, serta progres bergaya penggaris dengan jarum menyala.
- 🖱️ **Menu Klik Kanan (Context Menu)**:
  - Klik kanan pada baris lagu apa saja untuk: putar sekarang, tambah/hapus favorit, atau masukkan ke daftar playlist pilihan.
- 🔒 **100% Mandiri Tanpa CDN (Bundled Local Fonts)**:
  - Menggunakan font lokal **Bricolage Grotesque** (display/judul) dan **Instrument Sans** (isi/body).
  - Ikon Material Icons Round dibundel secara offline.
- ♿ **Aksesibilitas & Kurangi Gerakan**:
  - Navigasi keyboard penuh (Tab, Enter, Spasi untuk putar/jeda).
  - Mendukung `prefers-reduced-motion` untuk mematikan animasi putaran piringan bagi pengguna yang sensitif.
  - Jendela transparan acrylic bawaan Windows 11 dengan cadangan dark emerald solid.

---

## 🚀 Cara Menjalankan

### 1. Menjalankan Langsung (Mode Development)
```powershell
cd desktop
npm start
```

### 2. Membangun Installer Windows (`.exe` NSIS)
```powershell
cd desktop
npm run build
```
File installer `MusikKu Setup 1.2.0.exe` dan portable `MusikKu-Desktop-1.2.0-portable.exe` akan berada di folder `desktop/dist/`.

### 3. File Hasil Build yang Siap Digunakan
- **Installer**: `desktop/dist/MusikKu Setup 1.2.0.exe` (~100 MB)
- **Portable**: `desktop/dist/MusikKu-Desktop-1.2.0-portable.exe` (~100 MB)
