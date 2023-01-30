package com.maximintegrated.hsp

const val WHRM = "whrm"
const val SPO2 = "spo2"
const val HRV = "hrv"
const val RESP_RATE = "resp_rate"
const val STRESS = "stress"
const val SPORTS = "sports"
const val SLEEP = "sleep"
const val BPT = "bpt"
const val ECG = "ecg"
const val TEMP = "temp"
const val RED = "red"
const val IR = "ir"
const val GREEN = "green"
const val GREEN2 = "green2"
const val LOG_PARSER = "log_parser"

abstract class MaximDevice(val algorithms: ArrayList<String>) {
    open fun getPpgSensorsToDisplay(): List<String> {
        return listOf(GREEN, GREEN2)
    }
}

object ME11A : MaximDevice(arrayListOf(WHRM, SPO2, HRV, RESP_RATE, STRESS)) {
    override fun getPpgSensorsToDisplay(): List<String> {
        return listOf(IR, RED)
    }
}

object ME11B : MaximDevice(arrayListOf(WHRM, HRV, RESP_RATE, STRESS, SPORTS, SLEEP, ECG, TEMP, LOG_PARSER))
object ME11C : MaximDevice(arrayListOf(WHRM, SPO2, HRV, RESP_RATE, STRESS, SPORTS, SLEEP, LOG_PARSER))
object ME11D : MaximDevice(arrayListOf(BPT))
object ME15  : MaximDevice(arrayListOf(WHRM, SPO2, HRV, RESP_RATE, STRESS, SPORTS, SLEEP)) //MRD104
object UNDEFINED : MaximDevice(arrayListOf())