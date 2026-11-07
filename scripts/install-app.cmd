@echo off
rem 빌드 -> 설치 -> 실행 -> virtual console 포트(8088) 연결
setlocal
cd /d "%~dp0.."
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
if "%ANDROID_HOME%"=="" set "ANDROID_HOME=C:\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

call gradlew.bat :app:assembleDebug --console=plain --warning-mode=none
if errorlevel 1 exit /b 1

"%ADB%" wait-for-device
"%ADB%" shell "while [ \"$(getprop sys.boot_completed)\" != \"1\" ]; do sleep 1; done"
"%ADB%" install -r -t "app\build\outputs\apk\debug\app-debug.apk"
if errorlevel 1 exit /b 1
"%ADB%" shell am start -n com.calmseat/.MainActivity
"%ADB%" forward tcp:8088 tcp:8088
echo http://127.0.0.1:8088/
endlocal
