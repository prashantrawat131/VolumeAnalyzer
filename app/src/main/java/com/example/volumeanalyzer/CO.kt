package com.example.volumeanalyzer

import android.content.Context
import android.util.Log
import android.widget.Toast

class CO {
    companion object {
        const val TAG = "tagJi"

        fun log(message: String) {
            Log.d(TAG, message)
        }

        fun toast(context: Context,message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}