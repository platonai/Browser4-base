package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.ToolCallSpec
import ai.platon.pulsar.agentic.common.AgentFileSystem
import kotlin.reflect.KClass

class FileSystemToolExecutor : AbstractToolExecutor() {

    override val domain = "fs"

    override val targetClass: KClass<*> = AgentFileSystem::class

    init {
        toolCallSpecs["writeString"] = ToolCallSpec(
            domain = domain,
            method = "writeString",
            arguments = listOf(
                ToolCallSpec.Arg("filename", "String", null),
                ToolCallSpec.Arg("content", "String", "\"\"")
            ),
            returnType = "String",
            description = "Write content to a file"
        )
        
        toolCallSpecs["readString"] = ToolCallSpec(
            domain = domain,
            method = "readString",
            arguments = listOf(
                ToolCallSpec.Arg("filename", "String", null),
                ToolCallSpec.Arg("external", "Boolean", "false")
            ),
            returnType = "String",
            description = "Read content from a file"
        )
        
        toolCallSpecs["append"] = ToolCallSpec(
            domain = domain,
            method = "append",
            arguments = listOf(
                ToolCallSpec.Arg("filename", "String", null),
                ToolCallSpec.Arg("content", "String", null)
            ),
            returnType = "String",
            description = "Append content to a file"
        )
        
        toolCallSpecs["replaceContent"] = ToolCallSpec(
            domain = domain,
            method = "replaceContent",
            arguments = listOf(
                ToolCallSpec.Arg("filename", "String", null),
                ToolCallSpec.Arg("oldStr", "String", null),
                ToolCallSpec.Arg("newStr", "String", null)
            ),
            returnType = "String",
            description = "Replace content in a file"
        )
        
        toolCallSpecs["fileExists"] = ToolCallSpec(
            domain = domain,
            method = "fileExists",
            arguments = listOf(
                ToolCallSpec.Arg("filename", "String", null)
            ),
            returnType = "Boolean",
            description = "Check if a file exists"
        )
        
        toolCallSpecs["getFileInfo"] = ToolCallSpec(
            domain = domain,
            method = "getFileInfo",
            arguments = listOf(
                ToolCallSpec.Arg("filename", "String", null)
            ),
            returnType = "Map<String, Any>",
            description = "Get information about a file"
        )
        
        toolCallSpecs["deleteFile"] = ToolCallSpec(
            domain = domain,
            method = "deleteFile",
            arguments = listOf(
                ToolCallSpec.Arg("filename", "String", null)
            ),
            returnType = "String",
            description = "Delete a file"
        )
        
        toolCallSpecs["copyFile"] = ToolCallSpec(
            domain = domain,
            method = "copyFile",
            arguments = listOf(
                ToolCallSpec.Arg("source", "String", null),
                ToolCallSpec.Arg("dest", "String", null)
            ),
            returnType = "String",
            description = "Copy a file from source to destination"
        )
        
        toolCallSpecs["moveFile"] = ToolCallSpec(
            domain = domain,
            method = "moveFile",
            arguments = listOf(
                ToolCallSpec.Arg("source", "String", null),
                ToolCallSpec.Arg("dest", "String", null)
            ),
            returnType = "String",
            description = "Move a file from source to destination"
        )
        
        toolCallSpecs["listFiles"] = ToolCallSpec(
            domain = domain,
            method = "listFiles",
            arguments = emptyList(),
            returnType = "List<Map<String, Any>>",
            description = "List all files in the agent's file system"
        )
    }

    /**
     * Execute fs.* expressions against a FileSystem target using named args.
     */
    @Suppress("UNUSED_PARAMETER")
    @Throws(IllegalArgumentException::class)
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any {
        require(objectName == "fs") { "Object must be fs" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }
        require(target is AgentFileSystem) { "Target must be a FileSystem" }

        val fs = target

        return when (functionName) {
            // fs.writeString(filename: String, content: String)
            "writeString" -> {
                validateArgs(args, allowed = setOf("filename", "content"), required = setOf("filename"), functionName)
                fs.writeString(
                    paramString(args, "filename", functionName)!!,
                    paramString(args, "content", functionName, required = false, default = "") ?: ""
                )
            }
            // fs.readString(filename: String [, external: Boolean])
            "readString" -> {
                validateArgs(args, allowed = setOf("filename", "external"), required = setOf("filename"), functionName)
                fs.readString(
                    paramString(args, "filename", functionName)!!,
                    paramBool(args, "external", functionName, required = false, default = false) ?: false
                )
            }
            // fs.append(filename: String, content: String)
            "append" -> {
                validateArgs(args, allowed = setOf("filename", "content"), required = setOf("filename", "content"), functionName)
                fs.append(
                    paramString(args, "filename", functionName)!!,
                    paramString(args, "content", functionName)!!
                )
            }
            // fs.replaceContent(filename: String, oldStr: String, newStr: String)
            "replaceContent" -> {
                validateArgs(args, allowed = setOf("filename", "oldStr", "newStr"), required = setOf("filename", "oldStr", "newStr"), functionName)
                fs.replaceContent(
                    paramString(args, "filename", functionName)!!,
                    paramString(args, "oldStr", functionName)!!,
                    paramString(args, "newStr", functionName)!!
                )
            }
            // fs.fileExists(filename: String)
            "fileExists" -> {
                validateArgs(args, allowed = setOf("filename"), required = setOf("filename"), functionName)
                fs.fileExists(
                    paramString(args, "filename", functionName)!!
                )
            }
            // fs.getFileInfo(filename: String)
            "getFileInfo" -> {
                validateArgs(args, allowed = setOf("filename"), required = setOf("filename"), functionName)
                fs.getFileInfo(
                    paramString(args, "filename", functionName)!!
                )
            }
            // fs.deleteFile(filename: String)
            "deleteFile" -> {
                validateArgs(args, allowed = setOf("filename"), required = setOf("filename"), functionName)
                fs.deleteFile(
                    paramString(args, "filename", functionName)!!
                )
            }
            // fs.copyFile(source: String, dest: String)
            "copyFile" -> {
                validateArgs(args, allowed = setOf("source", "dest"), required = setOf("source", "dest"), functionName)
                fs.copyFile(
                    paramString(args, "source", functionName)!!,
                    paramString(args, "dest", functionName)!!
                )
            }
            // fs.moveFile(source: String, dest: String)
            "moveFile" -> {
                validateArgs(args, allowed = setOf("source", "dest"), required = setOf("source", "dest"), functionName)
                fs.moveFile(
                    paramString(args, "source", functionName)!!,
                    paramString(args, "dest", functionName)!!
                )
            }
            // fs.listFiles()
            "listFiles" -> {
                validateArgs(args, allowed = emptySet(), required = emptySet(), functionName)
                fs.listFilesInfo()
            }
            else -> throw IllegalArgumentException("Unsupported fs method: $functionName(${args.keys})")
        }
    }
}
