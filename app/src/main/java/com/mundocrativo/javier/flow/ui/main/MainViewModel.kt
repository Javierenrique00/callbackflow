package com.mundocrativo.javier.flow.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    val texto : MutableLiveData<String> by lazy { MutableLiveData<String>() }

}
