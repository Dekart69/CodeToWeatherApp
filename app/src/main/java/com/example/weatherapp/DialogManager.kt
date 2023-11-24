package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

object DialogManager {
    fun locationSettingsDialog(context: Context,listener:Listener){
        val builder=AlertDialog.Builder(context)
        val dialog=builder.create()
        dialog.setTitle("Enable location?")
        dialog.setMessage("Location disabled,do you want to enable location?")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK"){_,_ ->
            listener.OnClick(null)
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Cancel"){_,_ ->
            dialog.cancel()
        }
        dialog.show()

    }

    fun searchByNameDialog(context: Context,listener:Listener){
        val builder=AlertDialog.Builder(context)
        val edName=EditText(context)
        builder.setView(edName)
        val dialog=builder.create()
        dialog.setTitle("City: ")

        dialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK"){_,_ ->
            listener.OnClick(edName.text.toString())
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Cancel"){_,_ ->
            dialog.cancel()
        }
        dialog.show()

    }

    interface Listener{
        fun OnClick(name:String?)
    }
}