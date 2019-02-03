package pro.eugw.lcapi

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

var props = Properties()
var androidDB = JsonArray()
var vkDB = JsonArray()
var queue = RequestQueue()
var dispatcher = VkCommandDispatcher()
var secret = ""
var groupToken = ""
var monitorThreads = ArrayList<MonitorThread>()

fun main() {
    initProperties()
    initDB()
    initVkSecrets()
    initFCM()
    initMonitor()
    embeddedServer(Netty, props.getProperty("server.port", "46479").toInt()) {
        routing {
            get("/") {
                call.respondText("LCAPI Home page\nhttps://vk.com/brin_apps\nhttps://play.google.com/store/apps/details?id=pro.eugw.lessoncountdown\nv1.0", ContentType.Text.Any)
            }
            get("/classes") {
                val array = JsonArray()
                val dirs = File("classes")
                val lang = if (call.parameters.contains("lang")) call.parameters["lang"] else "en"
                dirs.listFiles().forEach { file ->
                    val schoolId = file.name
                    val schoolName = with(JsonParser().parse(File(File("info", schoolId), "school-info.json").readText()).asJsonObject) {
                        if (this.has(lang))
                            this[lang].asString
                        else
                            this["en"].asString
                    }
                    file.listFiles().forEach {fl ->
                        val string = fl.name.split(".")
                        val obj = JsonObject()
                        obj.addProperty("number", string[0])
                        obj.addProperty("letter", string[1])
                        obj.addProperty("subgroup", string[2])
                        obj.addProperty("school_id", schoolId)
                        obj.addProperty("school_name", schoolName)
                        array.add(obj)
                    }
                }
                val res = JsonObject()
                res.add("classes", array)
                call.respondText(res.toString())
            }
            get("/clazz") {
                if (!call.parameters.contains("school_id")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "specify school_id")
                    call.respond(HttpStatusCode.BadRequest, res.toString())
                    return@get
                }
                if (!call.parameters.contains("clazz")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "specify clazz")
                    call.respond(HttpStatusCode.BadRequest, res.toString())
                    return@get
                }
                val schoolId = call.parameters["school_id"]
                val clazz = call.parameters["clazz"]
                val schedule = File("classes", "$schoolId/$clazz.json")
                val bells = File(File("info", schoolId), "bells.json")
                if (!schedule.exists() || !bells.exists()) {
                    val res = JsonObject()
                    res.addProperty("schedule", "{}")
                    res.addProperty("bells", "{}")
                    call.respondText(res.toString())
                    return@get
                }
                val res = JsonObject()
                res.add("schedule", JsonParser().parse(schedule.readText()))
                res.add("bells", JsonParser().parse(bells.readText()))
                call.respondText(res.toString())
            }
            get("/register") {
                if (!call.parameters.contains("username")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "specify username")
                    call.respond(HttpStatusCode.BadRequest, res.toString())
                    return@get
                }
                if (!call.parameters.contains("password")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "specify password")
                    call.respond(HttpStatusCode.BadRequest, res.toString())
                    return@get
                }
                if (!call.parameters.contains("kusername")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "specify kundelik username")
                    call.respond(HttpStatusCode.BadRequest, res.toString())
                    return@get
                }
                if (!call.parameters.contains("kpassword")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "specify kundelik password")
                    call.respond(HttpStatusCode.BadRequest, res.toString())
                    return@get
                }
                if (!call.parameters.contains("fcmtoken")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "specify fcmtoken")
                    call.respond(HttpStatusCode.BadRequest, res.toString())
                    return@get
                }
                val username = call.parameters["username"]
                val password = call.parameters["password"]
                val kusername = call.parameters["kusername"]
                val kpassword = call.parameters["kpassword"]
                val fcmtoken = call.parameters["fcmtoken"]
                var accExists = false
                androidDB.forEach { joe ->
                    val obj = joe.asJsonObject
                    if (obj["username"].asString == username) {
                        accExists = true
                        if (obj["password"].asString == password) {
                            val res = JsonObject()
                            res.addProperty("token", obj["token"].asString)
                            call.respondText(res.toString())
                            return@get
                        } else {
                            val res = JsonObject()
                            res.addProperty("error_msg", "wrong password")
                            call.respond(HttpStatusCode.BadRequest, res.toString())
                            return@get
                        }
                    }
                }
                if (!accExists) {
                    val reg = JsonObject()
                    reg.addProperty("username", username)
                    reg.addProperty("password", password)
                    reg.addProperty("kusername", kusername)
                    reg.addProperty("kpassword", kpassword)
                    reg.addProperty("fcmtoken", fcmtoken)
                    val token = UUID.randomUUID().toString().replace("-", "")
                    reg.addProperty("token", token)
                    androidDB.add(reg)
                    val res = JsonObject()
                    res.addProperty("token", token)
                    call.respondText(res.toString())
                    thread(true) { PrintWriter(FileWriter("androidDB.json"), true).println(androidDB) }
                }
            }
            get("/test") {
                if (!call.parameters.contains("admin")) {
                    call.respond(HttpStatusCode.BadRequest, "u r not admin gtfo")
                    return@get
                }
                if (call.parameters["admin"] != "yeah") {
                    call.respond(HttpStatusCode.BadRequest, "u r not admin gtfo")
                    return@get
                }
                androidDB.forEach { joe ->
                    val obj = joe.asJsonObject
                    val registrationToken = obj["fcmtoken"].asString
                    val message = Message.builder()
                            .putData("subject", "Test subject")
                            .putData("mark", "Test mark")
                            .putData("date", SimpleDateFormat("yyyy-MM-dd").format(Date()))
                            .setToken(registrationToken)
                            .build()
                    FirebaseMessaging.getInstance().send(message)
                }
                call.respondText("Test sent")
            }
            post("/vkCallback") {
                val parsed = JsonParser().parse(call.receiveText()).asJsonObject
                if (!parsed.has("secret")) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "wrong secret")
                    call.respond(HttpStatusCode.Forbidden, res)
                    return@post
                }
                if (parsed["secret"].asString != secret) {
                    val res = JsonObject()
                    res.addProperty("error_msg", "wrong secret")
                    call.respond(HttpStatusCode.Forbidden, res)
                    return@post
                }
                call.respondText("ok")
                if (parsed["type"].asString == "message_new") {
                    val obj = parsed["object"].asJsonObject
                    if (obj["text"].asString.startsWith("/"))
                        dispatcher.dispatchCommand(obj["text"].asString, obj["from_id"].asString)
                }
            }
        }
    }.start(wait = true)
}

