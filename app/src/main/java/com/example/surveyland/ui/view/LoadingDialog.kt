package com.example.surveyland.ui.view

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.surveyland.databinding.DialogLoadingBinding

class LoadingDialog : DialogFragment() {

    private var _binding: DialogLoadingBinding? = null
    private val binding get() = _binding!!

    private var message: String = "加载中..."

    companion object {

        private var instance: LoadingDialog? = null

        fun show(
            manager: FragmentManager,
            message: String = "加载中...",
            cancelable: Boolean = false
        ) {
            if (instance == null) {
                instance = LoadingDialog()
            }

            instance?.apply {
                this.message = message
                isCancelable = cancelable

                if (!isAdded) {
                    show(manager, "LoadingDialog")
                }
            }
        }

        fun dismiss() {
            instance?.dismissAllowingStateLoss()
            instance = null
        }

        fun updateMessage(newMessage: String) {
            instance?._binding?.tvMessage?.text = newMessage
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvMessage.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}