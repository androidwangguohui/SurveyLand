package com.example.surveyland.ui.view


import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import com.example.surveyland.R

class CustomPromptDialog private constructor(
    context: Context,
    private val builder: Builder
) : Dialog(context, R.style.CustomDialogStyle) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_custom_prompt)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        val containerCustom = findViewById<FrameLayout>(R.id.containerCustom)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)
        val btnNeutral = findViewById<Button>(R.id.btnNeutral)

        tvTitle.text = builder.title
        tvMessage.text = builder.message

        tvTitle.isVisible = builder.title != null
        tvMessage.isVisible = builder.message != null

        // 自定义View
        builder.customView?.let {
            containerCustom.addView(it)
            containerCustom.isVisible = true
        }

        // 按钮
        btnCancel.isVisible = builder.cancelText != null
        btnConfirm.isVisible = builder.confirmText != null
        btnNeutral.isVisible = builder.neutralText != null

        btnCancel.text = builder.cancelText
        btnConfirm.text = builder.confirmText
        btnNeutral.text = builder.neutralText

        btnCancel.setOnClickListener {
            builder.onCancel?.invoke(this)
            dismiss()
        }

        btnConfirm.setOnClickListener {
            builder.onConfirm?.invoke(this)
            dismiss()
        }

        btnNeutral.setOnClickListener {
            builder.onNeutral?.invoke(this)
        }

        setCancelable(builder.cancelable)
        setCanceledOnTouchOutside(builder.outsideCancelable)
    }

    class Builder(private val context: Context) {

        var title: String? = null
        var message: String? = null
        var customView: View? = null

        var cancelText: String? = null
        var confirmText: String? = null
        var neutralText: String? = null

        var onCancel: ((Dialog) -> Unit)? = null
        var onConfirm: ((Dialog) -> Unit)? = null
        var onNeutral: ((Dialog) -> Unit)? = null

        var cancelable: Boolean = true
        var outsideCancelable: Boolean = true

        fun setTitle(text: String) = apply { title = text }
        fun setMessage(text: String) = apply { message = text }
        fun setCustomView(view: View) = apply { customView = view }

        fun setCancel(text: String, action: ((Dialog) -> Unit)? = null) = apply {
            cancelText = text
            onCancel = action
        }

        fun setConfirm(text: String, action: ((Dialog) -> Unit)? = null) = apply {
            confirmText = text
            onConfirm = action
        }

        fun setNeutral(text: String, action: ((Dialog) -> Unit)? = null) = apply {
            neutralText = text
            onNeutral = action
        }

        fun setCancelable(flag: Boolean) = apply { cancelable = flag }
        fun setOutsideCancelable(flag: Boolean) = apply { outsideCancelable = flag }

        fun build(): CustomPromptDialog {
            return CustomPromptDialog(context, this)
        }

        fun show(): CustomPromptDialog {
            val dialog = build()
            dialog.show()
            return dialog
        }
    }
}