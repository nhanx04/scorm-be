@echo off
chcp 65001 >nul
echo ==========================================
echo    SCORM Package Generator - Quick Run
echo ==========================================
echo.

REM Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java không được tìm thấy!
    echo.
    echo Vui lòng cài đặt Java 11 hoặc cao hơn:
    echo 1. Truy cập: https://adoptium.net/
    echo 2. Tải và cài đặt Java JDK
    echo 3. Khởi động lại Command Prompt
    echo.
    pause
    exit /b 1
)

echo ✅ Java đã được cài đặt
echo.

REM Create directories
if not exist "target\classes" mkdir target\classes
if not exist "lib" mkdir lib

REM Check if we have dependencies
set HAS_DEPS=0
if exist "lib\dom4j-2.1.4.jar" set HAS_DEPS=1

if %HAS_DEPS%==0 (
    echo 📦 Đang tải dependencies...
    echo.
    
    REM Download essential JARs using PowerShell
    powershell -Command "& {
        $urls = @{
            'dom4j-2.1.4.jar' = 'https://repo1.maven.org/maven2/org/dom4j/dom4j/2.1.4/dom4j-2.1.4.jar';
            'jackson-databind-2.15.2.jar' = 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar';
            'jackson-core-2.15.2.jar' = 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar';
            'jackson-annotations-2.15.2.jar' = 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar';
            'commons-compress-1.24.0.jar' = 'https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.24.0/commons-compress-1.24.0.jar';
            'commons-io-2.11.0.jar' = 'https://repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar';
            'slf4j-api-2.0.9.jar' = 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar';
            'slf4j-simple-2.0.9.jar' = 'https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar'
        };
        
        foreach ($file in $urls.Keys) {
            $filepath = 'lib\' + $file;
            if (-not (Test-Path $filepath)) {
                Write-Host ('Đang tải: ' + $file);
                try {
                    Invoke-WebRequest -Uri $urls[$file] -OutFile $filepath -UseBasicParsing;
                    Write-Host ('✅ Hoàn thành: ' + $file);
                } catch {
                    Write-Host ('❌ Lỗi: ' + $file);
                }
            }
        }
    }"
    
    echo.
    echo ✅ Tải dependencies hoàn thành!
)

echo 🔨 Đang compile...

REM Build classpath
setlocal enabledelayedexpansion
set CP=target\classes
for %%i in (lib\*.jar) do set CP=!CP!;%%i

REM Compile
javac -cp "!CP!" -d target\classes -encoding UTF-8 src\main\java\com\scorm\generator\*.java src\main\java\com\scorm\generator\model\*.java src\main\java\com\scorm\generator\gui\*.java 2>compile_error.log

if %errorlevel% neq 0 (
    echo ❌ Lỗi compile! Xem file compile_error.log
    type compile_error.log
    pause
    exit /b 1
)

REM Copy resources
copy src\main\resources\*.* target\classes\ >nul 2>&1

echo ✅ Compile thành công!
echo.
echo 🚀 Đang khởi động ứng dụng...
echo.

REM Run
java -cp "!CP!" com.scorm.generator.ScormGeneratorApp

echo.
echo 👋 Cảm ơn bạn đã sử dụng SCORM Package Generator!
pause
