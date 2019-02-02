package pro.eugw.lcapi

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.gson.JsonParser
import java.io.*
import java.util.*
import kotlin.concurrent.thread

fun initProperties() {
    props.load(FileReader("app.properties"))
}

fun initConsole() {
    thread(true) {
        val console = Scanner(System.`in`)
        while (true) {
            try {
                val str = console.next()
                println(str)
                when (str) {
                    "/testVk" -> {
                        val from = console.next()
                        println(from)
                        var sArg = ""
                        var t = ""
                        while (t != "") {
                            t = console.next()
                            sArg += t
                        }
                        println(sArg.replace("\n", ""))
                        dispatcher.dispatchCommand(sArg, from)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun initDB() {
    val androidDBFile = File("androidDB.json")
    if (!androidDBFile.exists())
        PrintWriter(FileWriter(androidDBFile), true).println("[]")
    androidDB = JsonParser().parse(androidDBFile.readText()).asJsonArray
    val vkDBFile = File("vkDB.json")
    if (!vkDBFile.exists())
        PrintWriter(FileWriter(vkDBFile), true).println("[]")
    vkDB = JsonParser().parse(vkDBFile.readText()).asJsonArray
}

fun initVkSecrets() {
    val parsed = JsonParser().parse(File("vk.json").readText()).asJsonObject
    secret = parsed["secret"].asString
    groupToken = parsed["groupToken"].asString
}

fun initFCM() {
    val options = FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(FileInputStream("lcsvc.json")))
            .setDatabaseUrl("https://lcnoti-ef773.firebaseio.com")
            .build()
    FirebaseApp.initializeApp(options)
}

fun initMonitor() {
    androidDB.forEach {
        monitorThreads.add(MonitorThread(it.asJsonObject["kusername"].asString, it.asJsonObject["kpassword"].asString, it.asJsonObject["fcmtoken"].asString, null))
    }
    vkDB.forEach {
        monitorThreads.add(MonitorThread(it.asJsonObject["kusername"].asString, it.asJsonObject["kpassword"].asString, null, it.asJsonObject["id"].asString))
    }
}
