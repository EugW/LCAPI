package pro.eugw.lcapi

data class AvailableClasses(val classes: ArrayList<CommonClass>, val school_name: String)
data class CommonClass(val number: String, val letter: String, val subgroup: String)
data class AvailableSchools(val schools: ArrayList<CommonSchool>)
data class CommonSchool(val id: String, val name: String)
data class Class(val schedule: String, val bells: String)