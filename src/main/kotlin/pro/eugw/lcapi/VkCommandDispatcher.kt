package pro.eugw.lcapi

import com.google.gson.JsonObject
import okhttp3.Request
import okhttp3.Response
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*
import kotlin.random.Random

class VkCommandDispatcher {

    fun dispatchCommand(command: String, from: String) {
        val spl = command.split(" ")
        val cmd = spl[0]
        val args = spl.subList(1, spl.size)
        val argMap = HashMap<String, String>()
        args.forEach {
            if (it.contains("=")) {
                val base = it.split("=")
                argMap[base[0]] = base[1]
            }
        }
        when (cmd) {
            "/register" -> {
                registerVk(from)
                if (argMap.containsKey("username") && argMap.containsKey("password"))
                    updateCredentialsVk(from, argMap["username"]!!, argMap["password"]!!)
            }
            "/unregister" -> {
                unregisterVk(from)
            }
            "/marks" -> {
                if (argMap.containsKey("count"))
                    marksVk(from, argMap["count"]!!.toInt())
            }
        }
    }

    private fun marksVk(id: String, count: Int) {
        println("kek")

    }

    private fun registerVk(id: String) {
        var accExists = false
        vkDB.forEach {
            val obj = it.asJsonObject
            if (obj["id"].asString == id) {
                accExists = true
                vkSendResponse("Вы уже зарегистрированы", id)
            }
        }
        if (!accExists) {
            val reg = JsonObject()
            reg.addProperty("id", id)
            vkDB.add(reg)
            PrintWriter(FileWriter("vkDB.json"), true).println(vkDB)
            vkSendResponse("Успешная регистрация", id)
        }
    }

    private fun updateCredentialsVk(id: String, username: String, password: String) {
        var updated = false
        vkDB.forEach {
            val obj = it.asJsonObject
            if (obj["id"].asString == id) {
                if (obj.has("kusername") && obj.has("kpassword")) {
                    if (obj["kusername"].asString != username)
                        updated = true
                    if (obj["kpassword"].asString != password)
                        updated = true
                } else
                    updated = true
                obj.addProperty("kusername", username)
                obj.addProperty("kpassword", password)
            }
        }
        PrintWriter(FileWriter("vkDB.json"), true).println(vkDB)
        if (updated)
            vkSendResponse("Данные обновлены", id)
    }

    private fun unregisterVk(id: String) {
        var removed = false
        val q = vkDB.find { it.asJsonObject["id"].asString == id }
        if (q != null) {
            vkDB.remove(q)
            removed = true
        }
        PrintWriter(FileWriter("vkDB.json"), true).println(vkDB)
        if (removed)
            vkSendResponse("Ваши данные удалены", id)
    }

    private fun vkSendResponse(message: String, id: String) {
        queue.addRequest(Request.Builder().url("https://api.vk.com/method/messages.send?user_id=$id&random_id=${Random.nextInt()}&message=$message&access_token=$groupToken&v=5.92").build(), object : RequestQueue.Listener {
            override fun onSuccess(response: Response) {}
            override fun onFail(response: Response) {}
        })
    }

}