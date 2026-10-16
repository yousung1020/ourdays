# OurDays

연인이 함께 일정을 계획하고, 기념일을 챙기고, 함께한 순간을 기록하는 커플 전용 앱입니다.

기존 캘린더 앱은 개인의 시간 관리에 맞춰져 있어서, 두 사람의 일정과 추억을 한 곳에서 보기 어렵습니다. OurDays는 두 사람의 시간과 경험을 함께 관리하는 것을 목표로 합니다.

## 주요 기능

- **일정**: 개인 일정, 상대방에게 공개하는 일정, 두 사람이 함께하는 공동 일정을 하나의 캘린더에서 봅니다.
- **기념일**: 사귄 날, 생일 같은 중요한 날짜를 등록하고 D-Day를 확인합니다.
- **추억**: 사진과 메모를 날짜별로 남기고 타임라인에서 다시 볼 수 있습니다.
- **커플 연결**: 초대 코드로 두 사용자를 하나의 커플로 연결합니다.
- **일정과 추억 연결**: 어떤 날의 일정과 그날의 기록을 함께 볼 수 있습니다.

## 기술 스택

**Android**
Kotlin, Jetpack Compose, Material 3, MVVM, Coroutines + Flow, Hilt, Retrofit + OkHttp, Room, DataStore

**Backend**
Kotlin, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL

**Infra**
Raspberry Pi 4 (4GB), Docker Compose, 외장 SSD(사진 저장), Firebase Cloud Messaging(푸시 알림), GitHub Actions

백엔드는 모듈러 모놀리스로 시작합니다. 규모가 작은 단계에서 MSA를 도입하지 않고, 도메인별 패키지로 나눠서 필요할 때 분리할 수 있게 둡니다.

```text
backend/src/main/kotlin/com/ourdays/
├── auth/
├── member/
├── couple/
├── calendar/
├── schedule/
├── anniversary/
├── memory/
├── media/
├── notification/
└── global/
```

사진 원본과 썸네일은 외장 SSD에 저장하고, PostgreSQL에는 메타데이터만 둡니다. 사진은 인증된 API를 거쳐서만 내려갑니다.

## 로드맵

| Phase | 내용 |
| --- | --- |
| 0. 기획·설계 | 요구사항, UX, ERD, API, 아키텍처 확정 |
| 1. 기반 구축 | Android·Spring Boot 프로젝트 생성, PostgreSQL 연결, API 호출 확인 |
| 2. 회원·커플 | 회원가입·로그인, 커플 초대와 연결 |
| 3. 일정 | 월간 캘린더, 일정 CRUD, 개인·공동 구분 |
| 4. 기념일 | 기념일 CRUD, D-Day 계산, 홈 표시 |
| 5. 추억 | 사진 업로드, 썸네일, 타임라인, 일정 연결 |
| 6. 실사용 안정화 | 알림, 오류 처리, 백업·복구, 보안 점검 |
| 7. 베타·출시 | 외부 테스터 피드백, 정책 정비, 배포 준비 |

첫 목표(MVP)는 로그인, 커플 연결, 일정·기념일·추억 기본 기능, 사진 저장과 조회, 라즈베리파이 배포까지입니다. Google Calendar 연동, 지도 기반 추억 탐색, 결제, iOS 지원 등은 MVP 이후로 미룹니다.

## 보안

- 모든 요청에서 서버가 인증과 권한을 확인합니다. 클라이언트가 보낸 사용자 ID나 커플 ID는 믿지 않습니다.
- 커플 간 데이터는 서로 격리되며, 연결을 해제하면 접근 권한을 바로 회수합니다.
- 업로드한 파일은 형식, MIME 타입, 크기를 검사하고 파일명은 서버에서 새로 만듭니다.
- 사진의 EXIF 같은 위치 정보는 저장하지 않습니다.

## 문서

- 제품 기획서 v1.0 (2026.10.09 초안): 제품 범위와 개발 방향을 정리했습니다. 세부 정책은 아직 확정 전입니다.
- [API 명세서](backend/docs/api/api-specification.md) (v0.1 초안)
- [백엔드 컨벤션](backend/docs/README.md)
- [AGENTS.md](AGENTS.md): 에이전트와 작업할 때 따르는 저장소 규칙

## 현재 상태

기획 단계입니다. 구현 전에 다음 항목을 정해야 합니다.

- 로그인 방식 (이메일 인증 또는 Google 로그인)
- 커플 연결 해제 후 공동 일정과 추억의 처리 방식
- 공동 일정의 수정·삭제 권한
- 사진 용량 제한과 이미지 최적화 기준
- Android 최소 지원 버전
- API 응답과 오류 형식

## 라이선스

미정
