package main.java.parser

import bean.Course
import bean.TimeDetail
import bean.TimeTable

import parser.Parser

class ZZUParser (source: String) : Parser(source) {

    val timeTable = TimeTable(
        name = "郑大时间表", timeList = listOf(
            TimeDetail(1, "08:00", "08:45"),
            TimeDetail(2, "08:55", "09:40"),
            TimeDetail(3, "10:00", "10:45"),
            TimeDetail(4, "10:55", "11:40"),
            TimeDetail(5, "14:00", "14:45"),
            TimeDetail(6, "14:55", "15:40"),
            TimeDetail(7, "16:00", "16:45"),
            TimeDetail(8, "16:55", "17:40"),
            TimeDetail(9, "19:00", "19:45"),
            TimeDetail(10, "19:55", "20:40")
        )
    )

    override fun generateTimeTable(): TimeTable? {
        return timeTable
    }

    override fun generateCourseList(): List<Course> {
        val doc = org.jsoup.Jsoup.parse(source)
        val table1 = doc.getElementById("manualArrangeCourseTable")
        val trs = table1?.getElementsByTag("tr")
        val importBeanList = ArrayList<ImportBean>()
        var node: Int = -1
        if (trs != null) {
            for (tr in trs) {
                val tds = tr.getElementsByTag("td")
                var countFlag = false
                var countDay = 0
                for (td in tds) {
                    val courseSource = td.text().trim()
                    if (courseSource.length <= 1) {
                        if (countFlag) {
                            countDay++
                        }
                        continue
                    }
                    if (Common.otherHeader.contains(courseSource)) {
                        //other list
                        continue
                    }
                    val result = Common.parseHeaderNodeString(courseSource)
                    if (result != -1) {
                        node = result
                        countFlag = true
                        continue
                    }
                    countDay++
                    // if (node % 2 != 0)
                    //     println(td.html())
                    if (node % 2 != 0)
                        importBeanList.addAll(parseImportBean(countDay,td.html(), node))
                }
            }
        } else {
            throw Exception()
        }
        // return ArrayList<Course>()
        return importList2CourseList(importBeanList, source)
    }

    private fun parseImportBean(cDay: Int, html: String, node: Int): ArrayList<ImportBean> {
        val courses = ArrayList<ImportBean>()
        val courseSplits = html.split(Regex("<br>+"))
            .filter { it.isNotEmpty() }
        for (i in 0 until courseSplits.size step 2) {
            var courseAndTeacher = courseSplits[i];
            var timeAndRoom = courseSplits[i + 1];
            try {
                val tmp = ImportBean (
                    startNode = node,
                    name = courseAndTeacher.substring(0, courseAndTeacher.indexOf("(")),
                    timeInfo = timeAndRoom.substring(timeAndRoom.indexOf("(") + 1, timeAndRoom.indexOf(",")),
                    room = timeAndRoom.substring(timeAndRoom.indexOf(",") + 1, if (timeAndRoom.get(timeAndRoom.length - 2) == ')' || timeAndRoom.lastIndexOf("(") == 0) timeAndRoom.length - 1 else timeAndRoom.length),
                    teacher = courseAndTeacher.substring(courseAndTeacher.lastIndexOf("(") + 1, courseAndTeacher.lastIndexOf(")")),
                    coureID = courseAndTeacher.substring(courseAndTeacher.indexOf("(") + 1, courseAndTeacher.indexOf(")")),
                    cDay = cDay
                )
                courses.add(tmp)
            } catch (e: Exception) {
                println(e)
            }
        }
        return courses
    }

    private fun importList2CourseList(importList: ArrayList<ImportBean>, source: String): List<Course> {
        val result = arrayListOf<Course>()
        for (i in importList) {
            val time = mutableListOf<Array<Int>>()
            val timeInfos = i.timeInfo.split(" ")
            for (timeInfo in timeInfos) {
                time.add(parseTime(timeInfo))
            }
            for (arr in time) {
                result.add(
                    Course(
                        name = i.name, day = i.cDay, room = i.room ?: "",
                        teacher = i.teacher ?: "", startNode = i.startNode,
                        endNode = i.startNode + 1,
                        type = arr[2],
                        startWeek = arr[0],
                        endWeek = arr[1],
                        courseID = i.coureID
                    )
                )
            }
        }
        return result
    }

    private fun parseTime(timeInfo: String): Array<Int> {
        //按顺序分别为startWeek, endWeek, type
        val result = Array(3) { 0 }
        //单双周
        var startPos = 0
        if (timeInfo[0] == '单') {
            result[2] = 1
            startPos = 1
        }
        else if(timeInfo[0] == '双') {
            result[1] = 2
            startPos = 2
        }
        //起止周
        val mid = timeInfo.indexOf("-")
        if (mid == -1) {
            result[0] = Integer.parseInt(timeInfo)
            result[1] = result[0]
        } else {
            result[0] = Integer.parseInt(timeInfo.substring(startPos, mid))
            result[1] = Integer.parseInt(timeInfo.substring(mid + 1, timeInfo.length))
        }
        return result
    }

}

data class ImportBean(
    var name: String,
    var timeInfo: String,
    var teacher: String?,
    var room: String?,
    var startNode: Int,
    var cDay: Int = 0,
    var coureID: String
)
