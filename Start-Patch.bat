@echo off
setlocal enabledelayedexpansion
title GkteTok - Patch Tool
cd /d "%~dp0"

echo   GkteTok [LSPatch v14]
echo.
echo Working dir: %CD%
echo.

rem --- Check Java 21 ---
set "JVER="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JVER=%%~v"
    goto :verfound
)
:verfound
if not defined JVER (
    echo [ERROR] Java not found. Get JDK 21:
    echo        https://adoptium.net/temurin/releases/?version=21
    echo.
    pause
    exit /b 1
)
for /f "tokens=1 delims=." %%a in ("%JVER%") do set "JMAJOR=%%a"
echo Java         : %JVER%
if %JMAJOR% LSS 21 (
    echo.
    echo [ERROR] Java 21 required. Found: %JVER%
    echo        Download: https://adoptium.net/temurin/releases/?version=21
    echo.
    pause
    exit /b 1
)

set "TAROK=1"
where tar >nul 2>nul
if errorlevel 1 set "TAROK="

set "JAR="
for %%f in ("lspatch*.jar") do if not defined JAR set "JAR=%%~ff"
if not defined JAR (
    echo [ERROR] lspatch*.jar not found. Get it:
    echo        https://github.com/JingMatrix/LSPatch/releases/latest
    echo.
    pause
    exit /b 1
)
echo LSPatch      : %JAR%
echo.

rem  STEP 1 - SOURCE CODE CHECK
echo [1/4] Checking source code...
if exist "%~dp0module-baseline\module-baseline" echo [WARN] Nested folder: module-baseline\module-baseline
set "MJAVA="
set "KTS="
if exist "%~dp0module-baseline\app" for /r "%~dp0module-baseline\app" %%f in (Module.java) do if not defined MJAVA set "MJAVA=%%f"
if not defined MJAVA if exist "%~dp0module-baseline" for /r "%~dp0module-baseline" %%f in (Module.java) do if not defined MJAVA set "MJAVA=%%f"
if exist "%~dp0module-baseline\app" for /r "%~dp0module-baseline\app" %%f in (build.gradle.kts) do if not defined KTS set "KTS=%%f"
set "MJYENI="
set "KODVAR="
if defined MJAVA findstr /m /c:"GKTETOK-V11" "%MJAVA%" >nul 2>nul
if defined MJAVA if not errorlevel 1 set "MJYENI=1"
if defined MJAVA if not errorlevel 1 set "KODVAR=1"
set "KTSYENI="
if defined KTS findstr /m /c:"GkteTok-1.6" "%KTS%" >nul 2>nul
if defined KTS if not errorlevel 1 set "KTSYENI=1"
set "DEXJAVA="
set "SEJAVA="
if exist "%~dp0module-baseline\app" for /r "%~dp0module-baseline\app" %%f in (DexFinder.java) do if not defined DEXJAVA set "DEXJAVA=%%f"
if exist "%~dp0module-baseline\app" for /r "%~dp0module-baseline\app" %%f in (SettingsEntry.java) do if not defined SEJAVA set "SEJAVA=%%f"
set "DEXYENI="
if defined DEXJAVA findstr /m /c:"dex index:" "%DEXJAVA%" >nul 2>nul
if defined DEXJAVA if not errorlevel 1 set "DEXYENI=1"
set "SEYENI="
if defined SEJAVA findstr /m /c:"settings helpers" "%SEJAVA%" >nul 2>nul
if defined SEJAVA if not errorlevel 1 set "SEYENI=1"
echo.
if not defined MJAVA goto :kyok
if defined MJYENI goto :kyeni
echo Source file  : %MJAVA%
for %%I in ("%MJAVA%") do echo Date         : %%~tI   size: %%~zI bytes
echo Source code  : OLD [no GkteTok fix]
goto :ksummary
:kyeni
echo Source file  : %MJAVA%
for %%I in ("%MJAVA%") do echo Date         : %%~tI   size: %%~zI bytes
echo Source code  : NEW [GkteTok version]
goto :ksummary
:kyok
echo Source code  : NOT FOUND - no Module.java
:ksummary
if not defined KTS goto :ktsok
if defined KTSYENI echo Gradle       : NEW [GkteTok-1.6]
if not defined KTSYENI echo Gradle       : OLD
goto :ktsbitti
:ktsok
echo Gradle       : build.gradle.kts not found
:ktsbitti
if defined DEXYENI echo Dex reader   : NEW [origin.apk support]
if defined DEXJAVA if not defined DEXYENI echo Dex reader   : OLD
if defined KODVAR echo Code tag     : NEW [GKTETOK-V11]
if defined MJAVA if not defined KODVAR echo Code tag     : OLD [marker missing, will fix]
if not defined DEXJAVA echo Dex reader   : DexFinder.java not found
echo.

rem  STEP 2 - AUTO-FIX IF NEEDED
set "ONARIM="
set "EKSIK="
if not defined MJAVA set "EKSIK=1"
if not defined MJYENI set "EKSIK=1"
if not defined DEXYENI set "EKSIK=1"
if not defined SEYENI set "EKSIK=1"
if defined KTS if not defined KTSYENI set "EKSIK=1"
if not defined EKSIK goto :onarimgerekmez
echo [2/4] Auto-fixing source...
set "FIXDIR=%~dp0fixes\module-baseline\app\src\main\java\com\golda\patchertiktok"
set "FIXKTS=%~dp0fixes\module-baseline\app\build.gradle.kts"
if not defined MJAVA goto :onarimyok
if not exist "%FIXDIR%\Module.java" goto :onarimyok
for %%I in ("%MJAVA%") do set "MJAVADIR=%%~dpI"
for %%F in ("%FIXDIR%\*.java") do call :restore "%%~fF"
if defined KTS if exist "%FIXKTS%" copy /y "%KTS%" "%KTS%.yedek" >nul 2>nul
if defined KTS if exist "%FIXKTS%" copy /y "%FIXKTS%" "%KTS%" >nul
findstr /m /c:"startupTriggers" "%MJAVA%" >nul 2>nul
if errorlevel 1 goto :onarimhata
findstr /m /c:"origin.apk" "%MJAVADIR%DexFinder.java" >nul 2>nul
if errorlevel 1 goto :onarimhata
findstr /m /c:"GKTETOK-V11" "%MJAVA%" >nul 2>nul
if errorlevel 1 goto :onarimhata
if defined KTS findstr /m /c:"GkteTok-1.6" "%KTS%" >nul 2>nul
if defined KTS if errorlevel 1 goto :onarimhata
echo [OK] Fix applied.
echo      Backups: *.yedek next to originals.
set "ONARIM=1"
goto :onarimbitti
:onarimgerekmez
echo [2/4] No fix needed - source up to date.
goto :onarimbitti
:onarimyok
echo [SKIP] "fixes" or source folder missing.
echo        Extract the new package into the tiktok-plus folder, then rerun.
goto :onarimbitti
:onarimhata
echo [ERROR] "fixes" does not match the new package.
pause
exit /b 1
:onarimbitti
if defined EKSIK if not defined ONARIM goto :kaynakproblem
echo.

rem  STEP 3 - MODULE APK CHECK, BUILD IF NEEDED
set "TAZEAPK=%~dp0module-baseline\app\build\outputs\apk\debug\app-debug.apk"
set "APKDURUM="
if exist "%TAZEAPK%" call :apkkontrol "%TAZEAPK%"
if not defined APKDURUM set "APKDURUM=YOK"
echo [3/4] Checking module APK:
echo Module APK   : %TAZEAPK%
if exist "%TAZEAPK%" for %%I in ("%TAZEAPK%") do echo APK date     : %%~tI   size: %%~zI bytes
if "%APKDURUM%"=="OK" echo APK status   : UP TO DATE [GKTETOK R1]
if "%APKDURUM%"=="ESKI" echo APK status   : OLD [will rebuild]
if "%APKDURUM%"=="SUPHELI" echo APK status   : UNREADABLE [will rebuild]
if "%APKDURUM%"=="YOK" echo APK status   : MISSING [will build]
echo.
set "DERLE="
if defined ONARIM set "DERLE=1"
if "%APKDURUM%"=="ESKI" set "DERLE=1"
if "%APKDURUM%"=="SUPHELI" set "DERLE=1"
if "%APKDURUM%"=="YOK" set "DERLE=1"
if not defined DERLE goto :derlemebitti
echo Building... 3-6 min. Do NOT close this window.
cd /d "%~dp0module-baseline"
if not exist "gradlew.bat" goto :gradlewatch
call gradlew.bat testDebugUnitTest assembleDebug
if errorlevel 1 goto :derlemehata
cd /d "%~dp0"
if not exist "%TAZEAPK%" goto :apkyokyeni
set "APKDURUM="
call :apkkontrol "%TAZEAPK%"
if not defined APKDURUM set "APKDURUM=SUPHELI"
if "%APKDURUM%"=="OK" echo [OK] Built - verified [GKTETOK R1].
if not "%APKDURUM%"=="OK" echo [OK] Built - APK unreadable, checking marker.
call :koddogrula "%TAZEAPK%"
if not errorlevel 1 goto :kodsaglandi
if defined TEMIZDENENDI goto :tazeeski
set "TEMIZDENENDI=1"
echo.
echo [WARN] New code missing - retrying with clean build...
echo        1-2 more min. Do NOT close this window.
cd /d "%~dp0module-baseline"
call gradlew.bat clean
call gradlew.bat testDebugUnitTest assembleDebug
if errorlevel 1 goto :derlemehata
cd /d "%~dp0"
if not exist "%TAZEAPK%" goto :apkyokyeni
call :koddogrula "%TAZEAPK%"
if errorlevel 1 goto :tazeeski
:kodsaglandi
echo [OK] New code verified [GKTETOK-V11].
goto :derlemebitti
:tazeeski
echo.
echo [ERROR] GKTETOK R1 missing from built APK.
echo        Problem is on the PC - phone changes won't help.
echo        Send the WHOLE window output to the developer.
pause
exit /b 1
:gradlewatch
echo [ERROR] gradlew.bat not found: %CD%
echo        Build manually:
echo            cd /d "%~dp0module-baseline"
echo            gradlew.bat testDebugUnitTest assembleDebug
cd /d "%~dp0"
goto :son
:derlemehata
echo.
echo [ERROR] Build failed.
cd /d "%~dp0"
goto :son
:apkyokyeni
echo [ERROR] APK not found after build. Expected at:
echo        %TAZEAPK%
goto :son
:derlemebitti
echo.

rem  STEP 4 - CLEAN APK SELECTION AND PATCHING
set "MODUL=%TAZEAPK%"
if not exist "%MODUL%" goto :modulyok
echo [4/4] Scanning for clean APK to patch...
echo.
set "SAYI=0"
set "TEK="
set "TEKAD="
set "FIRSTF="
set "FIRSTN="
for %%f in ("*.apk") do call :consider "%%~ff" "%%~nxf" "%%~zf"
if !SAYI! EQU 0 goto :adayyok
set "HEDEF="
if !SAYI! EQU 1 set "HEDEF=!TEK!"
if !SAYI! GTR 1 set /p "HEDEF=Target APK, Enter for first: "
if not defined HEDEF set "HEDEF=!FIRSTF!"
if not defined HEDEF goto :adayyok
if not exist "!HEDEF!" if exist "%~dp0!HEDEF!" set "HEDEF=%~dp0!HEDEF!"
if not exist "!HEDEF!" goto :hedefyok
echo Target       : !HEDEF!
echo Module       : %MODUL%
set "HYAMALI="
if defined TAROK tar -tf "!HEDEF!" 2>nul | findstr /i /c:"assets/lspatch/config.json" >nul
if defined TAROK if not errorlevel 1 set "HYAMALI=1"
if defined HYAMALI goto :hedefyamali
goto :patchnow

rem  PATCHING
:patchnow
echo.
echo   DISK WARNING: output is like ~2x the input size.
echo.
echo Patching... 3-8 min. Do NOT close this window.
echo.
java -Xmx4g -jar "%JAR%" "!HEDEF!" -m "%MODUL%" -f
if errorlevel 1 goto :patchhata
set "SONUC="
for /f "delims=" %%f in ('dir /b /o-d "*-lspatched.apk" 2^>nul') do if not defined SONUC set "SONUC=%~dp0%%f"
if defined SONUC move /y "%SONUC%" "%~dp0GkteTok-1.6-lspatched.apk" >nul 2>nul
if exist "%~dp0GkteTok-1.6-lspatched.apk" set "SONUC=%~dp0GkteTok-1.6-lspatched.apk"
echo.
echo ============================================================
if defined SONUC echo   [DONE] Patched GkteTok APK:
if defined SONUC echo   %SONUC%
if defined SONUC for %%I in ("%SONUC%") do echo   Date: %%~tI   size: %%~zI bytes
if not defined SONUC echo   [WARN] Output not found - see output above.
echo ============================================================
echo   Patched APKs in this folder - INSTALL THE NEWEST:
for %%f in ("*-lspatched.apk") do echo     %%f   date: %%~tf
echo ON THE PHONE:
echo   1. Uninstall the OLD patched TikTok
echo   2. Copy the patched APK to the phone and install
echo   3. Open TikTok and log in with username + password
echo.
if defined SONUC if exist "%SystemRoot%\explorer.exe" "%SystemRoot%\explorer.exe" /select,"%SONUC%"
pause
exit /b 0

:patchhata
echo.
echo [FAILED] Send the error text to the developer.
echo          "origin.apk overlaps" = source is ALREADY PATCHED.
echo.
pause
exit /b 1

:adayyok
echo [ERROR] No clean APK found to patch.
echo        Put the CLEAN APK from Antisplit-M in this folder:
echo        %CD%
echo.
pause
exit /b 1

:hedefyamali
echo [ERROR] This file is ALREADY patched:
echo        !HEDEF!
echo        Input must be CLEAN. Put the Antisplit-M output here:
echo        %CD%
echo.
pause
exit /b 1

:hedefyok
echo [ERROR] File not found: "!HEDEF!"
echo.
pause
exit /b 1

:modulyok
echo [ERROR] Module APK missing: %TAZEAPK%
echo.
pause
exit /b 1

:kaynakproblem
echo.
echo [ERROR] No new module code and auto-fix failed.
echo        Steps:
echo         1. Download source again.
echo         2. Extract the package DIRECTLY into: %~dp0
echo         3. Rerun this tool
echo.
pause
exit /b 1

:son
echo.
pause
exit /b 1

rem ---------- subroutine: module APK check ----------
:apkkontrol
set "APKDURUM="
if not exist "%~1" exit /b
if not defined TAROK goto :apksupheli
del "%TEMP%\ttplus-modul.dex" >nul 2>nul
tar -xOf "%~1" classes.dex > "%TEMP%\ttplus-modul.dex" 2>nul
if not exist "%TEMP%\ttplus-modul.dex" goto :apksupheli
set "SAN=0"
set "MRK=0"
set "DEXFIX=0"
set "KODMRK=0"
findstr /m /c:"com/golda/patchertiktok" "%TEMP%\ttplus-modul.dex" >nul 2>nul
if not errorlevel 1 set "SAN=1"
findstr /m /c:"hooks installed in" "%TEMP%\ttplus-modul.dex" >nul 2>nul
if not errorlevel 1 set "MRK=1"
findstr /m /c:"assets/lspatch/origin.apk" "%TEMP%\ttplus-modul.dex" >nul 2>nul
if not errorlevel 1 set "DEXFIX=1"
findstr /m /c:"GKTETOK-V11" "%TEMP%\ttplus-modul.dex" >nul 2>nul
if not errorlevel 1 set "KODMRK=1"
if "%SAN%%MRK%%DEXFIX%%KODMRK%"=="1111" set "APKDURUM=OK"
if not defined APKDURUM if "%SAN%"=="1" set "APKDURUM=ESKI"
if not defined APKDURUM set "APKDURUM=SUPHELI"
exit /b
:apksupheli
set "APKDURUM=SUPHELI"
exit /b

rem ---------- subroutine: is new code in the built module ----------
:koddogrula
if not defined TAROK exit /b 0
del "%TEMP%\ttplus-kod.dex" >nul 2>nul
tar -xOf "%~1" classes.dex > "%TEMP%\ttplus-kod.dex" 2>nul
if not exist "%TEMP%\ttplus-kod.dex" exit /b 0
findstr /m /c:"GKTETOK-V11" "%TEMP%\ttplus-kod.dex" >nul 2>nul
if not errorlevel 1 exit /b 0
del "%TEMP%\ttplus-kod.dex" >nul 2>nul
tar -xOf "%~1" classes2.dex > "%TEMP%\ttplus-kod.dex" 2>nul
if not exist "%TEMP%\ttplus-kod.dex" exit /b 1
findstr /m /c:"GKTETOK-V11" "%TEMP%\ttplus-kod.dex" >nul 2>nul
if not errorlevel 1 exit /b 0
exit /b 1

rem ---------- subroutine: candidate check ----------
:consider
if /i "%~2"=="app-debug.apk" exit /b
if %~3 LSS 1048576 (
    echo   [SKIP - too small or corrupt] %~2
    exit /b
)
set "YAMALI="
if defined TAROK tar -tf "%~1" 2>nul | findstr /i /c:"assets/lspatch/config.json" >nul
if defined TAROK if not errorlevel 1 set "YAMALI=1"
if not defined TAROK echo %~2 | findstr /i /c:"lspatched" >nul
if not defined TAROK if not errorlevel 1 set "YAMALI=1"
if defined YAMALI (
    echo   [SKIP - already patched] %~2
    exit /b
)
set /a SAYI+=1
set "TEK=%~1"
set "TEKAD=%~2"
if not defined FIRSTF (
    set "FIRSTF=%~1"
    set "FIRSTN=%~2"
)
echo   [CANDIDATE] %~2  [%~3 bytes]
exit /b

rem ---------- subroutine: restore source file ----------
:restore
if not defined MJAVADIR exit /b
set "HEDEFAD=%~nx1"
if not exist "%MJAVADIR%%HEDEFAD%" goto :restoreyeni
if exist "%MJAVADIR%%HEDEFAD%.yedek" goto :restorecopy
copy /y "%MJAVADIR%%HEDEFAD%" "%MJAVADIR%%HEDEFAD%.yedek" >nul 2>nul
:restorecopy
copy /y "%~1" "%MJAVADIR%%HEDEFAD%" >nul
exit /b
:restoreyeni
copy /y "%~1" "%MJAVADIR%%HEDEFAD%" >nul
exit /b
