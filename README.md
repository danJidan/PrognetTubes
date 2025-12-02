# PrognetTubes

## Commodity Server - Quick Start Guide

### 📁 Struktur Folder
```
src/
   com/commoditylive/common/Message.java          # Objek pesan serializable
   com/commoditylive/model/MarketDataModel.java   # Model data komoditas & laporan
   com/commoditylive/server/                      # Seluruh komponen server
      ServerMain.java
      ClientHandler.java
      ServerBroadcastThread.java
   com/commoditylive/client/                      # Aplikasi desktop & test client
      CommodityLiveGUI.java
      CommodityLiveAdminUI.java
      TestClient.java
```
Semua hasil kompilasi tersimpan di folder `out/` (dibuat otomatis oleh `javac -d out ...`).

### 🚀 Cara Menjalankan
1. **Compile**
   ```powershell
   if (Test-Path out) { Remove-Item out -Recurse -Force }
   $sources = Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName }
   javac -cp "lib/*" -d out $sources
   ```
2. **Jalankan Server (Terminal 1)**
   ```powershell
   java -cp "out;lib/*" com.commoditylive.server.ServerMain
   ```
3. **Jalankan Client (Terminal 2, 3, dst.)**
   - GUI utama:
     ```powershell
   java -cp "out;lib/*" com.commoditylive.client.CommodityLiveGUI
     ```
   - Console test client:
     ```powershell
     java -cp "out;lib/*" com.commoditylive.client.TestClient

### 📚 Use Case Utama

1. **Admin Perdagangan**
   - Login via tombol *Login Admin* di GUI.
   - Menambah komoditas baru, mengubah harga, atau menghapus komoditas lama.
   - Menandai status laporan pengguna (dibaca/belum) agar workflow customer-care tercatat.

2. **Trader / Manajemen**
   - Membuka dashboard CommodityLive untuk memonitor harga real-time berikut grafik & statistik.
   - Mengirimkan laporan atau insight pasar ke tim admin (fitur "Pesan Pengguna").

3. **Broadcast Otomatis**
   - Setiap perubahan harga/CRUD dari admin memicu broadcast `PRICE_UPDATE` ke semua klien.
   - Klien melakukan *refresh* daftar komoditas ketika broadcast diterima sehingga data konsisten.

### 🏛️ Arsitektur Jaringan

```
[Client GUI #1] -- TCP/IP -->
                          |
[Client GUI #2] -- TCP/IP -->   [CommodityLive Server] -- JDBC --> [SQLite DB]
                          |
[Client GUI #N] -- TCP/IP -->
```

- Semua klien (admin ataupun view-only) membuka koneksi socket ke server (`ServerMain`, port 5000).
- Server menjalankan thread `ClientHandler` per koneksi dan satu `ServerBroadcastThread` untuk distribusi pesan.
- Data komoditas & laporan tersimpan di SQLite (`commoditylive.db`) yang diakses melalui `DatabaseManager`.
- Untuk mode multi-laptop: klien hanya perlu mengganti `SERVER_HOST` (default `localhost`) ke IP server.
     ```

### 🎮 Fitur Admin Console (di Server)
Saat server berjalan tekan **ENTER** untuk membuka menu admin:
1. Check Connected Clients
2. Broadcast PRICE_UPDATE
3. Broadcast Custom Message
4. Server Status
0. Shutdown Server

### 🧪 Testing Workflow Lokal
1. Start server: `java ServerMain`
2. Jalankan beberapa `java TestClient`
3. Gunakan admin console untuk memantau/broadcast
4. Pastikan semua client menerima broadcast real-time

### 🖧 Testing Multi-Laptop (Server + Client)
Gunakan skenario ini ketika server/admin dan pengguna berada di laptop berbeda.
1. **Siapkan jaringan** – semua laptop pada SSID/ router Wi‑Fi yang sama, matikan VPN jika perlu.
2. **Laptop A (Server/Admin)**
   - Clone repo, compile per perintah di atas.
   - Jalankan server dan biarkan terminal terbuka.
   - Jalankan `ipconfig`, catat alamat IPv4 (mis. `192.168.0.42`).
   - Izinkan `java.exe` pada firewall Windows (port 5000, jaringan privat).
   - Jalankan GUI admin dan login `admin/admin123` bila diperlukan.
3. **Laptop B (User)**
   - Instal Java versi sama, salin repo atau folder `out/`.
   - Ubah konstanta `SERVER_HOST` di `src/com/CommodityLive/client/CommodityLiveGUI.java` menjadi IP Laptop A, lalu recompile.
   - Jalankan klien GUI.
4. **Laptop C (opsional admin terpisah)** – ulangi langkah Laptop B dan login admin.
5. **Validasi koneksi** – kirim laporan dari Laptop B; tab **Pesan Pengguna** admin harus menerima entri baru. Jika gagal, cek IP, firewall, dan koneksi jaringan.

### 📋 Protocol Actions
**Client Request:** GET_COMMODITY_LIST, FILTER_COMMODITY, UPDATE_PRICE, CREATE_COMMODITY, DELETE_COMMODITY, SEND_REPORT, GET_REPORTS, UPDATE_REPORT_STATUS.

**Broadcast:** PRICE_UPDATE.

### 💾 Database
- **SQLite** untuk persistensi data
- File database: `CommodityLive.db` (auto-created)
- Tables: `commodities`, `reports`, `price_history`
- Dependencies: `sqlite-jdbc.jar`, `slf4j-api.jar`, `slf4j-simple.jar`

### 🔧 Fitur Lengkap
✅ **CRUD Komoditas** - Create, Read, Update, Delete komoditas secara langsung dari admin UI  
✅ **Validasi Duplikat** - Nama komoditas dicek agar tidak ganda di database  
✅ **Update Harga Manual** - Harga hanya berubah ketika admin men-trigger aksi UPDATE_PRICE  
✅ **Delete Komoditas** - Hapus komoditas termasuk histori harga terkait  
✅ **Laporan Pengguna** - User mengirim pesan, admin membaca dan memberi status  
✅ **Update Status Pesan** - Tandai laporan sudah dibaca/belum dibaca  
✅ **Real-time Broadcast** - Semua perubahan dikirim sebagai `PRICE_UPDATE` ke seluruh klien  
✅ **Database Persistence** - Data komoditas, laporan, dan histori harga tersimpan permanen di SQLite

### 🔧 Catatan Developer
Handler pada `ClientHandler.java` masih placeholder. Implementasi bisnis dilakukan di metode seperti:
- `handleGetCommodityList()`
- `handleFilterCommodity()`
- `handleUpdatePrice()`

### 🌐 Konfigurasi Default
- Server Port: `5000`
- Host: `localhost`
- Protocol: TCP/IP dengan ObjectInputStream/ObjectOutputStream

