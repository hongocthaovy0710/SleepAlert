# 🚀 Hướng Dẫn Chạy Server & Client - SleepAlert

## 📋 Yêu Cầu Hệ Thống

### Backend (Server)
- ✅ Java JDK 17+
- ✅ PostgreSQL 14+ (hoặc 12+)
- ✅ Gradle 8.x (đã có trong project qua wrapper)

### Android Client
- ✅ Android Studio (Arctic Fox hoặc mới hơn)
- ✅ Android SDK API 24-35
- ✅ Gradle 8.11.1 (wrapper tự động cài)
- ✅ Emulator hoặc thiết bị Android thật

---

## 🗄️ BƯỚC 1: Cài Đặt & Cấu Hình PostgreSQL

### Windows (PowerShell):

#### 1.1. Tải PostgreSQL
```powershell
# Tải từ: https://www.postgresql.org/download/windows/
# Hoặc dùng Chocolatey:
choco install postgresql14
```

#### 1.2. Khởi động PostgreSQL Service
```powershell
# Kiểm tra service đang chạy
Get-Service -Name postgresql*

# Nếu chưa chạy, start service:
Start-Service postgresql-x64-14
```

#### 1.3. Tạo Database
```powershell
# Mở psql (password mặc định khi cài: postgres)
psql -U postgres

# Trong psql shell, chạy:
CREATE DATABASE sleepalertdb;
CREATE USER sleepadmin WITH PASSWORD 'sleepalert123';
GRANT ALL PRIVILEGES ON DATABASE sleepalertdb TO sleepadmin;
\q
```

#### 1.4. Kiểm tra kết nối
```powershell
psql -U postgres -d sleepalertdb -c "SELECT version();"
```

---

## 🖥️ BƯỚC 2: Chạy Backend Server

### 2.1. Di chuyển vào thư mục backend
```powershell
cd D:\Android\project\SleepAlert\backend
```

### 2.2. (Tùy chọn) Sửa cấu hình database nếu cần
Mở file `src/main/kotlin/com/sleepalert/server/Server.kt` và kiểm tra:
```kotlin
Database.connect(
    url = "jdbc:postgresql://localhost:5432/sleepalertdb",
    driver = "org.postgresql.Driver",
    user = "postgres",        // ← Đổi thành sleepadmin nếu dùng user mới
    password = "root"         // ← Đổi thành sleepalert123 hoặc password của bạn
)
```

### 2.3. Build và chạy server
```powershell
# Build backend (lần đầu sẽ tải dependencies, mất vài phút)
..\gradlew.bat :backend:build

# Chạy server
..\gradlew.bat :backend:run
```

**Hoặc chạy trực tiếp bằng Gradle:**
```powershell
cd D:\Android\project\SleepAlert
.\gradlew.bat :backend:run
```

### 2.4. Kiểm tra server đã chạy
Server sẽ khởi động trên **http://localhost:8080**

Test bằng PowerShell:
```powershell
# Test endpoint register
Invoke-RestMethod -Uri http://localhost:8080/register `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"testuser","password":"test123","email":"test@example.com"}'

# Test endpoint login
Invoke-RestMethod -Uri http://localhost:8080/login `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"testuser","password":"test123"}'
```

**Kết quả mong đợi:**
```json
{
  "status": "success",
  "message": "Đăng ký thành công"
}
```

---

## 📱 BƯỚC 3: Chạy Android Client

### 3.1. Mở project trong Android Studio
```powershell
# Từ thư mục project
cd D:\Android\project\SleepAlert
# Mở Android Studio và chọn "Open" → chọn thư mục này
```

### 3.2. Sync Gradle
- Android Studio sẽ tự động sync Gradle khi mở project
- Hoặc click **File → Sync Project with Gradle Files**

### 3.3. Cấu hình Emulator/Device

#### Nếu dùng Emulator:
- Tạo AVD (Android Virtual Device) trong Device Manager
- Khuyến nghị: Pixel 5 API 34 hoặc API 35

#### Nếu dùng thiết bị thật:
- Bật **Developer Options** và **USB Debugging**
- Kết nối qua USB

### 3.4. Build APK
```powershell
# Từ root project
.\gradlew.bat :app:assembleDebug
```

APK sẽ được tạo tại:
```
D:\Android\project\SleepAlert\app\build\outputs\apk\debug\app-debug.apk
```

### 3.5. Chạy app
**Cách 1: Từ Android Studio**
- Click **Run** (Shift+F10) hoặc nút ▶️ màu xanh

**Cách 2: Từ PowerShell**
```powershell
# Cài APK vào emulator/device đang kết nối
.\gradlew.bat :app:installDebug

# Khởi động app
adb shell am start -n com.sleepalert.app/.MainActivity
```

---

## 🔧 Xử Lý Sự Cố Thường Gặp

### ❌ Lỗi: "Unable to connect to server"
**Nguyên nhân:** App Android không kết nối được tới backend

**Giải pháp:**
1. **Emulator** sử dụng `10.0.2.2` để truy cập localhost của máy host:
   - URL trong code đã đúng: `http://10.0.2.2:8080/login`
   
2. **Thiết bị thật:** cần dùng IP thật của máy tính:
   ```powershell
   # Lấy IP của máy
   ipconfig | Select-String "IPv4"
   ```
   Sửa trong `LoginActivity.kt`:
   ```kotlin
   val url = URL("http://192.168.1.XXX:8080/login") // thay XXX bằng IP thật
   ```

3. Tắt Windows Firewall tạm thời hoặc mở port 8080:
   ```powershell
   New-NetFirewallRule -DisplayName "Ktor Server" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
   ```

### ❌ Lỗi: "Connection refused" (backend)
**Giải pháp:**
- Kiểm tra PostgreSQL đang chạy:
  ```powershell
  Get-Service postgresql*
  ```
- Kiểm tra database đã tạo:
  ```powershell
  psql -U postgres -d sleepalertdb -c "\dt"
  ```

### ❌ Lỗi: "AGP version incompatible"
**Giải pháp:** Đã fix trong project, nhưng nếu gặp lại:
```powershell
# Sync lại Gradle
.\gradlew.bat clean build --refresh-dependencies
```

---

## 🎯 Quy Trình Phát Triển Hàng Ngày

### Khởi động hệ thống:

1️⃣ **Start PostgreSQL** (nếu chưa chạy):
```powershell
Start-Service postgresql-x64-14
```

2️⃣ **Start Backend Server**:
```powershell
cd D:\Android\project\SleepAlert
.\gradlew.bat :backend:run
```
Giữ cửa sổ PowerShell này mở (server chạy liên tục)

3️⃣ **Start Android App** (cửa sổ PowerShell mới):
```powershell
cd D:\Android\project\SleepAlert
.\gradlew.bat :app:installDebug
adb shell am start -n com.sleepalert.app/.MainActivity
```

### Tắt hệ thống:
- **Backend:** Nhấn `Ctrl+C` trong cửa sổ chạy server
- **App:** Thoát app bình thường
- **PostgreSQL:** (tùy chọn) `Stop-Service postgresql-x64-14`

---

## 📊 Cấu Trúc API

### Endpoints hiện tại:

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| POST | `/register` | `{"username":"...", "password":"...", "email":"..."}` | `{"status":"success/error", "message":"..."}` |
| POST | `/login` | `{"username":"...", "password":"..."}` | `{"status":"success/error", "message":"..."}` |
| POST | `/forgot-password` | `{"email":"..."}` | `{"status":"success/error", "message":"..."}` |

---

## 🔐 Database Schema

### Table: `users`
| Column | Type | Constraints |
|--------|------|-------------|
| id | INTEGER | PRIMARY KEY, AUTO_INCREMENT |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| password | VARCHAR(100) | NOT NULL |
| email | VARCHAR(100) | NOT NULL |

---

## 🌟 Tips & Best Practices

1. **Development:** Luôn chạy server trước khi test app
2. **Debugging:** Xem logs trong Logcat (Android Studio) để debug network calls
3. **Database:** Dùng pgAdmin hoặc DBeaver để xem/quản lý database trực quan
4. **Git:** Commit thường xuyên, không commit `build/`, `local.properties`

---

## 📞 Liên Hệ & Hỗ Trợ

Nếu gặp vấn đề, kiểm tra:
1. ✅ PostgreSQL service đang chạy
2. ✅ Database `sleepalertdb` đã được tạo
3. ✅ Backend server đang chạy trên port 8080
4. ✅ Android app dùng đúng IP (10.0.2.2 cho emulator)
5. ✅ Firewall không chặn port 8080

**Chúc bạn code vui vẻ! 🎉**
