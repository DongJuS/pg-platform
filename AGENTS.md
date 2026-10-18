# Change Review Criteria

- A skeleton request means creating, for each service and feature, the Controller, DTO, ApplicationService, domain, Repository, Entity, Mapper and event/batch files, plus implementation TODOs. Write the API method/path, input/output, errors, processing order, source and undecided/deferred items in comments, and check the scope against the [per-feature file checklist](docs/prd/FEATURE_FILE_CHECKLIST.md).

- When judging whether a feature, flow, architecture or code structure needs changes, always check the latest relevant Figma frames first.
- Before changing feature flows, communication, networking or concurrency, also read the [confirmed design decisions](docs/adr/0001-financial-integration-decisions.md), and check the gap between the not-yet-applied checklist items and the current code.
- Reference Figma: [sinhan-financial-project](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project).
- Check the current content of the relevant frames. In each change proposal, state the name or node-id of the supporting frame and how it differs from the current implementation and docs.
- Use Notion, the PRD, the feature specification and external AI reviews as secondary references for cross-checking. If Figma conflicts with a decision the user has confirmed, report the two sources separately. The user's latest explicit instruction takes priority.
- If Figma is not accessible or does not state the content, mark the item as ‘needs confirmation’ and do not describe it as if a design change were confirmed.
