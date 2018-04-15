package pro.eugw.lcapi

import com.fasterxml.jackson.databind.JsonNode

data class AvailableClasses(val classes: ArrayList<CommonClass>)
data class CommonClass(val number: String, val letter: String, val subgroup: String, val school_id: String, val school_name: String)
data class Class(val schedule: JsonNode, val bells: JsonNode)