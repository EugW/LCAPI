package pro.eugw.lcapi

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import java.security.MessageDigest


@SpringBootApplication
class Application

var context: ApplicationContext? = null
var passEnc: String? = null

fun main(args: Array<String>) {
    if (args.contains("--password") || args.contains("-p"))
        if (args.size > args.indexOf(args.find { it == "--password" || it == "-p" }) + 1)
            passEnc = MessageDigest.getInstance("SHA-256").digest(args[args.indexOf(args.find { it == "--password" || it == "-p" }) + 1].toByteArray()).fold("") { str, it -> str + "%02x".format(it) }
    context = SpringApplication.run(Application::class.java)
    CommandResolver()
}


