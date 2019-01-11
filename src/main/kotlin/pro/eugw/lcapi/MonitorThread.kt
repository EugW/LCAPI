package pro.eugw.lcapi

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MonitorThread(kusername: String, kpassword: String, private val fcmtoken: String?, private val id: String?) : Thread() {

    private val dataDir = File("savedMarks")
    private var firstTime = false
    private lateinit var token: String
    private lateinit var personId: String
    private lateinit var schoolId: String
    private var array = JsonArray()

    init {
        if (!dataDir.exists())
            dataDir.mkdir()
        val jsonDetails = JSONObject()
        jsonDetails.put("username", kusername)
        jsonDetails.put("password", kpassword)
        jsonDetails.put("client_id", "387d44e3e0c94265a9e4a4caaad5111c")
        jsonDetails.put("client_secret", "8a7d709cfdbb4047b0ea8947afe89d67")
        jsonDetails.put("scope", "CommonInfo,ContactInfo,FriendsAndRelatives,EducationalInfo,SocialInfo,Files,Wall,Messages,Schools,Relatives,EduGroups,Lessons,Marks,EduWorks,Avatar")
        queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/authorizations/bycredentials").post(RequestBody.create(MediaType.parse("application/json"), jsonDetails.toString())).build(), object : RequestQueue.Listener {
            override fun onSuccess(response: Response) {
                token = JsonParser().parse(response.body()!!.string()).asJsonObject["accessToken"].asString
                queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/users/me?access_token=$token").build(), object : RequestQueue.Listener {
                    override fun onSuccess(response: Response) {
                        personId = JsonParser().parse(response.body()!!.string()).asJsonObject["personId"].asString
                        queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/users/me/schools?access_token=$token").build(), object : RequestQueue.Listener {
                            override fun onSuccess(response: Response) {
                                schoolId = JsonParser().parse(response.body()!!.string()).asJsonArray[0].asString
                                if (!File(dataDir, "$personId.json").exists()) {
                                    firstTime = true
                                    PrintWriter(FileWriter(File(dataDir, "$personId.json")), true).println("[]")
                                }
                                array = JsonParser().parse(FileReader(File(dataDir, "$personId.json"))).asJsonArray
                                start()
                            }
                            override fun onFail(response: Response) {}
                        })
                    }
                    override fun onFail(response: Response) {}
                })
            }
            override fun onFail(response: Response) {
            }
        })
    }

    override fun run() {
        super.run()
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        while (true) {
            val calendar = Calendar.getInstance()
            val to = sdf.format(calendar.time)
            calendar.add(Calendar.MONTH, -1)
            val from = sdf.format(calendar.time)
            queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/persons/$personId/schools/$schoolId/marks/$from/$to?access_token=$token").build(), object : RequestQueue.Listener {
                override fun onSuccess(response: Response) {
                    val arr = JsonParser().parse(response.body()!!.string()).asJsonArray
                    if (firstTime) {
                        firstTime = false
                        array = arr
                        PrintWriter(FileWriter(File(dataDir, "$personId.json")), true).println(arr)
                        return
                    }
                    val newMarks = JsonArray()
                    arr.forEach {
                        if (!array.contains(it)) {
                            newMarks.add(it)
                            queue.addRequest(Request.Builder().url("https://api.kundelik.kz/v1/lessons/${it.asJsonObject["lesson"].asString}?access_token=$token").build(), object : RequestQueue.Listener {
                                override fun onSuccess(response: Response) {
                                    val obj = JsonParser().parse(response.body()!!.string()).asJsonObject["subject"].asJsonObject["name"].asString
                                    if (id != null)
                                        sendVkNotification(obj, it.asJsonObject["value"].asString, it.asJsonObject["date"].asString.split("T")[0])
                                    if (fcmtoken != null)
                                        sendAndroidNotification(obj, it.asJsonObject["value"].asString, it.asJsonObject["date"].asString.split("T")[0])
                                }

                                override fun onFail(response: Response) {}
                            })
                        }
                    }
                    array.addAll(newMarks)
                    PrintWriter(FileWriter(File(dataDir, "$personId.json")), true).println(array)
                }
                override fun onFail(response: Response) {}
            })
            sleep(600000)
        }
    }

    private fun sendAndroidNotification(subject: String, mark: String, date: String) {
        val message = Message.builder()
                .putData("subject", subject)
                .putData("mark", mark)
                .putData("date", date)
                .setToken(fcmtoken)
                .build()
        FirebaseMessaging.getInstance().send(message)
    }

    private fun sendVkNotification(subject: String, mark: String, date: String) {
        val message = "Новая оценка по предмету $subject $mark выставлена $date"
        queue.addRequest(Request.Builder().url("https://api.vk.com/method/messages.send?user_id=$id&random_id=${Random.nextInt()}&message=$message&access_token=$groupToken&v=5.92").build(), object : RequestQueue.Listener {
            override fun onSuccess(response: Response) {}
            override fun onFail(response: Response) {}
        })
    }

}