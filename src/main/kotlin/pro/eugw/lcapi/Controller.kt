package pro.eugw.lcapi

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
class Controller {

    @GetMapping("/classes")
    fun classes(@RequestParam(value = "lang", defaultValue = "en") lang: String): AvailableClasses {
        val arrayList = ArrayList<CommonClass>()
        val dirs = File("classes")
        dirs.listFiles().forEach {
            val schoolId = it.name
            val schoolName = with(ObjectMapper().readTree(File(File("info", schoolId), "school-info.json"))) {
                if (this.has(lang))
                    this[lang].asText()
                else
                    this["en"].asText()
            }
            it.listFiles().forEach {
                val string = it.name.split(".")
                arrayList.add(CommonClass(string[0], string[1], string[2], schoolId, schoolName))
            }
        }
        return AvailableClasses(arrayList)
    }

    @GetMapping("/class")
    fun clazz(@RequestParam school_id: String, @RequestParam clazz: String): Class {
        val schedule = File("classes", "$school_id/$clazz.json")
        val bells = File(File("info", school_id), "bells.json")
        if (!schedule.exists() || !bells.exists())
            return Class(ObjectMapper().createObjectNode(), ObjectMapper().createObjectNode())
        return Class(ObjectMapper().readTree(schedule), ObjectMapper().readTree(bells))
    }

}