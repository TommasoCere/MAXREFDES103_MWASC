package com.maximintegrated.maximsensorsapp.bpt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.hsp.HspBptStreamData
import com.maximintegrated.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.bpt.BptStatus.*
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_bpt_calibration_fragment_content.*
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.*
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.bpWarningMessageTextView
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.bpWarningSkipButton
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.bpWarningStopButton
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.bpWarningTitleTextView
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.chartView
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.hrTextView
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.progressBar
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.progressTextView
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.signalTextView
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.spo2TextView
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.warningLayout
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import java.io.File
import java.util.*
import kotlin.math.min

class BptMeasurementFragment : Fragment(), IOnBackPressed {
    companion object {
        fun newInstance(): BptMeasurementFragment {
            return BptMeasurementFragment()
        }

        const val NUMBER_OF_DATA_TO_DISCARD = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_bpt_measurement, container, false)

    private lateinit var hspViewModel: HspViewModel
    private lateinit var bptViewModel: BptViewModel

    lateinit var menuItemStartMonitoring: MenuItem
    lateinit var menuItemStopMonitoring: MenuItem

    private var csvWriter: CsvWriter? = null
    private val irMovingAverage = MovingAverage(3)
    private lateinit var statusArray: Array<String>

    private var bptSuccess = false

    private var initialWarningIsShown = false
        set(value) {
            field = value
            pleaseWaitTextView.isVisible = value
        }

    private var showWarning = false
        set(value) {
            field = value
            chartView.isInvisible = value
            warningLayout.isVisible = value
        }

    private var dataCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusArray = resources.getStringArray(R.array.bpt_algorithm_out_status)

        hspViewModel = ViewModelProviders.of(requireActivity()).get(HspViewModel::class.java)

        bptViewModel = ViewModelProviders.of(requireActivity()).get(BptViewModel::class.java)

        hspViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                toolbar.connectionInfo = if (hspViewModel.bluetoothDevice != null) {
                    BleConnectionInfo(connectionState, device?.name, device?.address)
                } else {
                    null
                }
            }

        bptViewModel.stopTimer()

        toolbar.apply {
            inflateMenu(R.menu.bpt_menu)
            menu.apply {
                menuItemStartMonitoring = findItem(R.id.monitoring_start)
                menuItemStopMonitoring = findItem(R.id.monitoring_stop)
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.monitoring_start -> startMonitoring()
                    R.id.monitoring_stop -> showStopMonitoringDialog()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            title = getString(R.string.bp_trending)
            pageTitle = getString(R.string.bp_trending)
        }

        bptViewModel.isMonitoring.observe(this) {
            menuItemStopMonitoring.isVisible = it
            menuItemStartMonitoring.isVisible = !it
        }

        bptViewModel.elapsedTime.observe(this) {
            if (bptViewModel.startTime == "") {
                toolbar.subtitle = getFormattedTime(it)
            } else {
                toolbar.subtitle = "Start Time: ${bptViewModel.startTime} - ${getFormattedTime(it)}"
            }
        }

        setupChart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onDetach() {
        super.onDetach()
        hspViewModel.bptStreamData.removeObserver(dataStreamObserver)
    }

    private fun setupChart() {
        chartView.titleView.isVisible = false
        chartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.ppg_signal, R.color.channel_red)
        )
        chartView.maximumEntryCount = NUMBER_OF_SAMPLES_FOR_CHART
        chartView.line_chart_view.axisLeft.isEnabled = false
    }

    private fun initCsvWriter() {
        val timestamp = HspBptStreamData.TIMESTAMP_FORMAT.format(Date())
        val file = File(
            getDirectoryReference(BPT_DIRECTORY_NAME),
            "${File.separator}${BptSettings.currentUser}${File.separator}${BASE_BPT_FILE_NAME_PREFIX}${timestamp}${BPT_ESTIMATION_SUFFIX}.csv"
        )
        csvWriter = CsvWriter.open(file.absolutePath, HspBptStreamData.CSV_HEADER_ARRAY)
    }

    private val dataStreamObserver = Observer<HspBptStreamData> { data ->
        if (bptViewModel.isMonitoring.value == false) return@Observer
        addStreamData(data)
    }

    private fun startMonitoring() {
        if (bptViewModel.isMonitoring.value!!) return
        bptSuccess = false
        val calResult = bptViewModel.getValidCalibrationList()
        if (calResult.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.calibration_required_warning,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        chartView.clearChart()
        bptViewModel.startMeasurement()
        initCsvWriter()
        hspViewModel.streamType = HspViewModel.StreamType.BPT
        hspViewModel.bptStreamData.observeForever(dataStreamObserver)
        hspViewModel.setBptDateTime()
        hspViewModel.setSpO2Coefficients(
            bptViewModel.spO2Coefficients[0],
            bptViewModel.spO2Coefficients[1],
            bptViewModel.spO2Coefficients[2]
        )
        for ((index, calibration) in calResult.withIndex()) {
            hspViewModel.setCalibrationIndex(index)
            hspViewModel.setCalibrationResult(calibration.hexString)
        }
        hspViewModel.startBptEstimationStreaming()
        signalTextView.text = ""
        sbpTextView.text = getString(R.string.empty_value)
        dbpTextView.text = getString(R.string.empty_value)
        showWarning = false
        initialWarningIsShown = true
        chartView.isInvisible = true
        prevStatus = null
    }

    private fun stopMonitoring() {
        initialWarningIsShown = false
        if (!bptViewModel.isMonitoring.value!!) return
        hspViewModel.stopStreaming()
        bptViewModel.stopMeasurement()
        hspViewModel.bptStreamData.removeObserver(dataStreamObserver)
        csvWriter?.close()
        dataCount = 0
        csvWriter = null
    }

    private fun addStreamData(data: HspBptStreamData) {
        if(initialWarningIsShown){
            initialWarningIsShown = false
            chartView.isVisible = true
        }
        dataCount++
        if (dataCount <= NUMBER_OF_DATA_TO_DISCARD) {
            return
        }
        csvWriter?.write(data.toCsvModel())
        irMovingAverage.add(data.irCnt)
        chartView.addData(irMovingAverage.average())
        progressBar.progress = data.progress
        progressTextView.text = getString(R.string.percent_format, data.progress.toString())
        hrTextView.text = data.hr.toString()
        spo2TextView.text = data.spo2.toString()
        signalTextView.text = statusArray[data.status]
        onStatusChanged(data)
    }

    override fun onBackPressed(): Boolean {
        return bptViewModel.isMonitoring.value ?: false
    }

    override fun onStopMonitoring() {
        stopMonitoring()
    }

    private fun showStopMonitoringDialog() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Stop Monitoring")
        alertDialog.setMessage("Are you sure you want to stop monitoring ?")
            .setPositiveButton("OK") { dialog, which ->
                stopMonitoring()
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private var prevStatus: BptStatus? = null

    private fun onStatusChanged(data: HspBptStreamData) {
        val flag = min(RESERVED.ordinal, data.status)
        val status = BptStatus.fromInt(flag)
        if (status == prevStatus) {
            return
        } else if (prevStatus == NO_CONTACT_ERROR) {
            showWarning = false
        }
        prevStatus = status
        when (status) {
            SUCCESS -> {
                showWarning = false
                signalTextView.text = statusArray[data.status]
                if (!bptSuccess) {
                    sbpTextView.text = data.sbp.toString()
                    dbpTextView.text = data.dbp.toString()
                    saveHistoryData(data.toHistoryModel(false))
                    bptSuccess = true
                }
            }
            FAILURE -> {
                signalTextView.text = statusArray[data.status]
                stopMonitoring()
            }
            INIT_SUBJECT_FAILURE -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.subject_failure_error),
                    getString(R.string.subject_failure_error_message),
                    true
                )
            }
            INIT_CAL_REF_BP_TRENDING_ERROR -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.trending_error),
                    getString(R.string.trending_error_message),
                    true
                )
            }
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_1 -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.inconsistency_error),
                    getString(R.string.inconsistency_error_1),
                    true
                )
            }
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_2 -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.inconsistency_error),
                    getString(R.string.inconsistency_error_2),
                    true
                )
            }
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_3 -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.inconsistency_error),
                    getString(R.string.inconsistency_error_3),
                    true
                )
            }
            INIT_CAL_REF_CNT_MISMATCH -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.ref_count_mismatch_error),
                    getString(R.string.ref_count_mismatch_error_message),
                    true
                )
            }
            INIT_CAL_REF_OUT_OF_LIMIT -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.ref_out_of_limit_error),
                    getString(R.string.ref_out_of_limit_error_message),
                    true
                )
            }
            INIT_CAL_REF_MAXNUM_ERROR -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.ref_max_num_error),
                    getString(R.string.ref_max_num_error_message),
                    true
                )
            }
            PP_OUT_OF_RANGE_ERROR -> {
                signalTextView.text = statusArray[ESTIMATION_ERROR.ordinal]
                showWarningMessage(
                    getString(R.string.estimation_error),
                    getString(R.string.estimation_error_message),
                    true
                )
            }
            HR_OUT_OF_RANGE -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.hr_out_of_range_error),
                    getString(R.string.hr_out_of_range_error_message),
                    true
                )
            }
            HR_ABOVE_RESTING -> {
                if(!bptSuccess){
                    signalTextView.text = statusArray[data.status]
                    showWarningMessage(
                        getString(R.string.hr_above_resting_warning),
                        getString(R.string.hr_above_resting_warning_message),
                        false
                    )
                }
            }
            PI_OUT_OF_RANGE -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.pi_out_of_range_error),
                    getString(R.string.pi_out_of_range_error_message),
                    true
                )
            }
            ESTIMATION_ERROR -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.estimation_error),
                    getString(R.string.estimation_error_message),
                    true
                )
            }
            OUT_OF_RANGE_ERROR -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.bp_out_of_range_error),
                    getString(R.string.bp_out_of_range_error_message),
                    true
                )
            }
            OUT_OF_LIMIT_ERROR -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.bp_out_of_limit_error),
                    getString(R.string.bp_out_of_limit_error_message),
                    true
                )
            }
            NO_CONTACT_ERROR -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.no_contact_error),
                    getString(R.string.no_contact_error_message),
                    false
                )
            }
            NO_FINGER_ERROR -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    getString(R.string.no_finger_error),
                    getString(R.string.no_finger_error_message),
                    true
                )
            }
            RESERVED -> {
                signalTextView.text = statusArray[data.status]
                showWarningMessage(
                    "Unknown Error",
                    "Unknown Message",
                    true
                )
            }
            PROGRESS -> {
                if(!bptSuccess){
                    signalTextView.text = statusArray[data.status]
                }
            }
            CAL_SEGMENT_DONE -> {

            }
            else -> {
                signalTextView.text = statusArray[data.status]
            }
        }
    }

    private fun showWarningMessage(title: String, message: String, abort: Boolean) {
        bpWarningTitleTextView.text = title
        bpWarningMessageTextView.text = message
        bpWarningSkipButton.isVisible = !abort
        showWarning = true
        if (abort) {
            stopMonitoring()
            bpWarningSkipButton.text = getString(R.string.restart)
            bpWarningSkipButton.isVisible = true
            bpWarningStopButton.isVisible = false
            bpWarningSkipButton.setOnClickListener {
                startMonitoring()
            }
        } else {
            bpWarningSkipButton.text = getString(R.string.skip_now)
            bpWarningStopButton.text = getString(R.string.stop_measurement)
            bpWarningSkipButton.isVisible = true
            bpWarningStopButton.isVisible = true
            bpWarningStopButton.setOnClickListener {
                stopMonitoring()
                showWarning = false
            }
            bpWarningSkipButton.setOnClickListener {
                showWarning = false
            }
        }
    }
}