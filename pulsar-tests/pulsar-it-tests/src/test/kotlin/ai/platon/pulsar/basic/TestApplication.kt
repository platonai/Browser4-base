package ai.platon.pulsar.basic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:test-beans/app-context.xml")
class TestApplication
