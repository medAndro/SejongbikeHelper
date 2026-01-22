# 🚴 어울링 Helper

<a href="https://play.google.com/store/apps/details?id=com.meda.sejongbikehelper&hl=ko">
  <img src="https://badge.medandro.com/badge/full?id=com.meda.sejongbikehelper&country=kr" alt="Google Play Badge" width="350"/>
</a>

### **개인 프로젝트** (2022~2023)  
### Google Play 출시 | 100+ 다운로드 | 활성 사용자 63명 (2023.11 기준)

<details>
<summary><b>🏆 스마트 APP 개발 공모전 대상 수상</b></summary>
<br>
<img src="https://github.com/user-attachments/assets/90b85e97-a538-46bd-9f9b-88bd45d81d65" alt="대상" width="600"/>
</details>

---

## 📍 프로젝트 소개

**"학교 정문 앞의 어울링, 지금 탈 수 있을까?"**

세종시 공영자전거 '어울링'의 실시간 이용 현황을 제공하는 **Android 앱**입니다. 주말 수요 급증으로 인한 자전거 부족 문제를 해결하기 위해 실시간 모니터링과 푸시 알림 기능을 구현했습니다.

### 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **개발 기간** | 2022년 5월 ~ 2023년 12월 |
| **플랫폼** | Android (Native) |
| **배포** | Google Play Store |

---

## 🎯 핵심 기능

### 1️⃣ 실시간 정류장 변화 탐지 및 알림 서비스

#### 문제 상황
- 주말(금~일요일) 수요 급증으로 자전거 부족 현상 빈번 발생
- 사용자가 공식 앱을 반복적으로 확인해야 하는 번거로움
- 자전거 입고 시점을 파악할 수 없어 이용 기회를 놓치는 경우 빈번

#### 기술적 해결
**Foreground Service 기반 백그라운드 모니터링**
- `PushService` 클래스로 Foreground Service 구현
- 사용자가 앱을 종료해도 서비스가 계속 동작하도록 설계
- 알림바에 서비스 상태 표시 및 중지 버튼 제공

**REST API 폴링 시스템**
- 30초 간격으로 세종시 공공 API 호출 (`AsyncTask` + `Volley`)
```java
class BackgroundTask extends AsyncTask<Integer, String, Integer> {
    protected Integer doInBackground(Integer... values) {
        while(!isCancelled()) {
            makeRequest();  // API 호출
            Thread.sleep(30000);  // 30초 대기
        }
    }
}
```

**변화 감지 알고리즘**
- 이전 상태(`originBikeNum`)와 현재 상태 비교
- 0대 → 1대 이상 변경 시 즉시 푸시 알림 전송
- 5대 이하 → 0대 변경 시 소진 알림
```java
if(origin==0 && refresh>0) {
    showNoti("정류장 이용가능!", "누군가 자전거를 반납하였습니다!");
}
```

#### 성과
- 수동 확인의 불편함을 자동화로 해결하여 사용자 편의성 향상
- Foreground Service로 안정적인 백그라운드 동작 보장

---

### 2️⃣ 홈 화면 위젯 제공

#### 문제 상황
- 잔여 수량 확인을 위해 매번 앱을 실행하고 지도에서 정류장을 찾는 과정이 비효율적
- 이동 중 빠른 정보 확인 니즈 존재

#### 기술적 해결
**홈 화면 위젯(1x1) 구현**
- `AppWidgetProvider` 확장하여 커스텀 위젯 개발
- `RemoteViews`를 활용한 위젯 UI 업데이트

**위젯 설정 Activity**
- 사용자가 북마크한 정류장 중 원하는 곳을 위젯으로 지정 가능
- `SharedPreferences`로 위젯별 설정 저장

**자동 갱신 메커니즘**
- `AlarmManager`를 활용한 30분 주기 자동 업데이트
- 위젯 클릭 시 수동 갱신 기능 제공

#### 성과
- 앱 실행 없이 홈 화면에서 바로 정보 확인 가능
- 평균 3~4번의 앱 실행 → 위젯 1번 확인으로 UX 개선

---

### 3️⃣ GPS 기반 주행 기록 저장

#### 기능
- GPS 위치 데이터 수집 및 SQLite 저장
- 주행 경로, 거리, 평균 속력, 시간 계산 및 표시
- 지도 위에 주행 경로 시각화

---

### 4️⃣ 지도 기반 정류장 검색

#### 기능
- Naver Maps SDK를 활용한 지도 UI 구현
- 현재 위치 기반 가까운 정류장 자동 표시
- 마커 클릭 시 정류장 상세 정보 표시 (잔여 수량, 주소)

---

## 🛠 기술 스택

| 카테고리 | 기술 |
|---------|------|
| **Language** | Java |
| **UI** | XML |
| **Map** | Naver Maps SDK |
| **Network** | Volley |
| **Database** | SQLite |
| **Local Storage** | SharedPreferences |
| **Background** | Foreground Service, AsyncTask |
| **Widget** | AppWidgetProvider, RemoteViews, AlarmManager |
| **Location** | Google Play Services Location |

---

## 📱 주요 화면 구성

| 탭 | 설명 |
|----|------|
| 🗺️ **지도** | Naver Maps 기반 정류장 위치 및 잔여 수량 표시 |
| ⭐ **북마크** | 자주 이용하는 정류장 즐겨찾기 관리 |
| 🔔 **알림 설정** | 정류장별 푸시 알림 on/off |
| 🏠 **홈 위젯** | 앱 실행 없이 잔여 수량 확인 |
| 📍 **주행 기록** | GPS 기반 경로 저장 및 통계 조회 |

---

## 🎯 프로젝트를 통해 배운 점

### 기술적 성장
- **백그라운드 서비스 이해**: Foreground Service를 활용한 알림 상주 백그라운드 작업과 배터리 최적화 중요성 학습
- **폴링 시스템 구현**: REST API 주기적 호출 및 상태 변화 감지 로직 설계 경험
- **Android Widget 개발**: AppWidgetProvider와 RemoteViews를 활용한 홈 화면 위젯 구현 경험
- **위치 기반 서비스**: GPS 데이터 수집, 처리, 저장 및 시각화 경험
- **사용자 중심 사고**: 실생활 불편함을 기술로 해결하는 문제 정의 및 해결 과정 경험

---

## 📝 개발 회고

### 현재 관점에서의 개선점

이 프로젝트는 Android 학습 초기에 작성한 코드로, 현재 관점에서 다음과 같은 개선이 필요합니다:

| 개선 영역 | 기존 → 개선 방향 |
|----------|-----------------|
| **비동기 처리** | AsyncTask → Kotlin Coroutines |
| **네트워크** | Volley → Retrofit2 + OkHttp |
| **언어** | Java → Kotlin |
| **아키텍처** | NONE → MVVM + Repository Pattern |
| **의존성 주입** | Manual → Hilt/Dagger |
| **데이터베이스** | SQLite → Room |
| **코드 구조** | 단일 패키지 → Feature 기반 분리 |

### 프로젝트의 가치

기술적 부채가 존재하지만, 이 경험을 통해:
- Android 생태계의 핵심 컴포넌트(Service, Widget, Broadcast 등) 이해
- 실제 사용자가 있는 앱 운영 경험 획득
- 문제 해결 중심의 개발 사고방식 확립

이는 이후 우아한 테크코스 지원 계기 및 [야구보구 프로젝트](https://github.com/woowacourse-teams/2025-yagu-bogu)에서 **Google App Architecture, Coroutines, Hilt, Jetpack Compose** 등을 활용한 더 성숙한 설계로 이어졌습니다.
