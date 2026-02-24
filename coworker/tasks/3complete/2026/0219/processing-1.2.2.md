优化 check-dependencies.ps1, check-dependencies.sh

脚本生成生成了以下文件，但是实际检查很多文件为空，你需要检查文件内容是否正确，如果不正确，需要修复。
我检查过，dependency-tree.txt 是错误的，应该有内容，这是错误的。

Files Generated:
- dependency-updates.txt: Available dependency updates
- plugin-updates.txt: Available plugin updates
- property-updates.txt: Available property updates
- dependency-analysis.txt: Dependency usage analysis
- dependency-tree.txt: Full dependency tree

