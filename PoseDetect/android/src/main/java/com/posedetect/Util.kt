package com.posedetect

import android.util.Log

class Util {
  companion object {
    private const val LOG_TAG = "posedt"

    fun v(format: String) {
      if (BuildConfig.DEBUG) Log.v(LOG_TAG, format)
    }

    fun d(format: String) {
      if (BuildConfig.DEBUG) Log.d(LOG_TAG, format)
    }

    fun i(format: String) {
      if (BuildConfig.DEBUG) Log.i(LOG_TAG, format)
    }

    fun e(format: String) {
      if (BuildConfig.DEBUG) Log.e(LOG_TAG, format)
    }

    fun w(format: String) {
      if (BuildConfig.DEBUG) Log.w(LOG_TAG, format)
    }
  }
}
