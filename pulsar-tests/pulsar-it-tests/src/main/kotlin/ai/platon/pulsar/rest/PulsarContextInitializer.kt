package ai.platon.pulsar.rest

import ai.platon.pulsar.core.api.PulsarContexts
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.AbstractApplicationContext

class PulsarContextInitializer : ApplicationContextInitializer<AbstractApplicationContext> {
    override fun initialize(applicationContext: AbstractApplicationContext) {
        PulsarContexts.create(applicationContext)
    }
}
