# 实现 selectOption 功能

在 WebDriver 中，selectOption() 功能，用于选择下拉菜单中的选项。

使用示例：

```kotlin
// Single selection matching the value or label
driver.selectOption("select#colors", "blue")
// multiple selection
driver.selectOption("select#colors", listOf("red", "green", "blue"))
```

需要编写测试。
