package pro.eugw.lcapi

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
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
        var usr = JsonObject()
        vkDB.forEach {
            if (it.asJsonObject["id"].asString == id) {
                usr = it.asJsonObject
            }
        }
        if (usr == JsonObject())
            return
        val jsonDetails = JSONObject()
        jsonDetails.put("username", usr["kusername"].asString)
        jsonDetails.put("password", usr["kpassword"].asString)
        jsonDetails.put("client_id", "387d44e3e0c94265a9e4a4caaad5111c")
        jsonDetails.put("client_secret", "8a7d709cfdbb4047b0ea8947afe89d67")
        jsonDetails.put("scope", "CommonInfo,ContactInfo,FriendsAndRelatives,EducationalInfo,SocialInfo,Files,Wall,Messages,Schools,Relatives,EduGroups,Lessons,Marks,EduWorks,Avatar")
        queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/authorizations/bycredentials").post(RequestBody.create(MediaType.parse("application/json"), jsonDetails.toString())).build(), object : RequestQueue.Listener {
            override fun onSuccess(response: Response) {
                val token = JsonParser().parse(response.body()!!.string()).asJsonObject["accessToken"].asString
                if (token.isEmpty())
                    return
                queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/users/me?access_token=$token").build(), object : RequestQueue.Listener {
                    override fun onSuccess(response: Response) {
                        val personId = JsonParser().parse(response.body()!!.string()).asJsonObject["personId"].asString
                        queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/users/me/schools?access_token=$token").build(), object : RequestQueue.Listener {
                            override fun onSuccess(response: Response) {
                                val schoolId = JsonParser().parse(response.body()!!.string()).asJsonArray[0].asString
                                val sdf = SimpleDateFormat("yyyy-MM-dd")
                                val calendar = Calendar.getInstance()
                                val to = sdf.format(calendar.time)
                                calendar.add(Calendar.MONTH, -1)
                                val from = sdf.format(calendar.time)
                                queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/persons/$personId/schools/$schoolId/marks/$from/$to?access_token=$token").build(), object : RequestQueue.Listener {
                                    override fun onSuccess(response: Response) {
                                        val arr = JsonParser().parse(response.body()!!.string()).asJsonArray.reversed().subList(0, count)
                                        var message = "Твои оценки:<br>"
                                        arr.forEach { jsonElement ->
                                            queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/lessons/${jsonElement.asJsonObject["lesson"].asString}?access_token=$token").build(), object : RequestQueue.Listener {
                                                override fun onSuccess(response: Response) {
                                                    val obj = JsonParser().parse(response.body()!!.string()).asJsonObject["subject"].asJsonObject["name"].asString
                                                    message += "$obj ${jsonElement.asJsonObject["value"].asString} выставлена ${jsonElement.asJsonObject["date"].asString.split("T")[0]}<br>"
                                                    if (arr.last() == jsonElement)
                                                        vkSendResponse(message, id)
                                                }
                                                override fun onFail(response: Response) {}
                                            })
                                        }
                                    }
                                    override fun onFail(response: Response) {}
                                })
                            }
                            override fun onFail(response: Response) {}
                        })
                    }
                    override fun onFail(response: Response) {}
                })
            }
            override fun onFail(response: Response) {}
        })

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