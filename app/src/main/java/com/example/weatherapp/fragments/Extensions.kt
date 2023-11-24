package com.example.weatherapp.fragments

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

fun Fragment.isPermissionGranted(perm:String):Boolean {
    return ContextCompat.checkSelfPermission(
        activity as AppCompatActivity,
        perm)==PackageManager.PERMISSION_GRANTED

}