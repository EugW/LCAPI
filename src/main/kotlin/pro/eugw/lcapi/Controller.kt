package pro.eugw.lcapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
class Controller {

    @GetMapping("/schools")
    fun schools(@RequestParam(value = "lang", defaultValue = "en") lang: String): AvailableSchools {
        val arrayList = ArrayList<CommonSchool>()
        File("classes").listFiles().forEach {
                val file = File(it, "school-info.json")
                arrayList.add(CommonSchool(it.name, with(ObjectMapper().readTree(file)) {
                    if (this.has(lang))
                        this[lang].asText()
                    else
                        this["en"].asText()

                }))
        }
        return AvailableSchools(arrayList)
    }

    @GetMapping("/classes")
    fun classes(@RequestParam school_id: String, @RequestParam(value = "lang", defaultValue = "en") lang: String): AvailableClasses {
        val arrayList = ArrayList<CommonClass>()
        val file = File("classes", school_id)
        if (!file.exists())
            return AvailableClasses(arrayList, "")
        var schoolName = ""
        file.listFiles().forEach {
            if (it.name != "school-info.json") {
                val string = it.name.split(".")
                arrayList.add(CommonClass(string[0], string[1], string[2]))
            } else {
                schoolName = with(ObjectMapper().readTree(it)) {
                    if (this.has(lang))
                        this[lang].asText()
                    else
                        this["en"].asText()

                }
            }
        }
        return AvailableClasses(arrayList, schoolName)
    }

    @GetMapping("/class")
    fun bells(@RequestParam school_id: String, @RequestParam clazz: String): JsonNode {
        val file = File("classes", "$school_id/$clazz")
        println(file.absolutePath)
        if (!file.exists())
            return ObjectMapper().createObjectNode()
        return ObjectMapper().readTree(file)
    }

}