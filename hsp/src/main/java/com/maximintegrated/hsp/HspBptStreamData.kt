package com.maximintegrated.hsp

import java.text.SimpleDateFormat
import java.util.*

data class HspBptStreamData(
    val status: Int,
    val irCnt: Int,
    val redCnt: Int,
    val hr: Int,
    val progress: Int,
    var sbp: Int,
    var dbp: Int,
    val spo2: Int,
    val pulseFlag: Int,
    val r: Float,
    val ibi: Int,
    val spo2Confidence: Int,
    val bpReportFlag: Int,
    val spo2ReportFlag: Int,
    val endBpt: Int
) {

    companion object {
        val CSV_HEADER_ARRAY = arrayOf(
            "Timestamp", "irCnt", "redCnt", "status", "percentCompleted", "HR", "SpO2", "pulseFlag",
            "estimatedSBP", "estimatedDBP", "r", "ibi", "spo2Confidence", "bpReportFlag", "spo2ReportFlag",
            "endBpt"
        )

        val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.US)

        fun fromPacket(packet: ByteArray): HspBptStreamData {
            return with(BitStreamReader(packet, 8)) {
                HspBptStreamData(
                    status = nextInt(6),
                    irCnt = nextInt(19),
                    redCnt = nextInt(19),
                    hr = nextInt(9),
                    progress = nextInt(9),
                    sbp = nextInt(9),
                    dbp = nextInt(9),
                    spo2 = nextInt(8),
                    pulseFlag = nextInt(8),
                    r = nextFloat(16, 1000),
                    ibi = nextInt(16),
                    spo2Confidence = nextInt(8),
                    bpReportFlag = nextInt(1),
                    spo2ReportFlag = nextInt(1),
                    endBpt = nextInt(1)
                )
            }
        }
    }

    val timestamp = System.currentTimeMillis()

    fun toCsvModel(): String {
        return arrayOf(
            timestamp,
            irCnt,
            redCnt,
            status,
            progress,
            hr,
            spo2,
            pulseFlag,
            sbp,
            dbp,
            r,
            ibi,
            spo2Confidence,
            bpReportFlag,
            spo2ReportFlag,
            endBpt
        )
            .joinToString(separator = ",")
    }
}