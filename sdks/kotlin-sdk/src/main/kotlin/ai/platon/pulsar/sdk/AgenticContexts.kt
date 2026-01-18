package ai.platon.pulsar.sdk

object AgenticContexts {
    suspend fun getOrCreateSession(
        baseUrl: String? = null,
        useLocalDriver: Boolean = baseUrl == null
    ): AgenticSession {
        return AgenticSession.getOrCreate(baseUrl, useLocalDriver)
    }
}
