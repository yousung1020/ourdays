package com.ourdays

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OurdaysBackendApplication

fun main(args: Array<String>) {
	runApplication<OurdaysBackendApplication>(*args)
}
