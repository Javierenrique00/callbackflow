package com.mundocrativo.javier.flow.Util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CallbackBasedApi(entrada:Int) {
    private var callBack : Callback? = null
    private var dato = entrada
    private var completado = false

    interface Callback{
        fun onNextValue(value:Int)
        fun onApiError(cause:Throwable)
        fun onCompleted()
    }

    fun register(callBack:Callback){
        this.callBack = callBack
    }

    fun unregister(){
        callBack = null
    }

    fun genera(){
        if(!completado){
            //if(dato<0) callBack.onApiError()
            if(dato++>10){
                callBack!!.onCompleted()
                completado = true
            }
            else
            {
                callBack!!.onNextValue(dato)
            }
        }

    }

}

//https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html
// manejo de flow de una api, en forma cold

//--consideraciones adicionales
//https://medium.com/@elizarov/callbacks-and-kotlin-flows-2b53aa2525cf

fun flowFrom(api: CallbackBasedApi): Flow<Int> = callbackFlow {
    val callback = object : CallbackBasedApi.Callback {
        override fun onNextValue(value: Int) {
            // To avoid blocking you can configure channel capacity using
            // either buffer(Channel.CONFLATED) or buffer(Channel.UNLIMITED) to avoid overfill
            try {
                sendBlocking(value)
            } catch (e: Exception) {
                // Handle exception from the channel: failure in flow or premature closing
            }

        }

        override fun onCompleted() {
            channel.close()
        }

        override fun onApiError(cause: Throwable) {
            cancel(CancellationException("API Error", cause))
        }
    }

    api.register(callback)
    /*
     * Suspends until either 'onCompleted'/'onApiError' from the callback is invoked
     * or flow collector is cancelled (e.g. by 'take(1)' or because a collector's coroutine was cancelled).
     * In both cases, callback will be properly unregistered.
     */
    awaitClose {
        api.unregister()

    }

}