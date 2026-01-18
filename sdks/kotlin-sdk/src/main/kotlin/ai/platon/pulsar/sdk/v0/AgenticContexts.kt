package ai.platon.pulsar.sdk.v0

object AgenticContexts {
    fun getOrCreateSession(
        baseUrl: String? = null,
        useLocalDriver: Boolean = baseUrl == null
    ): AgenticSession {
        return AgenticSession.getOrCreate(baseUrl, useLocalDriver)
    }
}
