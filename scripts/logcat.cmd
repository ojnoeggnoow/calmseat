@echo off
rem 앱 로그만 (Ctrl+C로 끝)
setlocal
if "%ANDROID_HOME%"=="" set "ANDROID_HOME=C:\Android\Sdk"
"%ANDROID_HOME%\platform-tools\adb.exe" logcat -v time PleosVehicleSignals:* AaosCarSignals:* PleosSpeaker:* AndroidSpeaker:* AndroidRuntime:E *:S
endlocal
