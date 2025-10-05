@echo off
setlocal enabledelayedexpansion

echo ========================================
echo SCORM Package Generator
echo ========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or higher
    pause
    exit /b 1
)

echo Java found!
echo.

REM Create directories if they don't exist
if not exist "target" mkdir target
if not exist "target\classes" mkdir target\classes

REM Check if lib directory has JAR files
dir lib\*.jar >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Dependencies not found in lib\ directory
    echo Please run the PowerShell script first: powershell -ExecutionPolicy Bypass -File download-deps.ps1
    echo.
    pause
    exit /b 1
)

echo Building classpath...

REM Build classpath with all JAR files
set CLASSPATH=target\classes
for %%i in (lib\*.jar) do (
    set CLASSPATH=!CLASSPATH!;%%i
)

echo Classpath: !CLASSPATH!
echo.

echo Compiling Java sources...

REM Compile all Java files with proper classpath
javac -cp "!CLASSPATH!" -d target\classes -encoding UTF-8 ^
    src\main\java\com\scorm\generator\model\*.java ^
    src\main\java\com\scorm\generator\*.java ^
    src\main\java\com\scorm\generator\gui\*.java

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo Compilation successful!
echo.

REM Copy resources if they exist
if exist "src\main\resources" (
    echo Copying resources...
    xcopy "src\main\resources\*.*" "target\classes\" /Y /Q >nul 2>&1
)

echo Starting SCORM Package Generator...
echo.

REM Run the application
java -cp "!CLASSPATH!" com.scorm.generator.ScormGeneratorApp

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Application failed to start
    pause
    exit /b 1
)

echo.
echo Application closed successfully.
pause
