package com.mundocrativo.javier.flow.ui.main

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.mundocrativo.javier.flow.R
import com.mundocrativo.javier.flow.Util.CallbackBasedApi
import com.mundocrativo.javier.flow.Util.flowFrom
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import kotlinx.android.synthetic.main.main_fragment.view.textView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private lateinit var flujoBase: Flow<Int>
    private lateinit var flujoAsync : Flow<Int>
    private var cont = 0
    private lateinit var myApi: CallbackBasedApi

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)

        view.tickBtn.setOnClickListener {
            myApi.genera()
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel

        viewModel.texto.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        programaFlujo()
    }


    private fun programaFlujo() = GlobalScope.launch{

        myApi = CallbackBasedApi(5)
        flujoAsync = flowFrom(myApi)

        flujoBase = (0..10).asFlow()


        flujoBase.zip(flujoAsync){
            a,b ->
            "$a -> $b"
        }.collect{
            Log.v("msg","$it")
            viewModel.texto.postValue(it)
        }

        flujoBase.collect{
            Log.v("msg","$it")
            viewModel.texto.postValue(it.toString())
        }


    }




}
