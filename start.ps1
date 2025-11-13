# ========================================
# SleepAlert - Quick Start Script
# ========================================

Write-Host "🚀 SleepAlert Quick Start" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra PostgreSQL
Write-Host "1️⃣  Checking PostgreSQL..." -ForegroundColor Yellow
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue

if ($pgService) {
    if ($pgService.Status -eq "Running") {
        Write-Host "   ✅ PostgreSQL is running" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  PostgreSQL is stopped. Starting..." -ForegroundColor Yellow
        Start-Service $pgService.Name
        Write-Host "   ✅ PostgreSQL started" -ForegroundColor Green
    }
} else {
    Write-Host "   ❌ PostgreSQL not found! Please install PostgreSQL first." -ForegroundColor Red
    Write-Host "   Download: https://www.postgresql.org/download/windows/" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Kiểm tra database
Write-Host "2️⃣  Checking database..." -ForegroundColor Yellow
$dbCheck = psql -U postgres -d sleepalertdb -c "SELECT 1;" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✅ Database 'sleepalertdb' exists" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Database not found. Creating..." -ForegroundColor Yellow
    Write-Host "   Please enter PostgreSQL password (default: postgres):" -ForegroundColor Cyan
    
    $createDbScript = @"
CREATE DATABASE sleepalertdb;
CREATE USER sleepadmin WITH PASSWORD 'sleepalert123';
GRANT ALL PRIVILEGES ON DATABASE sleepalertdb TO sleepadmin;
"@
    
    $createDbScript | psql -U postgres
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ Database created successfully" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Failed to create database" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Chọn mode
Write-Host "3️⃣  Select mode:" -ForegroundColor Yellow
Write-Host "   [1] Start Backend Server only" -ForegroundColor White
Write-Host "   [2] Build Android APK only" -ForegroundColor White
Write-Host "   [3] Start Both (Server + Install APK)" -ForegroundColor White
Write-Host ""

$mode = Read-Host "Enter your choice (1/2/3)"

switch ($mode) {
    "1" {
        Write-Host ""
        Write-Host "🖥️  Starting Backend Server..." -ForegroundColor Cyan
        Write-Host "   Server will run on http://localhost:8080" -ForegroundColor Yellow
        Write-Host "   Press Ctrl+C to stop" -ForegroundColor Yellow
        Write-Host ""
        Set-Location backend
        gradle run
        Set-Location ..
    }
    
    "2" {
        Write-Host ""
        Write-Host "📱 Building Android APK..." -ForegroundColor Cyan
        .\gradlew.bat :app:assembleDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✅ Build successful!" -ForegroundColor Green
            Write-Host "   APK location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "To install on device/emulator:" -ForegroundColor Cyan
            Write-Host "   .\gradlew.bat :app:installDebug" -ForegroundColor White
        } else {
            Write-Host ""
            Write-Host "❌ Build failed. Check errors above." -ForegroundColor Red
        }
    }
    
    "3" {
        Write-Host ""
        Write-Host "🚀 Starting Full Stack..." -ForegroundColor Cyan
        Write-Host ""
        
        # Start server in background
        Write-Host "1. Starting Backend Server..." -ForegroundColor Yellow
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD\backend'; gradle run"
        
        Write-Host "   ✅ Server starting in new window..." -ForegroundColor Green
        Write-Host "   Waiting 10 seconds for server to initialize..." -ForegroundColor Yellow
        Start-Sleep -Seconds 10
        
        # Build and install app
        Write-Host ""
        Write-Host "2. Building Android App..." -ForegroundColor Yellow
        .\gradlew.bat :app:assembleDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "   ✅ Build successful!" -ForegroundColor Green
            
            Write-Host ""
            Write-Host "3. Installing on device/emulator..." -ForegroundColor Yellow
            .\gradlew.bat :app:installDebug
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "   ✅ App installed!" -ForegroundColor Green
                
                Write-Host ""
                Write-Host "4. Launching app..." -ForegroundColor Yellow
                adb shell am start -n com.sleepalert.app/.MainActivity
                
                Write-Host ""
                Write-Host "🎉 All done!" -ForegroundColor Green
                Write-Host ""
                Write-Host "Server: http://localhost:8080 (running in separate window)" -ForegroundColor Cyan
                Write-Host "App: Installed and launched on device/emulator" -ForegroundColor Cyan
            } else {
                Write-Host "   ❌ Install failed. Is device/emulator connected?" -ForegroundColor Red
                Write-Host "   Check with: adb devices" -ForegroundColor Yellow
            }
        } else {
            Write-Host "   ❌ Build failed. Check errors above." -ForegroundColor Red
        }
    }
    
    default {
        Write-Host ""
        Write-Host "❌ Invalid choice. Please run again and select 1, 2, or 3." -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=========================" -ForegroundColor Cyan
Write-Host "For detailed instructions, see SETUP_GUIDE.md" -ForegroundColor Gray
