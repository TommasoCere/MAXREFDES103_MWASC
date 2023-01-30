package com.maximintegrated.maximsensorsapp.sports_coaching

import android.content.Context
import com.google.gson.Gson
import com.maximintegrated.algorithms.sports.SportsCoachingAlgorithmOutput
import com.maximintegrated.algorithms.sports.SportsCoachingHistory
import com.maximintegrated.algorithms.sports.SportsCoachingHistoryItem
import com.maximintegrated.algorithms.sports.SportsCoachingSession
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.ioThread
import java.io.File

fun getSportsCoachingOutputFile(username: String, timestamp: String, type: String) = File(
    getDirectoryReference(SPORTS_COACHING_DIRECTORY_NAME),
    "${File.separator}$username${File.separator}${BASE_FILE_NAME_PREFIX}${timestamp}_${type}${OUT_SUFFIX}.json"
)

fun saveMeasurement(
    username: String,
    output: SportsCoachingAlgorithmOutput,
    timestamp: String,
    type: String
) {
    val file = getSportsCoachingOutputFile(username, timestamp, type)
    val json = Gson().toJson(output)
    ioThread {
        file.parentFile?.mkdirs()
        file.printWriter().use { out ->
            out.write(json)
        }
    }
}

fun getHistoryFromFiles(username: String): SportsCoachingHistory {
    val outputs = getSportsCoachingOutputsFromFiles(username)
    if (outputs.isEmpty()) {
        return SportsCoachingHistory(0)
    }

    val historyList =
        outputs.filter { it.session != SportsCoachingSession.VO2MAX_FROM_HISTORY }.map {
            SportsCoachingHistoryItem(
                it.timestamp,
                it.estimates,
                it.hrStats,
                it.session
            )
        }

    return SportsCoachingHistory(historyList as ArrayList<SportsCoachingHistoryItem>)
}

fun getSportsCoachingOutputsFromFiles(username: String): ArrayList<SportsCoachingAlgorithmOutput> {
    val outputs: ArrayList<SportsCoachingAlgorithmOutput> = arrayListOf()
    val inputDirectory =
        File(getDirectoryReference(SPORTS_COACHING_DIRECTORY_NAME), "${File.separator}$username")
    if (!inputDirectory.exists()) {
        return outputs
    }
    val directory = File(inputDirectory.absolutePath)
    val files = directory.listFiles().toList().sortedWith(Comparator<File> { file1, file2 ->
        when {
            file1.lastModified() > file2.lastModified() -> -1
            file1.lastModified() < file2.lastModified() -> 1
            else -> 0
        }
    }).toMutableList()
    val gson = Gson()
    for (file in files) {
        val json = file.readText()
        val output = gson.fromJson<SportsCoachingAlgorithmOutput>(
            json,
            SportsCoachingAlgorithmOutput::class.java
        )
        if (output != null) {
            outputs.add(output)
        }
    }
    return outputs
}

fun getStringValueOfSession(context: Context, session: SportsCoachingSession): String {
    return when (session) {
        SportsCoachingSession.VO2MAX_RELAX -> context.getString(R.string.vo2max)
        SportsCoachingSession.UNDEFINED -> context.getString(R.string.undefined)
        SportsCoachingSession.VO2 -> context.getString(R.string.undefined)
        SportsCoachingSession.RECOVERY_TIME -> context.getString(R.string.recovery_time)
        SportsCoachingSession.READINESS -> context.getString(R.string.readiness)
        SportsCoachingSession.VO2MAX_FROM_HISTORY -> context.getString(R.string.fitness_age)
        SportsCoachingSession.EPOC_RECOVERY -> context.getString(R.string.epoc_recovery)
    }
}

fun getScore(output: SportsCoachingAlgorithmOutput): Number {
    return when (output.session ?: SportsCoachingSession.UNDEFINED) {
        SportsCoachingSession.VO2MAX_RELAX -> output.estimates.vo2max.relax
        SportsCoachingSession.UNDEFINED -> 0
        SportsCoachingSession.VO2 -> output.estimates.vo2max.vo2
        SportsCoachingSession.RECOVERY_TIME -> output.estimates.recovery.recoveryTimeMin
        SportsCoachingSession.READINESS -> output.estimates.readiness.readinessScore
        SportsCoachingSession.VO2MAX_FROM_HISTORY -> output.estimates.vo2max.fitnessAge
        SportsCoachingSession.EPOC_RECOVERY -> output.estimates.recovery.epoc
    }
}