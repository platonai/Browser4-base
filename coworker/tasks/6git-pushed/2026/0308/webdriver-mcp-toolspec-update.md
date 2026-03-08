# WebDriver Tools Improvement

## WebDriverToolExecutor improvement

### 引入 @MCP 注解
根据 WebDriverToolExecutor 中 callFunctionOn 的实现，为所有调用到的 WebDriver 方法增加一个注解 @MCP

示例：

```kotlin
@MCP
suspend fun selectOption(selector: String, values: List<String>): List<String>
```

### 使用 @MCP 注解生成 ToolSpec

- 在 ToolSpecGenerator 中增加对 @MCP 注解的处理：仅生成被 @MCP 注解的方法的 ToolSpec，其他方法不生成 ToolSpec。
- 检查 ToolSpecGenerator 生成 description 的逻辑
    - 如果方法上有 KDoc 注释，则使用 KDoc 注释生成 description。
    - 如果方法上没有 KDoc 注释，则使用方法名生成 description，生成规则。
    - 如果 KDoc 中有 @mcp 标签，则 @mcp 标签所在的段落加入到 description 中
    - 如果 KDoc 中没有 @mcp 标签，则整个 KDoc 注释加入到 description 中

注：@mcp 大小写不敏感

## WebDriver 中加入 drag 方法

在 WebDriver 中加入 drag 方法，来满足 WebDriverToolExecutor 中 callFunctionOn 对 drag 方法的调用。
