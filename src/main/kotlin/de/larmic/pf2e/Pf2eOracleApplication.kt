package de.larmic.pf2e

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class Pf2eOracleApplication

fun main(args: Array<String>) {
    runApplication<Pf2eOracleApplication>(*args)
}
