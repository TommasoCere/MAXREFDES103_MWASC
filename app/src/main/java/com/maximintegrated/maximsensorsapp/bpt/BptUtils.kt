package com.maximintegrated.maximsensorsapp.bpt

import com.maximintegrated.maximsensorsapp.BPT_DIRECTORY_NAME
import com.maximintegrated.maximsensorsapp.BPT_HISTORY_FILE_NAME
import com.maximintegrated.maximsensorsapp.BPT_CALIBRATION_DATA_FILE_NAME
import com.maximintegrated.maximsensorsapp.getDirectoryReference
import com.maximintegrated.maximsensorsapp.toIntOrZero
import com.maximintegrated.maximsensorsapp.toLongOrZero
import de.siegmar.fastcsv.reader.CsvReader
import de.siegmar.fastcsv.reader.CsvRow
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets

val HISTORY_FILE: File
    get() = File(
        getDirectoryReference(BPT_DIRECTORY_NAME),
        "${File.separator}${BptSettings.currentUser}${File.separator}${BPT_HISTORY_FILE_NAME}.csv"
    )

val CALIBRATION_FILE: File
    get() = File(
        getDirectoryReference(BPT_DIRECTORY_NAME),
        "${File.separator}${BptSettings.currentUser}${File.separator}${BPT_CALIBRATION_DATA_FILE_NAME}.txt"
    )


const val NUMBER_OF_REFERENCES = 3
const val NUMBER_OF_FEATURES = 3
const val PPG_TEMPLATE_LENGTH = 50
const val MAX_NUMBER_OF_CALIBRATION = 5
const val SUGGESTED_NUMBER_OF_CALIBRATION = 3
const val NUMBER_OF_SAMPLES_FOR_CHART = 150

fun saveHistoryData(historyData: BptHistoryData) {
    if (!HISTORY_FILE.exists()) {
        HISTORY_FILE.parentFile?.mkdirs()
        HISTORY_FILE.createNewFile()
        HISTORY_FILE.appendText(BptHistoryData.CSV_HEADER_ARRAY.joinToString(",") + "\n")
    }
    HISTORY_FILE.appendText(historyData.toText())
}

fun readHistoryData(): List<BptHistoryData> {
    val list: ArrayList<BptHistoryData> = arrayListOf()
    if (!HISTORY_FILE.exists()) {
        return list
    }
    val reader = CsvReader()
    reader.setContainsHeader(true)
    try {
        val parser = reader.parse(HISTORY_FILE, StandardCharsets.UTF_8)
        var row: CsvRow? = parser.nextRow()
        while (row != null) {
            val timestamp = row.getField(0).toLongOrZero()
            val sbp = row.getField(1).toIntOrZero()
            val dbp = row.getField(2).toIntOrZero()
            val hr = row.getField(3).toIntOrZero()
            val spo2 = row.getField(4).toIntOrZero()
            val pulseFlag = row.getField(5).toIntOrZero()
            val isCalibration = row.getField(6) == "Calibration"
            val data = BptHistoryData(timestamp, isCalibration, sbp, dbp, hr, spo2, pulseFlag)
            list.add(data)
            row = parser.nextRow()
        }
    } catch (e: Exception) {
        Timber.d("Exception: $e")
    }
    return list
}

fun saveCalibrationData(calibrationInHexString: String, timestamp: Long, sbp: Int, dbp: Int) {
    if (!CALIBRATION_FILE.exists()) {
        CALIBRATION_FILE.parentFile?.mkdirs()
        CALIBRATION_FILE.createNewFile()
    }
    CALIBRATION_FILE.appendText("$calibrationInHexString $timestamp $sbp $dbp\n")
}

fun readCalibrationData(): List<BptCalibrationData> {
    val list: ArrayList<BptCalibrationData> = arrayListOf()

    if (!CALIBRATION_FILE.exists()) {
        return list
    }

    val lines = CALIBRATION_FILE.bufferedReader().readLines()
    if (lines.isEmpty()) {
        return arrayListOf()
    }

    return lines.map { BptCalibrationData.parseCalibrationDataFromString(it) }
}
