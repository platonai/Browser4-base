# Review MicroDOMTreeNode

Review the `MicroDOMTreeNode` class and its methods, especially focusing on the `toNanoTree` and `toNanoTreeUnfiltered` functions.

Make sure toNanoTreeInRange(0.0, 1000000.0) and toNanoTreeUnfiltered() generates the same NanoDOMTree, if not, identify
the differences and potential reasons for those differences.

```kotlin
    fun toNanoTree(): NanoDOMTree = toNanoTreeInRange(0.0, 1000000.0)

    fun toNanoTreeUnfiltered(): NanoDOMTree {
        val helper = MicroToNanoTreeHelper(this, seenChunks)
        return helper.toNanoTreeUnfiltered()
    }
```
