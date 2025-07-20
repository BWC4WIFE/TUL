package com.ui.dialog

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bwc.translator.databinding.DialogUserSettingsBinding
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UserSettingsDialogFragment : BottomSheetDialogFragment() {

    interface UserSettingsListener {
        fun onRequestPermission()
    }

    private var _binding: DialogUserSettingsBinding? = null
    private val binding get() = _binding!!
    private var listener: UserSettingsListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UserSettingsListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement UserSettingsListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUserSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        binding.autoPlaybackSwitch.setOnCheckedChangeListener { _, isChecked ->
            val message = "Auto-playback " + if (isChecked) "ON" else "OFF"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        binding.sendFeedbackBtn.setOnClickListener {
            Toast.makeText(requireContext(), "Feedback action triggered", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            binding.requestPermissionBtn.visibility = View.VISIBLE
            binding.requestPermissionBtn.setOnClickListener {
                listener?.onRequestPermission()
                dismiss()
            }
        } else {
            binding.requestPermissionBtn.visibility = View.GONE
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
