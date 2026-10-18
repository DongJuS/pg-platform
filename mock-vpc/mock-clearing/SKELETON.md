# mock-clearing implementation files

api/MockClearingController.java records the required institution behavior and candidate paths as comments. The HTTP paths, DTOs and wire contracts for acceptance and final results are not confirmed yet, so no Controller/Mapping/bean is registered.

Implementation points are split across scenario/SuccessScenario.java, TimeoutScenario.java and FailureScenario.java. Mock PG also has DuplicateWebhookScenario.java and OutOfOrderWebhookScenario.java. They do not execute real results, external sends or delays. They throw an explicit not-implemented exception.

The existing *ApiSkeleton/*ScenarioSkeleton files are initial placeholders. New feature implementation starts from the comments in the named Controller/Scenario files. Real calls to financial institutions are out of scope for this module.
