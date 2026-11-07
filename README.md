# 차분한 조수석

초보 운전자 옆에 탄 사람(부모님, 친구)을 위한 Pleos Connect 앱.

운전 처음 하는 사람 옆에 타면 "어어어 거기!" 하고 소리치게 되는데, 그게 오히려 운전자를 더 놀라게 한다.
이 앱은 조수석 사람한테 지금 운전자가 뭘 하고 있는지(깜빡이, 브레이크, 속도, 기어, 핸들)를 보여주고,
버튼을 누르면 차가 대신 차분한 목소리로 운전자한테 말해준다. 내려서는 오늘 어땠는지 리포트를 보여준다.

## 화면

| 주행 중 | 정차 후 |
|---|---|
| (스크린샷 추가 예정) | (스크린샷 추가 예정) |

- 왼쪽: 지금 운전자가 뭐 하는지 한 줄 ("왼쪽으로 갈 준비", "속도 줄이는 중" ...)
- 오른쪽: 동승자 버튼 5개. 누르면 TTS로 운전자한테 전달
- 차가 서면(P단 or 0km/h) 리포트 버튼 활성화. 깜빡이 사용률, 도움 준 횟수 등

## 실행

APK: `apk/` (제출 때 추가)

```
adb install apk/xxx_release.apk
```

소스 빌드:

```
gradlew.bat :app:assembleDebug
```

JDK 17, Android SDK 35 필요. Pleos SDK는 settings.gradle.kts에 적힌 maven 저장소에서 받아진다.

## 테스트 환경

- Pleos Connect Emulator v2.1 (API 34)에서 실제 신호로 동작
- 그 외 안드로이드 기기/에뮬레이터(API 28+)에서는 데모 모드로 돌아감 (가상 주행 시나리오 반복, 화면에 "데모" 표시)

에뮬레이터에서 `adb forward tcp:8088 tcp:8088` 하고 http://127.0.0.1:8088/ 열면 Virtual Console에서 깜빡이/브레이크/기어/핸들 값을 바꿀 수 있다.

## 쓴 SDK

- Vehicle SDK 2.3.2: TurnSignal, Brake, SteeringWheel, Gear
- TextToSpeech SDK 3.3.1 (ON_DEVICE)
- 속도는 Pleos SDK에 없어서 AAOS CarPropertyManager로 받음

## 구조

```
vehicle/   차 신호 (Pleos / AAOS / 데모)
voice/     TTS (Pleos, 안 되면 기본 TTS)
ui/        화면 2개
CoachViewModel.kt  신호 -> 상황 문장, 버튼 기록, 차선변경 판정
```

## 주행 중 제한

- distractionOptimized 선언
- 주행 중엔 상황 카드 + 큰 버튼만. 한 번 탭이면 끝
- 리포트는 정차 중에만, 출발하면 자동으로 닫힘
- 운전자한테 가는 건 전부 음성. 화면은 동승자용
