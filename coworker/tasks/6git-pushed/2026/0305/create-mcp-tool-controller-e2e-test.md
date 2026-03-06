# Create E2E test for MCPToolController

Create an end-to-end (E2E) test for the `MCPToolController` in the `ai.platon.pulsar.rest.api.controller` package,
using `ai.platon.pulsar.rest.api.controller.MCPToolControllerE2ETest` framework.

Use real pages from Mock server to test the functionality of the `MCPToolController`.

Refer to the existing E2E test for the `CommandController` as a reference for how to structure and implement the test cases.

[CommandControllerE2ETest.kt](../../../pulsar-tests/pulsar-rest-tests/src/test/kotlin/ai/platon/pulsar/rest/api/controller/CommandControllerE2ETest.kt)

All commands supported by program.ts should be covered in the test cases to ensure comprehensive testing of the `MCPToolController`.

[program.ts](../../../sdks/browser4-cli/src/program.ts)
