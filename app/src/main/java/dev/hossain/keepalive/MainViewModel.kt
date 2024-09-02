package dev.hossain.keepalive

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val allPermissionsGranted = MutableLiveData(false)
}
