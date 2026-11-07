@echo off
rem 에뮬레이터 켜기 (AVD: Pleos_Connect_v2_1)
rem 경로에 한글 있으면 에뮬레이터가 죽어서 전부 C:\Android 밑으로 잡아둠
setlocal
set "ANDROID_HOME=C:\Android\Sdk"
set "ANDROID_SDK_ROOT=C:\Android\Sdk"
set "ANDROID_AVD_HOME=C:\Android\avd"
set "TEMP=C:\Android\tmp"
set "TMP=C:\Android\tmp"
cd /d C:\Android
start "Pleos Connect Emulator" "%ANDROID_HOME%\emulator\emulator.exe" -avd Pleos_Connect_v2_1 -no-snapshot-load -gpu host
endlocal
