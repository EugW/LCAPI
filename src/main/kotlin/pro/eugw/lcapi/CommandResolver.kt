package pro.eugw.lcapi

import org.springframework.boot.SpringApplication
import java.util.*

class CommandResolver {

    init {
        while (true) {
            resolve(Scanner(System.`in`).next())
        }
    }

    private fun resolve(command: String) {
        when (command) {
            "stop" -> {
                if (context != null) {
                    SpringApplication.exit(context)
                    System.exit(0)
                }
            }
            "start" -> {
                if (context != null)
                    SpringApplication.exit(context)
                context = SpringApplication.run(Application::class.java)
            }
        }

    }



}