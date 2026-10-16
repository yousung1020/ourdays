# 하네스 문서

## 구성

```text
docs/harness/
├── personal_convention/      개인 로컬 규칙(저장소에 올리지 않는 것을 기본으로 한다)
│   ├── work_scope_convention.md   한 채팅 한 작업 단위, 테스트와 검증, 코드 배치, 작업 후 답변 스타일
│   ├── handoff_convention.md      핸드오프 작성과 갱신, 작업 단위 설계 기준, 계획 승인 절차
│   └── comment-style.md           Javadoc과 구현 주석 규칙
└── project_code_convention/  팀 규칙(저장소에 추적한다)
    ├── architecture_convention.md   구조, 의존 방향, 확장 지점 (템플릿, 프로젝트에 맞게 채운다)
    ├── controller_convention.md, service_convention.md, dto_convention.md,
    │   converter_convention.md, exception_convention.md   계층별 규칙
    ├── testing_convention.md        테스트 구조와 실행할 테스트 범위
    └── issue_pr_convention.md       이슈와 Pull Request 본문 작성 규칙(템플릿 섹션 안에 무엇을 쓰는지)
```

## 새 프로젝트에 적용하는 순서

1. `docs/harness/`에 `personal_convention/`과 `project_code_convention/`을 복사한다. 개인 규칙은 `.gitignore`에 추가한다.
2. `architecture_convention.md`의 `<...>`, "현재 구조", "도메인 책임", "확장 지점", "현재 구현에서 주의할 사실"을 프로젝트에 맞게 채운다.
3. 계층별 문서의 공통 이름(`ApiResponse`, `GeneralException`, `GeneralExceptionAdvice`, `XXXErrorCode`, `*ControllerDocs`, `ReqDTO/ResDTO`)이 프로젝트와 다르면 바꾼다. 예시 코드의 `Member` 도메인은 형식 예시다.
4. 저장소 루트 `AGENTS.md`(또는 `CLAUDE.md`)에 아래 내용을 넣어 에이전트가 이 문서들을 읽게 한다.
   - 요청받은 동작만 구현하고 광범위한 리팩터링을 피한다.
   - 큰 수정 전에 요구사항, 영향받는 컴포넌트, 가정을 먼저 공유한다.
   - 한 채팅의 작업 단위 진행, 코드 배치, 보고 방식은 `personal_convention/work_scope_convention.md`를, 작업 단위 설계, 계획 승인, 핸드오프 갱신은 `personal_convention/handoff_convention.md`를 따른다.
   - 계층별 규칙과 테스트 범위는 `project_code_convention/`을 따른다.
   - 사용자가 정할 일(정책 선택, 계획 승인)은 선택지, 추천, 추천 이유를 함께 내고 임의로 확정하지 않는다. 하네스가 구조화 질문 도구(`AskUserQuestion`, `request_user_input`, `AskQuestion` 등)를 제공하면 그 도구로 묻고, 없거나 현재 모드에서 쓸 수 없으면 번호를 붙인 선택지를 텍스트로 낸다. 추천안은 첫 번째에 `(추천)`으로 표시한다.
   - 빌드와 테스트 명령, 외부 인프라 시작 명령, 커밋 메시지 prefix 규칙.
   - 답변 문체(예: 일관된 존댓말)와 커밋 규칙(사용자가 요청할 때만 커밋, 커밋 메시지에 AI 작성 표식이나 작업 단위 표기를 넣지 않음). 개인 규칙 문서는 gitignore 대상이라 저장소를 새로 받은 환경에서는 없으므로, 어디서나 지켜야 하는 규칙은 추적되는 `AGENTS.md`에 둔다.
5. 핸드오프가 쓰는 경로(`docs/handoff/issue-{번호}-handoff.md`, `docs/handoff/plan/future-plan.md`, `docs/handoff/legacy/`, `docs/project-overview.md`)를 만든다. `docs/handoff/`도 개인 로컬 문서로 둘지 정한다.

## 핵심 흐름

- 이슈를 시작할 때 핸드오프에 구체적인 작업 단위 목록을 쓰고(`handoff_convention.md` "작업 단위 설계"), 사용자가 계획을 한 번 승인한다.
- 이후 한 채팅에서 단위 하나를 구현, 검증하고 `work_scope_convention.md`의 답변 스타일로 보고한 뒤 멈춘다. 사용자가 diff를 리뷰하고 직접 커밋한다.
- 계획에 없는 변경이 필요해지면 구현을 멈추고 계획 갱신안을 낸다.

## 원본과 달라진 점

- 프로젝트 고유 내용(도메인 목록, 생성기와 렌더러 확장 지점, 협업 기능 등)을 제거했고 `architecture_convention.md`는 템플릿으로 다시 썼다.
- 종료 문서 보관 디렉터리 이름은 원본에서 `regacy/`였고 여기서는 `legacy/`로 썼다.
- 제품 맥락 문서 `infra-gen-project-overview.md`는 `project-overview.md`로, 계획 문서 `backend-future-plan.md`는 `future-plan.md`로 일반화했다.
