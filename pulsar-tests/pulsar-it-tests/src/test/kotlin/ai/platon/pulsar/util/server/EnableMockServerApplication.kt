package ai.platon.pulsar.util.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ImportResource

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.pulsar.boot.autoconfigure",
        "ai.platon.pulsar.test.server"
    ]
)
@ImportResource("classpath:test-beans/app-context.xml")
class EnableMockServerApplication
