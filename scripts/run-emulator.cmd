@echo off
rem 에뮬레이터 켜기 (AVD: Pleos_Connect_v2_1)
setlocal
if "%ANDROID_HOME%"=="" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "AVD=Pleos_Connect_v2_1"
start "Pleos Connect Emulator" "%ANDROID_HOME%\emulator\emulator.exe" -avd %AVD% -no-snapshot-load -gpu auto
endlocal
