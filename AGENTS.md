# 변경 검토 기준

- 스켈레톤 요청은 각 서비스의 기능별 Controller·DTO·ApplicationService·도메인·Repository·Entity·Mapper·이벤트/배치 파일과 구현 TODO를 만드는 작업이다. API 메서드/경로·입출력·오류·처리 순서·출처·미정/보류를 주석에 적고 [기능별 파일 체크리스트](docs/prd/FEATURE_FILE_CHECKLIST.md)로 범위를 확인한다.

- 기능, 흐름, 아키텍처, 코드 구조의 수정 필요 여부를 판단할 때는 항상 관련된 최신 Figma 프레임을 먼저 확인한다.
- 기능 흐름·통신·네트워크·동시성 변경 전 [확정 설계 결정](docs/adr/0001-financial-integration-decisions.md)을 함께 읽고, 미반영 체크리스트와 현재 코드의 차이를 확인한다.
- 기준 Figma: [sinhan-financial-project](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project).
- 관련 프레임의 현재 내용을 확인하고, 수정 제안에는 근거 프레임의 이름 또는 node-id와 현재 구현·문서와의 차이를 적는다.
- Notion, PRD, 기능명세서, 외부 AI 리뷰는 보조 자료로 대조한다. Figma와 사용자가 확정한 결정이 충돌하면 두 근거를 구분해 알린다. 사용자의 최신 명시적 지시를 우선한다.
- Figma에 접근할 수 없거나 해당 내용이 명시되지 않았으면 그 항목을 ‘확인 필요’로 표시하고, 설계상 수정이 확정된 것처럼 설명하지 않는다.
