# Architecture & Responsibility Convention

> 템플릿이다. `<...>`로 표시한 부분과 "현재 구조", "도메인 책임", "확장 지점", "현재 구현에서 주의할 사실"은 프로젝트에 맞게 채운다. 나머지 규칙은 계층형 Spring 백엔드를 기준으로 그대로 쓸 수 있다.

## 목적

현재 백엔드의 구조, 의존 방향, 확장 지점을 빠르게 파악하기 위한 지도다. 새 책임이 어느 도메인과 계층에 속하는지 판단할 때 쓴다. 계층별 세부 규칙은 [controller](./controller_convention.md), [service](./service_convention.md), [dto](./dto_convention.md), [converter](./converter_convention.md), [exception](./exception_convention.md), [testing](./testing_convention.md) 문서를 따르고 여기서 반복하지 않는다.

## 현재 구조

소스 루트는 `<base.package>`이다. 아래는 형식 예시다.

```text
src/main/java/<base/package>
├── domain
│   ├── <feature-a>   <한 줄 책임>
│   ├── <feature-b>   <한 줄 책임>
│   └── <feature-c>   <한 줄 책임>
└── global
    ├── apiPayload  공통 응답·예외 응답
    ├── auth        인증 filter와 보안 adapter
    ├── config      애플리케이션 infrastructure bean
    ├── entity      공통 audit Entity
    ├── properties  환경 설정 binding
    └── util        여러 도메인이 쓰는 공통 utility
```

- 기능은 `domain/<feature>`에 묶고(`controller → service → repository`), 여러 도메인이 쓰는 인프라만 `global/`에 둔다.
- 테스트는 `src/test/java` 아래에서 운영 코드 패키지를 그대로 따른다.

기본 의존 방향:

```text
Controller → CommandService / QueryService → Repository, Converter, 외부 Client, 협력 Service → Entity / 외부 시스템
```

도메인에 고유한 흐름(입력 검증 → 변환 → 출력 생성, 메시지 수신 → 저장 → 방송 등)이 있으면 위 형식으로 한 줄씩 추가한다.

## 도메인 책임과 협력 관계

도메인마다 책임 한 줄과 협력 대상을 적는다.

- `<feature-a>`: <책임>. 협력: <다른 도메인 Service, 외부 Client, 공통 utility>
- `global`: 인증 filter, 공통 응답/예외, Redis·Jackson·HTTP client 등 infrastructure. 도메인 업무 규칙은 소유하지 않는다.

도메인 간 호출은 유스케이스 조정에 필요한 범위로 제한한다.

주요 유스케이스 흐름도 한 줄씩 적는다. 예: `<Feature>Controller` → `<Feature>CommandService` → `<Repository>` / `<Converter>`

## 주요 인터페이스와 확장 지점

새 구현체를 끼워 넣는 지점(전략, 렌더러, parser, provider 등)을 목록으로 둔다. 각 항목에 인터페이스, 현재 구현체, 하는 일을 한 줄로 적는다.

- `<Interface>` (`<ImplA>`, `<ImplB>`): <하는 일>
- `BaseErrorCode` / `BaseSuccessCode` (도메인별 enum): 공통 HTTP status, code, message 계약

사용 규칙:

- 구현체 선택이 필요한 경계에는 기존 interface를 재사용하고 중복 interface를 만들지 않는다.
- Spring이 주입한 구현체 목록을 key(`getSupportedType()` 등)로 map에 등록할 때 같은 key의 구현체를 중복 등록하지 않는다.
- 구현체는 자신의 출력, 변환 책임만 수행하고 Repository 접근이나 HTTP 응답 생성을 맡지 않는다.
- enum에 타입이 있다는 사실만으로 그 타입의 parser, 처리 구현이 완료된 것으로 판단하지 않는다.
- 새 구현체를 추가할 때 고쳐야 하는 공용 파일이 있으면 이 절에 적어 두고, 고칠 곳이 늘어나면 구조를 다시 본다.

## 입력 검증과 출력 생성의 경계

입력을 해석해 결과를 만드는 흐름이 있으면 검증 계층과 생성 계층을 나눈다.

- 검증 계층(validator, parser): 입력의 정합성만 판단한다. 결과물의 문법이나 형식은 만들지 않는다.
- 생성 계층(generator, renderer): 검증을 통과한 입력을 결과물로 바꾼다. 입력을 재검증하지 않는다. 다만 결과물 형식이 지원하지 못하는 조합(예: 한 번에 하나만 처리할 수 있는 구조에 여러 개 입력)은 생성 계층에서 명시적인 오류로 거부한다. 조용히 일부만 처리하지 않는다.

## 공통 횡단 경계

- 정상 응답은 `ApiResponse`와 도메인 SuccessCode, 예외는 `GeneralException`과 도메인 ErrorCode를 쓰고 HTTP 변환은 `GeneralExceptionAdvice`에 위임한다.
- 인증 filter 내부 예외와 인증되지 않은 요청은 각각 전용 handler가 처리한다.
- JWT 유틸은 생성, 서명, claim 검증을, token lifecycle 정책은 인증 Service가 맡는다.
- Redis 유틸은 CRUD 저장 동작만 제공하고, key 의미와 TTL 정책은 호출하는 도메인이 정한다.
- 순수 parser, validator, converter, generator는 Spring context 없이 테스트할 수 있는 구조를 우선한다.

## 현재 구현에서 주의할 사실

다음은 현재 코드의 사실이며, 목표 계약으로 추정해 바꾸지 않는다. 이 절에는 규칙과 다르게 구현된 곳, 외부로 노출되지 않는 내부 객체의 예외, 아직 구현하지 않은 지원 범위를 번호로 적는다.

1. `<규칙과 다르게 구현된 곳 또는 의도된 예외>`
2. `<enum에는 있지만 구현이 없는 타입과 거부 방식>`
3. `GeneralExceptionAdvice`의 `ResponseEntity`는 전역 예외 변환 경계의 구현이며, 일반 controller의 정상 응답 규칙과 다르다.

## 새 기능 추가 시 확인 순서

- [ ] 새 책임이 속할 도메인과 계층을 정하고 해당 세부 convention 문서를 확인한다.
- [ ] 기존 interface 확장 지점이 있는지 확인한다.
- [ ] 도메인 간 호출이 유스케이스 조정 범위를 넘지 않는지 확인한다.
- [ ] 현재 구현 사실과 future plan을 구분한다.
- [ ] 테스트 경계와 실행 범위는 `testing_convention.md`를 따른다.
