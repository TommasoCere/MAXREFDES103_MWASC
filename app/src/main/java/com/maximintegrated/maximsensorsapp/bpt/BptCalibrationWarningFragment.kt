package com.maximintegrated.maximsensorsapp.bpt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.addFragment
import com.maximintegrated.maximsensorsapp.exts.replaceFragment
import com.maximintegrated.maximsensorsapp.getFormattedTime
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_bpt_calibration_warning_fragment_content.*

class BptCalibrationWarningFragment : Fragment() {
    companion object {
        fun newInstance(): BptCalibrationWarningFragment {
            return BptCalibrationWarningFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_bpt_calibration_warning, container, false)

    private lateinit var hspViewModel: HspViewModel

    private lateinit var bptViewModel: BptViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        toolbar.pageTitle = getString(R.string.bp_trending)

        restartTimerButton.setOnClickListener {
            restartTimer()
        }

        goToCalibrationButton.setOnClickListener {
            bptViewModel.stopTimer()
            requireActivity().replaceFragment(BptCalibrationFragment.newInstance())
        }

        bptViewModel.elapsedTime.observe(this){
            calibrationCircleProgressView.setValue(it / 1000f)
            calibrationCircleProgressView.setText(getFormattedTime(it))
        }

        bptViewModel.startTimer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    private fun restartTimer() {
        bptViewModel.restartTimer()
    }
}