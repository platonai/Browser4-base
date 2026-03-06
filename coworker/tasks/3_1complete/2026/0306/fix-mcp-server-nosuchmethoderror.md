# Fix bugs

[ERROR] Failures: 
[ERROR]   TestMCPServerForPluginMock.callingNonExistentToolThrowsException:294 Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NoSuchMethodError>
[ERROR]   TestMCPServerForPluginMock.callingToolWithoutNameThrowsException:307 Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NoSuchMethodError>
[ERROR]   TestMCPServerMock.callingNonExistentToolThrowsException:140 Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NoSuchMethodError>
[ERROR] Errors: 
[ERROR]   TestMCPServerForPluginMock.addToolCalculatesSumCorrectly:240 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.addToolHandlesDecimalNumbers:258 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.callingAddWithoutRequiredArgumentReturnsError:348 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.callingToolWithoutRequiredArgumentReturnsErrorResponse:320 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.echoToolExecutesCorrectly:216 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.multiplyToolCalculatesProductCorrectly:277 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.responseFormatIsMcpCompliantForErrorCases:480 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.responseFormatIsMcpCompliantForSuccessfulToolExecution:458 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.serverHandlesMultipleSequentialToolCalls:366 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerForPluginMock.serverMaintainsStateAcrossMixedSuccessfulAndFailedCalls:413 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerMock.addToolAddsTwoNumbers:97 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerMock.callingToolWithoutRequiredArgumentReturnsError:153 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerMock.echoToolReturnsInputMessage:74 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[ERROR]   TestMCPServerMock.multiplyToolMultipliesTwoNumbers:120 NoSuchMethod 'java.util.Map ai.platon.pulsar.test.mcp.MockMCPServer.callTool(java.lang.String)'
[INFO] 
[ERROR] Tests run: 44, Failures: 3, Errors: 14, Skipped: 0

