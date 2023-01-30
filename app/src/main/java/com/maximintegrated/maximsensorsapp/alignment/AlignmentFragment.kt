package com.maximintegrated.maximsensorsapp.alignment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.maximintegrated.maximsensorsapp.DataRecorder
import com.maximintegrated.maximsensorsapp.MWA_OUTPUT_DIRECTORY
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.align
import com.obsez.android.lib.filechooser.ChooserDialog
import kotlinx.android.synthetic.main.fragment_alignment.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

class AlignmentFragment : Fragment() {

    companion object {
        fun newInstance() = AlignmentFragment()
    }

    private var rawFile: File? = null
        set(value) {
            field = value
            rawFileTextView.text = rawFile?.path ?: ""
            savedFile = null
        }

    private var refFile: File? = null
        set(value) {
            field = value
            refFileTextView.text = refFile?.path ?: ""
            savedFile = null
        }

    private var savedFile: File? = null
        set(value) {
            field = value
            savedTextView.text = value?.path ?: ""
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alignment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rawFileImageView.setOnClickListener {
            showFilesToImportRawFile()
        }
        refFileImageView.setOnClickListener {
            showFilesToImportRefFile()
        }
        refFileInfoImageView.setOnClickListener {
            showRefFileDialog()
        }
        alignButton.setOnClickListener {
            savedFile = null
            warningMessageView.visibility = View.GONE
            if (rawFile == null || refFile == null) return@setOnClickListener
            progressBar.visibility = View.VISIBLE
            doAsync {
                try {
                    val file = align(rawFile!!, refFile!!)
                    uiThread {
                        savedFile = file
                        warningMessageView.text = getString(R.string.alignment_successful)
                        warningMessageView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_check,
                            0,
                            0,
                            0
                        )
                        warningMessageView.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    uiThread {
                        warningMessageView.text = e.message
                        warningMessageView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_warning,
                            0,
                            0,
                            0
                        )
                        warningMessageView.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun showFilesToImportRawFile() {
        ChooserDialog(requireContext())
            .withStartFile(MWA_OUTPUT_DIRECTORY.absolutePath)
            .withChosenListener { _, dirFile ->
                run {
                    rawFile = dirFile
                }
            }
            .build()
            .show()
    }

    private fun showFilesToImportRefFile() {
        ChooserDialog(requireContext())
            .withStartFile(MWA_OUTPUT_DIRECTORY.absolutePath)
            .withChosenListener { _, dirFile ->
                run {
                    refFile = dirFile
                }
            }
            .build()
            .show()
    }

    private fun showRefFileDialog() {
        val contentView = LayoutInflater.from(context).inflate(R.layout.dialog_ref_file_info, null)
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setView(contentView)
        alertDialog.show()
    }
}