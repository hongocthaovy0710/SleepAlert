# ⚡ HƯỚNG DẪN NHANH - SleepAlert

## 🎯 Cách Nhanh Nhất (Recommended)

### Bước 1: Cài PostgreSQL (chỉ lần đầu)
```powershell
# Tải và cài: https://www.postgresql.org/download/windows/
# Hoặc dùng Chocolatey:
choco install postgresql14
```

### Bước 2: Chạy script tự động
```powershell
cd D:\Android\project\SleepAlert
.\start.ps1
```

Chọn option:
- **1** = Chỉ chạy Server
- **2** = Chỉ build APK
- **3** = Chạy cả Server + cài APK (khuyến nghị)

---

## 🔧 Cách Thủ Công

### Lần đầu tiên:
```powershell
# 1. Tạo database
psql -U postgres
CREATE DATABASE sleepalertdb;
\q

# 2. Build backend (riêng biệt)
cd D:\Android\project\SleepAlert\backend
gradle build
cd ..

# 3. Sync Gradle cho Android app
.\gradlew.bat :app:assembleDebug

# 4. Chạy server (giữ cửa sổ này mở)
cd backend
gradle run
cd ..

# 5. Cửa sổ mới: Cài APK
.\gradlew.bat :app:installDebug

# 6. Mở app trên emulator/device
adb shell am start -n com.sleepalert.app/.MainActivity
```

### Các lần sau:
```powershell
# Cửa sổ 1: Start server
cd D:\Android\project\SleepAlert\backend
gradle run

# Cửa sổ 2: Install app
cd D:\Android\project\SleepAlert
.\gradlew.bat :app:installDebug
adb shell am start -n com.sleepalert.app/.MainActivity
```

---

## 🐛 Troubleshooting

### Lỗi: "Connection refused"
```powershell
# Kiểm tra PostgreSQL
Get-Service postgresql*

# Nếu stopped:
Start-Service postgresql-x64-14
```

### Lỗi: "Unable to connect to server" (từ app)
- **Emulator**: Đã dùng `10.0.2.2` → OK ✅
- **Thiết bị thật**: Lấy IP máy tính:
  ```powershell
  ipconfig | Select-String "IPv4"
  ```
  Sửa trong `LoginActivity.kt`: `http://192.168.x.x:8080/...`

### Lỗi: "Port 8080 already in use"
```powershell
# Tìm process đang dùng port 8080
netstat -ano | findstr :8080

# Kill process (thay PID)
taskkill /PID <PID> /F
```

---

## 📍 Các URL Quan Trọng

- **Backend Server**: http://localhost:8080
- **Test API** (từ trình duyệt): http://localhost:8080/health (nếu thêm endpoint)
- **PostgreSQL**: localhost:5432
- **Database**: sleepalertdb

---

## 📁 Files Quan Trọng

| File | Mục đích |
|------|---------|
| `start.ps1` | Script tự động chạy server + app |
| `SETUP_GUIDE.md` | Hướng dẫn chi tiết đầy đủ |
| `backend/build.gradle.kts` | Cấu hình backend dependencies |
| `app/build.gradle.kts` | Cấu hình Android app |
| `backend/src/.../Server.kt` | Code backend server |
| `app/src/.../LoginActivity.kt` | Code màn hình đăng nhập |

---

## ✅ Checklist Trước Khi Chạy

- [ ] PostgreSQL đã cài và đang chạy
- [ ] Database `sleepalertdb` đã được tạo
- [ ] Java JDK 17+ đã cài
- [ ] Android SDK đã cài (qua Android Studio)
- [ ] Emulator hoặc device đã kết nối (`adb devices`)

---

**💡 Tip**: Bookmark file này để tra cứu nhanh!

Xem `SETUP_GUIDE.md` cho hướng dẫn chi tiết hơn.
