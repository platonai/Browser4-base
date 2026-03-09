# Fix PulsarWebDriverClickTests

Analyze the actual web page to correct the test cases, it seems that the test cases uses an old version of the web page,
so the elements are not found and the tests fail.
Update the test cases to match the current structure of the web page, ensuring that all elements are correctly identified
and interacted with. This may involve updating the locators used in the tests to find the correct elements on the page.

The test pages can be found in folder:

[generated](../../../pulsar-tests/pulsar-tests-common/src/main/resources/static/generated)

## References

[PulsarWebDriverClickTests.kt](../../../pulsar-tests/pulsar-it-tests/src/test/kotlin/ai/platon/browser4/driver/chrome/dom/PulsarWebDriverClickTests.kt)
[interactive-screens.html](../../../pulsar-tests/pulsar-tests-common/src/main/resources/static/generated/interactive-screens.html)
[generated](../../../pulsar-tests/pulsar-tests-common/src/main/resources/static/generated)
