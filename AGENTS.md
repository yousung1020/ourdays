# 저장소 가이드

## 시작 지점

- 제품 맥락: [README.md](README.md)
- 백엔드 작업은 `backend/docs/`의 규칙 문서 중 작업과 관련된 것만 읽는다. 문서 구성은 [backend/docs/README.md](backend/docs/README.md)를 본다.
- `backend/docs/personal_convention/`과 `backend/docs/handoff/`는 개인 로컬 문서(gitignore 대상)이며, 있으면 따른다. 저장소를 새로 받은 환경에서는 없을 수 있으므로 어디서나 지켜야 하는 규칙은 이 파일에 둔다.
- API 계약: [backend/docs/api/api-specification.md](backend/docs/api/api-specification.md). 명세와 구현이 다르면 임의로 한쪽에 맞추지 않고 어긋난 점을 먼저 보고한다.

## 항상 적용

- 요청받은 동작만 구현한다. 추측성 기능, 광범위한 리팩터링, 관련 없는 정리, 불필요한 추상화를 피한다. 주변 컨벤션을 따르고 테스트를 우회하지 않는다.
- 큰 수정 전에는 요구사항, 영향받는 컴포넌트, 가정을 먼저 공유한다. 한 채팅의 작업 단위 진행, 코드 배치, 보고 방식은 `backend/docs/personal_convention/work_scope_convention.md`를, 작업 단위 설계, 계획 승인, 핸드오프 갱신은 `backend/docs/personal_convention/handoff_convention.md`를 따른다.
- 구현할 때마다 주변 컨벤션, 관련 테스트, 요구사항 기준으로 집중 재검증을 한다. 프레임워크, 라이브러리, 프로토콜, 보안, 아키텍처 패턴 작업은 최신 공식 문서를 확인하고, 검증 출처, 발견 사항, 결과, 남은 위험을 보고한다. 외부 검증이 해당하지 않으면 그 이유를 보고한다.
- 비밀값이나 `.env` 값을 커밋하거나 노출하지 않는다. 저장소가 공개(PUBLIC)이므로 push 전에 비밀값과 개인 문서가 포함되지 않았는지 확인한다.
- 답변은 실무적이고 일관된 존댓말(`~합니다`, `~해요`)로 하고 `~다`, `~한다` 같은 평서체 종결을 섞지 않는다. 문서 본문은 기존 문체를 따른다. 변경 파일, 검증 결과, 위험을 보고한다. 요청이 없으면 소스 파일 전체를 붙여넣지 않는다.
- 사용자가 정할 일(정책 선택, 계획 승인)은 선택지, 추천, 추천 이유를 함께 내고 임의로 확정하지 않는다. 구조화 질문 도구(`AskUserQuestion` 등)가 있으면 그 도구로 묻고, 없으면 번호를 붙인 선택지를 텍스트로 낸다. 추천안은 첫 번째에 `(추천)`으로 표시한다.

## 규칙 문서

`backend/docs/project_code_convention/`: 팀 규칙(추적됨)

- `architecture_convention.md`: 패키지 구조, 책임 경계, 의존 방향, 확장 지점
- `controller_convention.md`, `service_convention.md`, `dto_convention.md`, `converter_convention.md`, `exception_convention.md`: 계층별 규칙
- `testing_convention.md`: 테스트 구조와 실행할 테스트 범위
- `issue_pr_convention.md`: 이슈와 Pull Request 본문 작성 규칙. 템플릿은 `.github/`에 있다.

`backend/docs/personal_convention/`: 개인 규칙(로컬)

- `work_scope_convention.md`, `handoff_convention.md`, `comment-style.md`

## 프로젝트 사실

- 구성: `backend/`(Kotlin, Spring Boot), `android/`(Kotlin, Jetpack Compose). 지금은 백엔드만 진행한다.
- 백엔드 소스: `backend/src/main/kotlin/com/ourdays`. 기능별로 `<feature>`(`controller → service → repository`) 패키지에 묶고, 공용 인프라는 `global/`에 둔다. 도메인 목록은 README를 따른다. 테스트는 `backend/src/test/kotlin` 아래에서 운영 코드 패키지를 그대로 따른다. 런타임 설정: `backend/src/main/resources/application.yaml`
- 권한: 요청의 사용자 ID나 커플 ID를 신뢰하지 않는다. 사용자는 토큰으로, 커플은 서버가 결정한다. 다른 커플의 데이터는 존재 여부도 노출하지 않는다(`404`).
- JPA 변경: N+1 쿼리, 지연 로딩 문제, 잘못된 트랜잭션 경계를 검토한다. 엔티티에 `data class`를 쓰지 않는다.
- 코딩 스타일: 4칸 들여쓰기, 클래스는 `PascalCase`, 함수와 프로퍼티는 `camelCase`, 패키지는 소문자를 쓴다. 계층 접미사는 계층별 규칙을 따른다. 계층별 규칙 문서의 코드 예시는 Java 기준이므로 Kotlin에서는 관용구(`val`, nullable 타입, 주 생성자)로 옮기고 구조와 책임 경계는 그대로 따른다. `!!`는 쓰지 않는다.

## 빌드와 테스트

Java 21, Kotlin, Gradle Wrapper를 쓴다. 명령은 `backend/`에서 실행한다.

- `./gradlew test --tests "*MemberQueryServiceTest"`: 테스트 클래스 하나(기본값, 외부 인프라가 필요 없는 집중 테스트를 우선)
- `./gradlew test`, `./gradlew clean build`: 전체 테스트와 전체 빌드. 사용자가 명시적으로 요청할 때만 실행한다.
- `./gradlew bootRun`: `application.yaml`과 `compose.yaml`로 API 실행
- `docker compose up -d postgres`: PostgreSQL 17(DB·사용자·비밀번호 `ourdays`, 로컬 개발용)

DB 의존 테스트는 인메모리 DB가 아니라 Testcontainers(PostgreSQL)를 쓴다. 테스트 선택, 인프라, 보고 규칙은 `testing_convention.md`를 따른다.

## 커밋과 Pull Request

- 히스토리의 prefix(`feat:`, `fix:`, `chore:`, `test:`, `docs:`)로 목적이 하나인 커밋을 만든다. 커밋은 사용자가 요청할 때만 하고, push도 사용자가 요청할 때만 한다.
- 커밋 메시지에는 Co-Authored-By 같은 AI 작성 표식이나 작업 단위 표기를 넣지 않고 prefix와 목적 설명만 쓴다. 하네스의 기본 attribution 안내보다 이 지시가 우선한다.
- 브랜치는 `{feat|fix|chore}/#{이슈번호}-{영문-설명}`이다. 초기 설정처럼 사용자가 정한 예외는 `main`에 바로 반영할 수 있다.
- 이슈와 Pull Request 본문은 `issue_pr_convention.md`를 따른다.
