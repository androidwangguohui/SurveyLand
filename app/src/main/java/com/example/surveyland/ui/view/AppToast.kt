package com.example.surveyland.ui.view

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.surveyland.R

object AppToast {

    private var toast: Toast? = null

    enum class Type {
        NORMAL,
        SUCCESS,
        ERROR,
        WARNING
    }

    fun show(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
        type: Type = Type.NORMAL
    ) {

        toast?.cancel()

        val view = LayoutInflater.from(context)
            .inflate(R.layout.toast_layout, null)

        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)

        tvMessage.text = message

        when (type) {
            Type.SUCCESS -> {
                ivIcon.visibility = View.VISIBLE
                ivIcon.setImageResource(android.R.drawable.checkbox_on_background)
            }
            Type.ERROR -> {
                ivIcon.visibility = View.VISIBLE
                ivIcon.setImageResource(android.R.drawable.ic_delete)
            }
            Type.WARNING -> {
                ivIcon.visibility = View.VISIBLE
                ivIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            }
            else -> {
                ivIcon.visibility = View.GONE
            }
        }

        toast = Toast(context.applicationContext)
        toast?.duration = duration
        toast?.view = view
        toast?.setGravity(Gravity.CENTER, 0, 0)
        toast?.show()
    }

    fun success(context: Context, message: String) {
        show(context, message, Toast.LENGTH_SHORT, Type.SUCCESS)
    }

    fun error(context: Context, message: String) {
        show(context, message, Toast.LENGTH_SHORT, Type.ERROR)
    }

    fun warning(context: Context, message: String) {
        show(context, message, Toast.LENGTH_SHORT, Type.WARNING)
    }

    fun cancel() {
        toast?.cancel()
    }
}