# OurDays API 명세서

| 항목 | 내용 |
| --- | --- |
| 문서 버전 | v0.1 (초안) |
| 작성일 | 2026.10.10 |
| 기준 문서 | 서비스 기획서 v1.0, Figma 디자인(OurDays) |
| Base URL | `https://{도메인}/api/v1` |
| 형식 | REST, JSON(UTF-8), 사진 업로드만 `multipart/form-data` |

이 문서는 구현 전 합의용 초안이다. `미확정`으로 표시한 항목은 아직 제품·기술 정책이 정해지지 않아 임시 가정으로 적었다. 가정을 바꾸면 해당 절을 함께 수정한다. 맨 아래 "미확정 사항"에 모아 두었다.

## 1. 공통 규칙

### 1.1 인증

- Google 로그인 후 서버가 발급한 **액세스 토큰**을 `Authorization: Bearer {accessToken}` 헤더로 보낸다. `/auth/*`를 제외한 모든 API에 필요하다.
- 액세스 토큰은 짧게(예: 30분), 리프레시 토큰은 길게(예: 30일) 유지하고, 리프레시 토큰은 사용할 때마다 교체한다(rotation).
- 서버는 인증뿐 아니라 **요청한 데이터에 접근할 권한**도 매번 검증한다. 클라이언트가 보낸 사용자 ID·커플 ID는 신뢰하지 않으며, 커플은 토큰의 사용자로부터 서버가 결정한다. 그래서 이 문서의 어떤 API에도 `coupleId`, `memberId` 요청 파라미터가 없다.

### 1.2 공통 응답 형식

모든 응답은 아래 형식으로 감싼다(`ApiResponse<T>`). 실제 필드명은 구현 클래스에 맞춰 확정한다.

성공:

```json
{
  "isSuccess": true,
  "code": "SCHEDULE200_1",
  "message": "일정을 조회했습니다.",
  "result": { }
}
```

실패:

```json
{
  "isSuccess": false,
  "code": "SCHEDULE404_1",
  "message": "일정을 찾을 수 없습니다.",
  "result": null
}
```

입력 검증 실패(`COMMON400_1`)는 `result`에 필드별 사유를 담는다.

```json
{
  "isSuccess": false,
  "code": "COMMON400_1",
  "message": "입력값이 올바르지 않습니다.",
  "result": [
    { "field": "title", "reason": "제목은 필수입니다." }
  ]
}
```

- 에러 코드 형식은 `{DOMAIN}{HTTP_STATUS}_{SEQUENCE}`이다(exception_convention 기준).
- 응답 메시지에는 내부 구현 정보와 개인정보를 넣지 않는다.

### 1.3 날짜·시간·시간대

| 종류 | 형식 | 예 |
| --- | --- | --- |
| 날짜 | `yyyy-MM-dd` | `2026-11-17` |
| 시각 | `HH:mm` | `19:30` |
| 시점(생성·수정 시각 등) | ISO 8601, UTC 오프셋 포함 | `2026-10-09T19:30:00+09:00` |

- 일정의 날짜·시간은 사용자가 입력한 **로컬 값 그대로** 저장하고 `timeZone`(IANA, 기본 `Asia/Seoul`)과 함께 다룬다. 종일 일정은 날짜만 쓰고 시각은 `null`이다.
- D-Day와 "오늘"은 서버가 사용자의 `timeZone` 기준으로 계산한다. 클라이언트와 서버의 시간대 차이로 값이 어긋나지 않게 한다.

### 1.4 페이지네이션

- 추억 타임라인처럼 길어지는 목록은 **커서 방식**을 쓴다. 요청은 `?cursor={값}&size={n}`(기본 20, 최대 50), 응답은 아래 형태다. 첫 요청은 `cursor`를 생략한다.

```json
{ "items": [ ], "nextCursor": "eyJkYXRlIjoi...", "hasNext": true }
```

- 일정·기념일은 기간(`from`, `to`) 기반 조회이므로 페이지네이션 대신 기간 상한(최대 62일)을 둔다.

### 1.5 멱등성

생성 API(`POST`)에는 선택 헤더 `Idempotency-Key: {UUID}`를 지원한다. 같은 키로 재요청하면 새로 만들지 않고 첫 응답을 그대로 돌려준다. 모바일 네트워크 재시도로 일정·추억이 중복 생성되는 것을 막는다. 키는 24시간 보관한다.

### 1.6 공통 에러 코드

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | `COMMON400_1` | 입력값 검증 실패 |
| 400 | `COMMON400_2` | 요청 형식 오류(JSON 파싱 실패 등) |
| 401 | `AUTH401_1` | 인증 정보 없음 또는 액세스 토큰 만료 |
| 401 | `AUTH401_2` | 유효하지 않은 토큰 |
| 403 | `COMMON403_1` | 접근 권한 없음 |
| 404 | `COMMON404_1` | 존재하지 않는 경로 |
| 405 | `COMMON405_1` | 허용되지 않는 메서드 |
| 413 | `COMMON413_1` | 요청 본문이 너무 큼 |
| 429 | `COMMON429_1` | 요청이 너무 많음 |
| 500 | `COMMON500_1` | 서버 오류 |

> 다른 사람의 데이터 ID로 조회하면 `403`이 아니라 `404`로 응답한다(존재 여부를 노출하지 않기 위해). 도메인 표의 `404`가 이 경우를 포함한다.

### 1.7 커플 연결 상태

커플 기능 API는 사용자의 커플 연결 상태에 따라 동작이 달라진다.

| 상태 | 값 | 설명 |
| --- | --- | --- |
| 연결 전 | `NONE` | 커플에 속하지 않음 |
| 초대 대기 | `INVITING` | 내가 만든 초대 코드가 유효하고 아직 입력되지 않음 |
| 승인 대기 | `PENDING` | 상대가 내 코드를 입력해 내 승인을 기다림(또는 내가 입력해 상대 승인을 기다림) |
| 연결됨 | `CONNECTED` | 활성 커플 |
| 해제됨 | `DISCONNECTED` | 연결이 해제된 직후의 상태. 새로 연결하면 `NONE`을 거치지 않고 새 커플이 된다 |

**연결 전 사용 가능 범위(확정):** 개인 일정·개인 기념일은 사용할 수 있다. 공동 일정, 상대 일정 조회, 추억은 `COUPLE403_1`로 막는다.

## 2. 인증 (Auth)

로그인 수단은 **Google 로그인만** 지원한다(확정).

### 2.1 Google 로그인

`POST /auth/google` — 인증 불필요

Android에서 Google Sign-In으로 받은 ID 토큰을 서버가 검증하고 가입(최초) 또는 로그인을 처리한다.

요청:

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs...",
  "timeZone": "Asia/Seoul"
}
```

응답 `200`:

```json
{
  "isSuccess": true,
  "code": "AUTH200_1",
  "message": "로그인했습니다.",
  "result": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "accessTokenExpiresIn": 1800,
    "isNewMember": true,
    "member": { "id": 1, "name": "유성", "email": "y***@gmail.com" },
    "couple": { "status": "NONE" }
  }
}
```

- `isNewMember`가 `true`이면 앱이 소개·커플 연결 흐름으로 보낸다.
- 에러: `AUTH401_3`(Google 토큰 검증 실패), `AUTH400_1`(필수 값 누락)

### 2.2 토큰 갱신

`POST /auth/refresh` — 인증 불필요

```json
{ "refreshToken": "eyJ..." }
```

응답 `200`: `accessToken`, `refreshToken`(새로 발급), `accessTokenExpiresIn`

- 에러: `AUTH401_4`(리프레시 토큰 만료·폐기). 이미 교체된 리프레시 토큰이 다시 오면 탈취로 보고 해당 사용자의 모든 세션을 폐기한다.

### 2.3 로그아웃

`POST /auth/logout`

```json
{ "refreshToken": "eyJ...", "deviceToken": "fcm-token-or-null" }
```

응답 `200`, `result: null`. 리프레시 토큰을 폐기하고 `deviceToken`이 있으면 푸시 수신 등록을 해제한다.

## 3. 회원 (Member)

### 3.1 내 정보 조회

`GET /members/me`

```json
{
  "id": 1,
  "name": "유성",
  "email": "yousung@gmail.com",
  "birthday": "1999-03-14",
  "profileImageUrl": null,
  "timeZone": "Asia/Seoul",
  "createdAt": "2026-10-01T10:00:00+09:00"
}
```

### 3.2 내 정보 수정

`PATCH /members/me` — 보낸 필드만 수정한다.

```json
{ "name": "유성", "birthday": "1999-03-14", "timeZone": "Asia/Seoul" }
```

| 필드 | 규칙 |
| --- | --- |
| `name` | 1~20자 |
| `birthday` | 과거 날짜, 선택 |
| `timeZone` | IANA 시간대 ID |

에러: `MEMBER400_1`(이름 길이 등 검증 실패)

### 3.3 회원 탈퇴

`DELETE /members/me`

- 커플에 연결된 상태에서도 탈퇴할 수 있으며, 먼저 연결을 해제한 것으로 처리한다(공동 데이터 처리는 `미확정`, 11장 참고).
- 응답 `200`. 이후 모든 토큰이 무효가 된다.
- 에러: `MEMBER409_1`(탈퇴 처리 중)

### 3.4 기기 등록(푸시)

`PUT /members/me/devices`

앱 실행 시, FCM 토큰이 갱신될 때 호출한다. 같은 `deviceId`가 있으면 토큰을 갱신한다.

```json
{ "deviceId": "android-uuid", "fcmToken": "fcm-token", "platform": "ANDROID" }
```

`DELETE /members/me/devices/{deviceId}` — 기기 등록 해제(로그아웃·알림 거부 시)

### 3.5 알림 설정

`GET /members/me/notification-settings`, `PATCH /members/me/notification-settings`

```json
{
  "enabled": true,
  "schedule": true,
  "sharedScheduleChange": true,
  "anniversary": true,
  "coupleEvents": true,
  "defaultScheduleReminderMinutes": 30
}
```

- 이 설정은 **서버가 푸시를 보낼지 판단하는 값**이다. 기기 OS의 알림 권한은 앱이 직접 확인하며 서버로 보내지 않는다.

### 3.6 내 기록 내보내기

`POST /members/me/exports`

내가 만든 일정·기념일·추억·사진을 압축 파일로 만드는 작업을 시작한다. 완료되면 푸시로 알리고 다운로드 링크(`GET /members/me/exports/{exportId}`)를 제공한다.

응답 `202`:

```json
{ "exportId": "e_01H...", "status": "PROCESSING" }
```

- `status`: `PROCESSING`, `READY`, `FAILED`. `READY`일 때 `downloadUrl`과 `expiresAt`(24시간)을 포함한다.
- 공동 기록의 내보내기 범위는 `미확정`이다.

### 3.7 테마 설정

테마(메인 컬러, 라이트/다크)는 **사용자별로 내 기기에만 적용**하기로 했으므로(확정) 서버 API가 없다. 앱 로컬(DataStore)에 저장한다. 여러 기기 간 동기화가 필요해지면 `GET/PUT /members/me/preferences`를 추가한다.

## 4. 커플 (Couple)

### 4.1 내 커플 정보 조회

`GET /couples/me`

연결 상태 확인, 홈 헤더, '우리' 탭, 커플 연결 관리 화면에서 쓴다.

연결됨:

```json
{
  "status": "CONNECTED",
  "coupleId": 10,
  "startedOn": "2022-11-17",
  "daysTogether": 1423,
  "connectedAt": "2026-10-01T12:00:00+09:00",
  "partner": { "id": 2, "name": "세희", "profileImageUrl": null }
}
```

연결 전 / 초대 대기 / 승인 대기:

```json
{ "status": "INVITING", "invitation": { "code": "K7Q29M", "expiresAt": "2026-10-10T12:00:00+09:00" } }
```

```json
{ "status": "PENDING", "pendingRequest": { "id": 55, "direction": "INCOMING", "requester": { "name": "세희" }, "requestedAt": "2026-10-09T21:00:00+09:00" } }
```

- `direction`: `INCOMING`(상대가 내 코드를 입력, 내가 승인해야 함), `OUTGOING`(내가 코드를 입력, 상대 승인 대기)
- `daysTogether`는 사귄 날을 **첫날로 센** 일수다(`startedOn`이 오늘이면 1). `미확정`.
- `INVITING`·`PENDING` 상태는 앱이 폴링하지 않고 푸시(`COUPLE_REQUESTED`, `COUPLE_CONNECTED`)로 갱신한다.

### 4.2 사귄 날 설정

`PATCH /couples/me`

```json
{ "startedOn": "2022-11-17" }
```

- 연결됨 상태에서 두 사람 중 누구든 설정할 수 있다. 변경하면 자동 기념일(`AUTO`)이 다시 계산되고 상대에게 알림이 간다.
- 에러: `COUPLE400_1`(미래 날짜), `COUPLE403_1`(연결 전)

### 4.3 초대 코드 생성

`POST /couples/invitations`

```json
// 응답 201
{ "code": "K7Q29M", "expiresAt": "2026-10-10T12:00:00+09:00" }
```

- 코드는 6자리 영대문자·숫자(혼동되는 `0 O 1 I` 제외), 유효기간 24시간, **1회만** 사용할 수 있다.
- 이미 유효한 코드가 있으면 그 코드를 폐기하고 새로 만든다("새 코드 만들기").
- 코드 생성은 사용자당 시간당 5회로 제한한다.
- 에러: `COUPLE409_1`(이미 연결됨), `COUPLE429_1`(생성 횟수 초과)

### 4.3-1 초대 코드 조회 / 폐기

`GET /couples/invitations/active` — 현재 유효한 내 코드(없으면 `404`, `COUPLE404_1`)
`DELETE /couples/invitations/active` — 코드 폐기

### 4.4 초대 코드 입력(연결 요청)

`POST /couples/connections`

```json
{ "code": "K7Q29M" }
```

응답 `201`:

```json
{ "requestId": 55, "status": "PENDING", "inviter": { "name": "세희" } }
```

- 대소문자는 구분하지 않는다. 코드를 입력하면 곧바로 연결되지 않고, **코드를 만든 사람의 승인**을 받아야 한다(양측 동의).
- 한 사용자가 코드를 연속으로 틀리면(5회/10분) 잠시 막는다.
- 에러
  - `COUPLE400_2` 코드 형식 오류
  - `COUPLE404_2` 코드가 없거나 만료·사용됨(두 경우를 구분하지 않는다. 추측 방지)
  - `COUPLE409_1` 내가 이미 연결됨
  - `COUPLE409_2` 상대가 이미 다른 사람과 연결됨
  - `COUPLE400_3` 내 코드를 내가 입력
  - `COUPLE429_2` 입력 시도 초과

### 4.5 연결 요청 승인 / 거절

`POST /couples/connections/{requestId}/accept`
`POST /couples/connections/{requestId}/reject`

응답 `200`. 승인하면 커플이 생성되고 `GET /couples/me`가 `CONNECTED`를 돌려준다. 양쪽에 푸시(`COUPLE_CONNECTED`)를 보낸다.

- 승인하는 사람은 코드를 만든 사람이다. 에러: `COUPLE404_3`(요청 없음·이미 처리됨), `COUPLE409_1`, `COUPLE409_2`

### 4.6 연결 해제

`DELETE /couples/me/connection`

- 응답 `200`. 두 사람 모두 공동 데이터 접근이 즉시 막히고, 상대에게 해제 알림(`COUPLE_DISCONNECTED`)을 보낸다.
- 해제 후 데이터 처리는 `미확정`이다. 현재 가정:
  - 개인 일정·개인 기념일은 소유자에게 남는다.
  - 공동 일정·공동 기념일·추억은 **두 사람 모두에게 비공개**로 전환해 보관한다(보관 기간·내보내기·재연결 시 복원 여부는 미정).
  - 해제 직전에 `POST /members/me/exports`로 내 기록을 내보낼 수 있게 안내한다.
- 에러: `COUPLE404_4`(연결된 커플 없음)

## 5. 일정 (Schedule)

### 5.1 일정 유형과 접근 규칙

| 유형 | `ownerType` | 설명 |
| --- | --- | --- |
| 개인 일정 | `PERSONAL` | 내가 관리. `visibility`로 상대 공개 여부 결정 |
| 공동 일정 | `SHARED` | 두 사람이 함께 관리 |

응답에는 요청자 기준의 `owner` 값이 들어간다.

| `owner` | 의미 | 화면 표기 |
| --- | --- | --- |
| `ME` | 내 개인 일정 | 나(파랑) |
| `PARTNER` | 상대의 **공개** 개인 일정 | 상대(코랄) |
| `SHARED` | 공동 일정 | 함께(골드) |

| 작업 | 내 개인 | 상대 공개 개인 | 상대 비공개 개인 | 공동 |
| --- | --- | --- | --- | --- |
| 조회 | 가능 | 가능 | **존재 여부도 노출하지 않음**(목록에서 제외, 상세는 404) | 가능 |
| 수정·삭제 | 가능 | 불가(403) | - | 가능(양쪽 모두) |

- 연결 전에는 `PERSONAL` 일정만 만들고 조회할 수 있다.
- 공동 일정을 수정·삭제하면 상대에게 푸시(`SCHEDULE_CHANGED`, `SCHEDULE_DELETED`)를 보낸다. 수정 권한은 `미확정`이며 현재는 양쪽 모두 허용한다.

### 5.2 일정 목록 조회

`GET /schedules?from=2026-10-01&to=2026-10-31&owner=ME,PARTNER,SHARED`

| 파라미터 | 필수 | 설명 |
| --- | --- | --- |
| `from`, `to` | O | 조회 기간(양 끝 포함, 최대 62일). 기간과 하루라도 겹치는 일정을 모두 반환 |
| `owner` | X | 쉼표로 구분한 필터. 생략하면 전체. 캘린더의 나/세희/함께 필터 칩에 대응 |

응답 `200`:

```json
{
  "items": [
    {
      "id": 101,
      "title": "퇴근 후 저녁",
      "owner": "SHARED",
      "ownerType": "SHARED",
      "visibility": null,
      "allDay": false,
      "startDate": "2026-10-09",
      "startTime": "19:00",
      "endDate": "2026-10-09",
      "endTime": "21:00",
      "location": "성수동 ○○식당",
      "editable": true,
      "hasMemory": false
    }
  ]
}
```

- 목록 응답은 가볍게 유지한다(설명·알림 등은 상세에서). `hasMemory`는 연결된 추억 유무다.
- 같은 날 일정이 많아도 한 번에 받는다(기간 상한이 있으므로).

### 5.3 일정 생성

`POST /schedules`

```json
{
  "title": "영화 보기",
  "ownerType": "SHARED",
  "visibility": null,
  "allDay": false,
  "startDate": "2026-10-17",
  "startTime": "19:30",
  "endDate": "2026-10-17",
  "endTime": "21:40",
  "timeZone": "Asia/Seoul",
  "location": "CGV 용산아이파크몰",
  "description": "팝콘은 반반",
  "reminderMinutes": 30,
  "color": null
}
```

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `title` | O | 1~50자 |
| `ownerType` | O | `PERSONAL` / `SHARED` |
| `visibility` | `PERSONAL`일 때 O | `PUBLIC` / `PRIVATE`. `SHARED`이면 `null`(항상 공개) |
| `allDay` | O | `true`면 `startTime`·`endTime` 불필요 |
| `startDate`, `endDate` | O | `endDate >= startDate` |
| `startTime`, `endTime` | 시간 일정일 때 O | 같은 날이면 `endTime >= startTime` |
| `location` | X | 최대 100자 |
| `description` | X | 최대 1000자 |
| `reminderMinutes` | X | `null`=알림 없음. 0, 5, 10, 30, 60, 1440 중 하나 |
| `color` | X | 개인 일정 색 지정은 보류. 현재는 소유자 색만 사용(`null`) |

응답 `201`: 생성된 일정 상세(5.4 형식)

- 에러: `SCHEDULE400_1`(기간 오류), `SCHEDULE400_2`(visibility 규칙 위반), `COUPLE403_1`(연결 전 `SHARED` 생성)

### 5.4 일정 상세 조회

`GET /schedules/{scheduleId}`

```json
{
  "id": 101,
  "title": "영화 보기",
  "owner": "SHARED",
  "ownerType": "SHARED",
  "visibility": null,
  "allDay": false,
  "startDate": "2026-10-17",
  "startTime": "19:30",
  "endDate": "2026-10-17",
  "endTime": "21:40",
  "timeZone": "Asia/Seoul",
  "location": "CGV 용산아이파크몰",
  "description": "팝콘은 반반",
  "reminderMinutes": 30,
  "editable": true,
  "createdBy": { "id": 2, "name": "세희" },
  "createdAt": "2026-10-05T10:00:00+09:00",
  "updatedBy": { "id": 1, "name": "유성" },
  "updatedAt": "2026-10-08T09:00:00+09:00",
  "memory": { "id": 31, "title": "가을 첫 캠핑" }
}
```

- `memory`는 연결된 추억이 없으면 `null`.
- 에러: `SCHEDULE404_1`(없음 또는 접근 불가)

### 5.5 일정 수정

`PATCH /schedules/{scheduleId}` — 보낸 필드만 수정한다. 필드 규칙은 5.3과 같다.

- 개인 일정을 `SHARED`로, 또는 그 반대로 바꾸는 유형 변경은 허용한다. `PERSONAL → SHARED`는 상대에게 새 공동 일정으로 보인다.
- 에러: `SCHEDULE403_1`(상대의 개인 일정 수정 시도), `SCHEDULE404_1`, `SCHEDULE400_1`, `SCHEDULE409_1`(동시 수정 충돌, 8장 참고)

### 5.6 일정 삭제

`DELETE /schedules/{scheduleId}` → `200`, `result: null`

- 연결된 추억은 **삭제되지 않고** 일정 연결만 해제된다.
- 삭제는 즉시 반영되며, 개인 일정 삭제는 앱이 10초간 "실행 취소"를 제공하고 취소 시 `POST /schedules/{scheduleId}/restore`로 복원한다(소프트 삭제 후 7일 보관).
- 에러: `SCHEDULE403_1`, `SCHEDULE404_1`

## 6. 캘린더 집계 (Calendar)

캘린더 화면 전용 조회 API다. 월간 마커와 날짜별 목록을 한 번에 받는다.

### 6.1 월간 마커

`GET /calendar/months/{yyyy-MM}?owner=ME,PARTNER,SHARED`

```json
{
  "month": "2026-10",
  "days": [
    { "date": "2026-10-03", "schedules": { "ME": 0, "PARTNER": 0, "SHARED": 1 }, "hasAnniversary": false, "hasMemory": true },
    { "date": "2026-10-09", "schedules": { "ME": 1, "PARTNER": 1, "SHARED": 1 }, "hasAnniversary": false, "hasMemory": false }
  ]
}
```

- 마커가 있는 날짜만 반환한다. 일정 개수는 셀에 쓰지 않고 유무만 표시하므로(0/1 이상) 개수는 참고용이다.
- 이전·다음 달은 클라이언트가 미리 한 번 더 요청해 캐시한다.

### 6.2 날짜별 목록

`GET /calendar/days/{yyyy-MM-dd}`

```json
{
  "date": "2026-10-03",
  "schedules": [ { "id": 88, "title": "가을 캠핑", "owner": "SHARED", "allDay": true, "startDate": "2026-10-03", "endDate": "2026-10-04", "startTime": null, "endTime": null, "location": null, "editable": true, "hasMemory": true } ],
  "anniversaries": [ ],
  "memories": [ { "id": 31, "title": "가을 첫 캠핑", "date": "2026-10-03", "memo": "모닥불 앞에서...", "thumbnailUrl": "/media/501?size=thumb", "photoCount": 4 } ]
}
```

- 일정 항목은 5.2와 같은 형식, 기념일은 7.2와 같은 형식이다.
- "일정 → 추억" 흐름을 위해 같은 날의 일정과 추억을 함께 내려준다.

## 7. 기념일 (Anniversary)

### 7.1 종류

| `type` | 설명 | 수정·삭제 |
| --- | --- | --- |
| `COUPLE` | 사용자가 만든 커플 기념일(여행 기념일 등) | 가능 |
| `BIRTHDAY` | 생일 | 가능 |
| `CUSTOM` | 직접 만든 기념일 | 가능 |
| `AUTO` | 사귄 날 기준 자동 생성(100일 단위, N주년 등) | 불가. 사귄 날이 바뀌면 재계산 |

- 기념일은 커플 공유가 기본이다. 연결 전에는 내 개인 기념일(`BIRTHDAY`, `CUSTOM`)만 만들 수 있고, 연결되면 두 사람 모두에게 보인다. 연결 전에 만든 개인 기념일의 연결 후 공유 여부는 `미확정`.
- 자동 기념일의 생성 범위와 기본 알림은 `미확정`이다(예: 100일 단위 + 주년).

### 7.2 기념일 목록 조회

`GET /anniversaries?scope=UPCOMING&type=COUPLE,BIRTHDAY&limit=20`

| 파라미터 | 설명 |
| --- | --- |
| `scope` | `UPCOMING`(오늘 이후, D-Day 오름차순), `PAST`(지난 것, 최근순), `ALL`. 기본 `UPCOMING` |
| `type` | 쉼표로 구분. 생략하면 전체. 화면의 전체/커플/생일/직접 추가 칩에 대응 |
| `limit` | 기본 20, 최대 100 |

```json
{
  "items": [
    {
      "id": 7,
      "type": "AUTO",
      "title": "4주년",
      "baseDate": "2022-11-17",
      "nextDate": "2026-11-17",
      "dDay": 39,
      "repeat": "YEARLY",
      "reminders": ["ON_DAY", "BEFORE_3_DAYS"],
      "showOnCalendar": true,
      "editable": false
    }
  ]
}
```

- `dDay`는 서버가 사용자 시간대의 "오늘" 기준으로 계산한 값이다. 양수=남음, `0`=오늘, 음수=지남(지난 기념일은 `daysSince` 필드를 쓴다).
- 반복 기념일(`YEARLY`)의 `nextDate`는 오늘 이후 가장 가까운 발생일이다. 2월 29일 기념일은 평년에 2월 28일로 계산한다(`미확정`).
- `AUTO` 중 일 단위 기념일(예: 1500일)은 `baseDate`에 사귄 날, `nextDate`에 해당 일수째 되는 날을 담는다.

### 7.3 기념일 생성

`POST /anniversaries`

```json
{
  "type": "COUPLE",
  "title": "첫 여행 기념일",
  "baseDate": "2025-05-02",
  "repeat": "YEARLY",
  "reminders": ["ON_DAY", "BEFORE_3_DAYS"],
  "showOnCalendar": true
}
```

| 필드 | 규칙 |
| --- | --- |
| `type` | `COUPLE` / `BIRTHDAY` / `CUSTOM` (`AUTO`는 생성 불가) |
| `title` | 1~30자 |
| `baseDate` | 날짜. 과거·미래 모두 가능 |
| `repeat` | `NONE` / `YEARLY` |
| `reminders` | `ON_DAY`, `BEFORE_1_DAY`, `BEFORE_3_DAYS`, `BEFORE_7_DAYS` 중 0개 이상 |
| `showOnCalendar` | 기본 `true` |

응답 `201`: 7.2의 항목 형식. 에러: `ANNIVERSARY400_1`(검증 실패), `ANNIVERSARY400_2`(`AUTO` 생성 시도), `ANNIVERSARY409_1`(같은 이름·날짜 중복)

### 7.4 기념일 수정 / 삭제

`PATCH /anniversaries/{id}`, `DELETE /anniversaries/{id}`

- `AUTO` 기념일은 `ANNIVERSARY403_1`. 존재하지 않거나 접근 불가면 `ANNIVERSARY404_1`.

## 8. 홈 (Home)

`GET /home`

홈 화면 한 번에 필요한 정보를 모아 반환한다(첫 화면 요청 수를 줄이기 위함). 각 블록은 독립적으로 비어 있을 수 있다.

```json
{
  "today": "2026-10-09",
  "couple": { "status": "CONNECTED", "daysTogether": 1423, "partner": { "name": "세희" } },
  "nearestAnniversary": { "id": 7, "title": "4주년", "nextDate": "2026-11-17", "dDay": 39, "type": "AUTO" },
  "todaySchedules": [ ],
  "recentMemories": [ ]
}
```

- `todaySchedules`: 오늘 걸치는 일정(5.2 항목 형식), 시작 시각 오름차순, 최대 10개
- `recentMemories`: 최근 추억 최대 3개(타임라인 항목 형식)
- `nearestAnniversary`: 없으면 `null`
- 연결 전에는 `couple.status`만 내려가고 공동 데이터(공동 일정·상대 일정·추억)는 비어 있다.

## 9. 추억 (Memory)

### 9.1 타임라인 조회

`GET /memories?cursor=&size=20&month=2026-10`

- 날짜 내림차순(같은 날은 최근 작성순). `month`(선택)는 해당 월만 조회한다. 월별 구분 헤더는 클라이언트가 `date`로 그룹핑한다.

```json
{
  "items": [
    {
      "id": 31,
      "title": "가을 첫 캠핑",
      "date": "2026-10-03",
      "memo": "모닥불 앞에서 고구마 구워 먹은 밤. 별이 생각보다...",
      "thumbnailUrl": "/media/501?size=thumb",
      "photoCount": 4,
      "schedule": { "id": 88, "title": "가을 캠핑" },
      "author": { "id": 1, "name": "유성" }
    }
  ],
  "nextCursor": null,
  "hasNext": false
}
```

- `memo`는 목록에서 앞 100자까지만 내려준다. 사진이 없는 추억은 `thumbnailUrl: null`.

### 9.2 추억 상세 조회

`GET /memories/{memoryId}`

```json
{
  "id": 31,
  "title": "가을 첫 캠핑",
  "date": "2026-10-03",
  "memo": "모닥불 앞에서 고구마 구워 먹은 밤. 별이 생각보다 많이 보였다.",
  "photos": [
    { "mediaId": 501, "url": "/media/501", "thumbnailUrl": "/media/501?size=thumb", "width": 4032, "height": 3024, "uploadedBy": { "id": 1, "name": "유성" } }
  ],
  "schedule": { "id": 88, "title": "가을 캠핑", "owner": "SHARED" },
  "author": { "id": 1, "name": "유성" },
  "createdAt": "2026-10-03T22:10:00+09:00",
  "updatedAt": "2026-10-04T08:00:00+09:00"
}
```

- 사진 순서는 `photos` 배열 순서다. 첫 번째 사진이 대표 사진(타임라인 썸네일)이다.
- `url`은 인증이 필요한 API 경로다(10장). 공개 URL이 아니다.

### 9.3 추억 생성

`POST /memories`

```json
{
  "title": "가을 첫 캠핑",
  "date": "2026-10-03",
  "memo": "모닥불 앞에서 고구마 구워 먹은 밤",
  "scheduleId": 88,
  "mediaIds": [501, 502, 503]
}
```

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `title` | O | 1~50자 |
| `date` | O | 날짜 |
| `memo` | X | 최대 2000자 |
| `scheduleId` | X | 연결할 일정. 일정이 있으면 `date`를 일정 시작일로 맞추는 것은 클라이언트가 한다(서버는 강제하지 않음) |
| `mediaIds` | X | 10장 이하. 미리 업로드해 `READY` 상태인 내 사진만 가능 |

- 연결할 수 있는 일정은 접근 가능한 일정(내 개인, 상대 공개, 공동)이다. 이미 다른 추억이 연결된 일정은 `MEMORY409_1`(일정당 추억 1개, `미확정`).
- 연결 전에는 추억 API를 쓸 수 없다(`COUPLE403_1`).
- 응답 `201`: 9.2 형식. 에러: `MEMORY400_1`(검증 실패), `MEMORY400_2`(사진 10장 초과·소유하지 않은 사진), `MEMORY404_2`(일정 없음)

### 9.4 추억 수정 / 삭제

`PATCH /memories/{memoryId}` — 제목·날짜·메모·일정 연결·사진 구성을 수정한다.

- 사진 구성은 `mediaIds` 배열 전체를 보내 교체한다(순서 포함). 목록에서 빠진 사진은 추억에서 분리되고 삭제 대기(10.4)가 된다. `scheduleId: null`로 일정 연결을 해제한다.
- 작성자뿐 아니라 연결된 상대도 수정·삭제할 수 있다(공동 공간, `미확정`). 상대가 수정하면 푸시를 보내지 않는다.

`DELETE /memories/{memoryId}` → `200`. 연결된 사진도 함께 삭제(실제 파일은 10.4 정책에 따라 정리). 일정은 영향받지 않는다.

- 에러: `MEMORY404_1`(없음·접근 불가)

## 10. 사진 (Media)

사진은 외장 SSD에 저장하고, DB에는 메타데이터만 둔다. 사진 파일은 **인증된 API를 통해서만** 내려간다(서버 저장 경로·정적 URL 노출 없음).

### 10.1 업로드

`POST /media` — `multipart/form-data`

| 파트 | 설명 |
| --- | --- |
| `file` | 이미지 1장 |
| `clientId` | 클라이언트가 만든 임의 문자열(선택). 응답과 함께 돌려줘 업로드 실패·재시도 UI를 연결한다 |

- 한 요청에 **한 장씩** 업로드한다. 여러 장은 앱이 병렬(최대 3개)로 보내며, 장별로 성공·실패가 독립적이라 일부 실패 시 실패한 사진만 "다시 시도"할 수 있다(추억 기록 화면의 업로드 실패 상태).
- 허용 형식: JPEG, PNG, WebP, HEIC(서버가 JPEG로 변환). 확장자·Content-Type이 아니라 **파일 내용**으로 형식을 검사한다.
- 최대 크기: 파일당 15MB(`미확정`). 앱이 업로드 전에 긴 변 2560px 이하로 줄인다.
- 서버 처리: 파일명은 서버가 새로 만들고, EXIF의 위치정보 등은 제거하며(방향 정보는 반영), 썸네일(긴 변 512px)을 만든다.

응답 `201`:

```json
{
  "mediaId": 501,
  "clientId": "tmp-1",
  "status": "READY",
  "width": 4032,
  "height": 3024,
  "contentType": "image/jpeg",
  "size": 2412331
}
```

- `status`: `READY`(썸네일까지 완료), `PROCESSING`(썸네일 생성 중. 곧 `READY`). 썸네일 생성은 업로드 응답 전에 끝내는 것을 기본으로 한다.
- 업로드한 사진은 추억에 연결되기 전까지 **미연결** 상태이며, 24시간 안에 연결되지 않으면 서버가 정리한다.
- 에러: `MEDIA400_1`(파일 없음), `MEDIA400_2`(허용되지 않는 형식), `MEDIA413_1`(크기 초과), `MEDIA429_1`(업로드 속도 제한), `MEDIA507_1`(저장 공간 부족)

### 10.2 사진 조회

`GET /media/{mediaId}?size=thumb`

- `size`: `thumb`(기본값 아님, 목록용), 생략하면 원본. 응답은 이미지 바이너리다(`Content-Type: image/jpeg` 등).
- 같은 커플의 추억에 연결된 사진, 또는 내가 업로드한 미연결 사진만 볼 수 있다. 그 외에는 `MEDIA404_1`.
- 캐시: `Cache-Control: private, max-age=86400`, `ETag`를 지원한다. 앱은 이미지 로더에 `Authorization` 헤더를 붙이도록 설정한다.

### 10.3 미연결 사진 삭제

`DELETE /media/{mediaId}` — 아직 추억에 연결되지 않은 내 사진을 삭제한다. 연결된 사진은 추억 수정(9.4)으로만 뺄 수 있다(`MEDIA409_1`).

### 10.4 삭제 정책

- 추억·사진 삭제는 DB에서 먼저 처리하고, 실제 파일 삭제는 비동기로 정리한다. 파일 삭제가 실패하면 재시도 대상으로 남겨, DB와 SSD 사이에 고아 파일이 쌓이지 않게 한다.
- 파일 저장이 실패하면 DB에 메타데이터를 남기지 않는다(보상 처리).

## 11. 푸시 알림 (FCM)

서버가 FCM으로 보내는 `data` 메시지 형식이다. FCM은 전달을 보장하지 않으므로 중요한 정보는 서버에 저장하고, 앱은 알림을 탭하거나 포그라운드 진입 시 해당 API로 다시 조회한다.

| `type` | 대상 | 시점 | 탭 시 이동 |
| --- | --- | --- | --- |
| `SCHEDULE_REMINDER` | 일정 참여자 | 일정 시작 `reminderMinutes` 전 | 일정 상세 |
| `SCHEDULE_CHANGED` | 상대 | 공동 일정 수정 | 일정 상세 |
| `SCHEDULE_DELETED` | 상대 | 공동 일정 삭제 | 캘린더 |
| `ANNIVERSARY_REMINDER` | 두 사람 | 설정한 기념일 알림 시점(당일 오전 9시 등) | 기념일 목록 |
| `COUPLE_REQUESTED` | 코드 생성자 | 상대가 코드를 입력 | 요청 승인 화면 |
| `COUPLE_CONNECTED` | 두 사람 | 승인 완료 | 홈 |
| `COUPLE_DISCONNECTED` | 상대 | 연결 해제 | 연결 해제 안내 |
| `EXPORT_READY` | 요청자 | 내보내기 완료 | 계정 > 내보내기 |

```json
{ "type": "SCHEDULE_CHANGED", "targetId": "101", "title": "영화 보기", "body": "세희가 일정을 수정했어요." }
```

- `targetId`는 이동할 리소스의 ID다. 메시지에는 일정 제목 외의 민감한 상세 내용을 넣지 않는다.
- 알림 설정(3.5)에서 끈 유형은 보내지 않는다. 비공개 일정은 상대에게 알림을 보내지 않는다.

## 12. 동기화·동시성

- 수정 가능한 리소스(일정·기념일·추억)의 상세 응답에는 `version`(정수)을 포함하고, `PATCH` 요청에 `version`을 보내면 서버가 비교한다. 다른 기기·상대가 먼저 수정했다면 `409`(`{DOMAIN}409_X`, 예: `SCHEDULE409_1`)와 함께 최신 상세를 돌려준다. 앱은 최신 내용으로 다시 보여주고 사용자가 이어서 수정한다.
- 오프라인에서 만든 변경은 앱이 큐에 쌓아 복구 후 순서대로 보내며, 생성 요청은 `Idempotency-Key`로 중복을 막는다. 오프라인 처리 범위(읽기 캐시만 vs 쓰기까지)는 `미확정`이다. 현재 시안은 읽기 캐시 + "마지막 동기화" 표시를 가정한다.
- 목록 새로고침(당겨서 새로고침)으로 상대의 변경을 가져온다. 별도의 실시간 채널(WebSocket)은 두지 않고, 푸시 + 재조회로 갱신한다.

## 13. 화면별 API 매핑

| 화면(Figma) | 사용 API |
| --- | --- |
| 로그인 | `POST /auth/google` |
| 커플 연결 · 내 코드 | `POST /couples/invitations`, `GET /couples/me`(푸시로 갱신) |
| 커플 연결 · 코드 입력 / 오류 | `POST /couples/connections` |
| 커플 연결 · 요청 승인 | `POST /couples/connections/{id}/accept`, `/reject` |
| 연결 완료 · 사귄 날 | `PATCH /couples/me` |
| 홈 | `GET /home` |
| 캘린더(월간, 필터) | `GET /calendar/months/{yyyy-MM}` |
| 캘린더 날짜 목록 | `GET /calendar/days/{date}` |
| 빠른 등록 / 상세 등록 / 수정 | `POST /schedules`, `PATCH /schedules/{id}` |
| 일정 상세 | `GET /schedules/{id}` |
| 일정 삭제 · 실행 취소 | `DELETE /schedules/{id}`, `POST /schedules/{id}/restore` |
| 기념일 목록 | `GET /anniversaries` |
| 기념일 추가 / 수정 | `POST /anniversaries`, `PATCH /anniversaries/{id}` |
| 추억 타임라인 | `GET /memories` |
| 추억 기록 | `POST /media`(장별 업로드) → `POST /memories` |
| 업로드 실패 · 재시도 | `POST /media`(실패한 사진만 다시) |
| 추억 상세 / 사진 보기 | `GET /memories/{id}`, `GET /media/{id}` |
| 우리 탭 | `GET /couples/me`, `GET /anniversaries?limit=2` |
| 테마 설정 | (서버 API 없음, 로컬 저장) |
| 알림 설정 | `GET·PATCH /members/me/notification-settings`, `PUT /members/me/devices` |
| 계정 | `GET·PATCH /members/me`, `POST /auth/logout`, `POST /members/me/exports`, `DELETE /members/me` |
| 커플 연결 관리 · 해제 | `GET /couples/me`, `DELETE /couples/me/connection` |

## 14. 도메인별 에러 코드

| 도메인 | HTTP | 코드 | 메시지(안) |
| --- | --- | --- | --- |
| AUTH | 400 | `AUTH400_1` | 필수 값이 누락되었습니다. |
| AUTH | 401 | `AUTH401_3` | Google 인증에 실패했습니다. |
| AUTH | 401 | `AUTH401_4` | 로그인이 만료되었습니다. 다시 로그인해 주세요. |
| MEMBER | 400 | `MEMBER400_1` | 회원 정보가 올바르지 않습니다. |
| MEMBER | 409 | `MEMBER409_1` | 탈퇴 처리 중인 회원입니다. |
| COUPLE | 400 | `COUPLE400_1` | 사귄 날은 오늘 이전이어야 합니다. |
| COUPLE | 400 | `COUPLE400_2` | 초대 코드 형식이 올바르지 않습니다. |
| COUPLE | 400 | `COUPLE400_3` | 내 초대 코드는 입력할 수 없습니다. |
| COUPLE | 403 | `COUPLE403_1` | 커플 연결 후 사용할 수 있습니다. |
| COUPLE | 404 | `COUPLE404_1` | 유효한 초대 코드가 없습니다. |
| COUPLE | 404 | `COUPLE404_2` | 코드가 맞지 않거나 만료되었습니다. |
| COUPLE | 404 | `COUPLE404_3` | 연결 요청을 찾을 수 없습니다. |
| COUPLE | 404 | `COUPLE404_4` | 연결된 커플이 없습니다. |
| COUPLE | 409 | `COUPLE409_1` | 이미 커플로 연결되어 있습니다. |
| COUPLE | 409 | `COUPLE409_2` | 상대가 이미 다른 사람과 연결되어 있습니다. |
| COUPLE | 429 | `COUPLE429_1` | 초대 코드를 너무 많이 만들었습니다. |
| COUPLE | 429 | `COUPLE429_2` | 잠시 후 다시 시도해 주세요. |
| SCHEDULE | 400 | `SCHEDULE400_1` | 일정 기간이 올바르지 않습니다. |
| SCHEDULE | 400 | `SCHEDULE400_2` | 공개 범위 설정이 올바르지 않습니다. |
| SCHEDULE | 403 | `SCHEDULE403_1` | 상대의 개인 일정은 수정할 수 없습니다. |
| SCHEDULE | 404 | `SCHEDULE404_1` | 일정을 찾을 수 없습니다. |
| SCHEDULE | 409 | `SCHEDULE409_1` | 다른 곳에서 먼저 수정되었습니다. |
| ANNIVERSARY | 400 | `ANNIVERSARY400_1` | 기념일 정보가 올바르지 않습니다. |
| ANNIVERSARY | 400 | `ANNIVERSARY400_2` | 자동 기념일은 직접 만들 수 없습니다. |
| ANNIVERSARY | 403 | `ANNIVERSARY403_1` | 자동 기념일은 수정할 수 없습니다. |
| ANNIVERSARY | 404 | `ANNIVERSARY404_1` | 기념일을 찾을 수 없습니다. |
| ANNIVERSARY | 409 | `ANNIVERSARY409_1` | 같은 기념일이 이미 있습니다. |
| MEMORY | 400 | `MEMORY400_1` | 추억 정보가 올바르지 않습니다. |
| MEMORY | 400 | `MEMORY400_2` | 사진을 확인해 주세요. (최대 10장) |
| MEMORY | 404 | `MEMORY404_1` | 추억을 찾을 수 없습니다. |
| MEMORY | 404 | `MEMORY404_2` | 연결할 일정을 찾을 수 없습니다. |
| MEMORY | 409 | `MEMORY409_1` | 이미 추억이 연결된 일정입니다. |
| MEDIA | 400 | `MEDIA400_1` | 업로드할 파일이 없습니다. |
| MEDIA | 400 | `MEDIA400_2` | 지원하지 않는 이미지 형식입니다. |
| MEDIA | 404 | `MEDIA404_1` | 사진을 찾을 수 없습니다. |
| MEDIA | 409 | `MEDIA409_1` | 추억에 연결된 사진은 직접 삭제할 수 없습니다. |
| MEDIA | 413 | `MEDIA413_1` | 사진 용량이 너무 큽니다. |
| MEDIA | 429 | `MEDIA429_1` | 사진을 너무 빠르게 올리고 있습니다. |
| MEDIA | 507 | `MEDIA507_1` | 저장 공간이 부족합니다. |

성공 코드는 `{DOMAIN}200_n`(조회·수정·삭제), `{DOMAIN}201_n`(생성) 형식으로 도메인별 enum에 둔다.

## 15. 미확정 사항

이 문서가 임시로 가정한 값이다. 정해지면 관련 절을 수정한다.

| 항목 | 현재 가정 | 관련 절 |
| --- | --- | --- |
| 연결 해제 후 공동 데이터 | 두 사람 모두 비공개로 보관, 보관 기간·내보내기·재연결 복원은 미정 | 4.6, 3.6 |
| 공동 일정 수정·삭제 권한 | 양쪽 모두 가능 + 상대에게 알림 | 5.1 |
| 사귄 날 기반 자동 기념일 범위 | 100일 단위 + N주년, 수정 불가 | 7.1 |
| 연결 전 만든 개인 기념일의 연결 후 공유 | 미정 | 7.1 |
| 일 수 계산 | 사귄 날을 첫날(1일)로 계산 | 4.1 |
| 2월 29일 반복 기념일 | 평년에는 2월 28일 | 7.2 |
| 추억 개별 공개 범위 | 커플 공동 공간만(나만 보기 추억 없음) | 9 |
| 일정당 추억 개수 | 1개 | 9.3 |
| 추억 수정·삭제 권한 | 작성자와 상대 모두 | 9.4 |
| 사진 장수·용량 | 추억당 10장, 파일당 15MB | 9.3, 10.1 |
| 오프라인 처리 범위 | 읽기 캐시 + 마지막 동기화 표시 | 12 |
| 응답 래퍼 필드명 | `isSuccess`/`code`/`message`/`result` | 1.2 |
| API 경로 접두사 | `/api/v1`(기획서). 컨트롤러 컨벤션 예시는 `/api`이므로 맞춰 확정 | 상단 |
| Android 최소 지원 버전 | 미정 (HEIC 변환·Edge-to-edge 범위에 영향) | 10.1 |
