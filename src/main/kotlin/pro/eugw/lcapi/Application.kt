package pro.eugw.lcapi

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext


@SpringBootApplication
class Application

var context: ApplicationContext? = null

fun main(args: Array<String>) {
    context = SpringApplication.run(Application::class.java)
    CommandResolver()
}


