# Test failed: Anchor node should have 'target' attribute

org.opentest4j.AssertionFailedError: Anchor node should have 'target' attribute ==>
Expected :true
Actual   :false
<Click to see difference>


	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:158)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:139)
	at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:69)
	at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:41)
	at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:228)
	at ai.platon.pulsar.driver.chrome.dom.DOMStateBuilderTest.testHrefAndNavigationAttributesArePreservedInNanoDOMTree(DOMStateBuilderTest.kt:531)
