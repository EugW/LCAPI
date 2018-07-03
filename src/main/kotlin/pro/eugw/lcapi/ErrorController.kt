package pro.eugw.lcapi

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.io.PrintWriter
import java.io.StringWriter
import javax.servlet.http.HttpServletRequest


@Controller
class ErrorController : ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    fun handleError(request: HttpServletRequest): String {
        return if (request.getAttribute("javax.servlet.error.exception") != null) {
            val ex = request.getAttribute("javax.servlet.error.exception") as Exception
            val sw = StringWriter()
            ex.printStackTrace(PrintWriter(sw))
            "${ex.message} <br> $sw"
        } else {
            "Unmapped"
        }
    }

    override fun getErrorPath(): String {
        return "/error"
    }

}