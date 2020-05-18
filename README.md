# callbackflow

Android Kotlin example how to run an asynchronous flow using callbackFlow.

If you understand suspending functions in Kotlin, you know that this function only returns a single value, but if you need to return asynchronous values, Flow is one answer.

The source of this information is in kotlin main documentation: https://kotlinlang.org/docs/reference/coroutines/flow.html


Flow is named cold asynchronous, that means that the returned value is calculated when is collected. Let see.

        fun foo(): Flow<Int> = flow { // flow builder
            for (i in 1..3) {
                delay(100) // pretend we are doing something useful here
                emit(i) // emit next value
            }
        }

        fun main() = runBlocking<Unit> {
            // Launch a concurrent coroutine to check if the main thread is blocked
            launch {
                for (k in 1..3) {
                    println("I'm not blocked $k")
                    delay(100)
                }
            }
            // Collect the flow
            foo().collect { value -> println(value) } 
        }

Because flow are cold, there is no obvious way to transform asynchronous events from any source to a flow, but what is cold and what is hot ?

You can check https://medium.com/@elizarov/cold-flows-hot-channels-d74769805f9 to see the difference.

I understand that a Hot source is a source that can emit an event/data and the reception of this event is independent of the source. So the receptor has to know how to get the event/data to receive or compute the event/data. One emitter and one receptor. In a real program the receptor has to be alive or running to hear/receive the events/data. In that kind of systems Kotlin uses a Channel functionalities. You can see in Kotlin documentation:  https://kotlinlang.org/docs/reference/coroutines/channels.html

Channels works with coroutines and the meaning is: Deferred values provide a convenient way to transfer a single value between coroutines. Channels provide a way to transfer a stream of values.

**The example that I want to show is how can I use flow asynchronous cold operations on sources that are hot, like events ?**

Searching for answer  I found a way to do that: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html

The answer is "callbackFlow", it say that:  *"Creates an instance of the cold Flow with elements that are sent to a SendChannel provided to the builder’s block of code via ProducerScope. It allows elements to be produced by code that is running in a different context or concurrently."*

*"The resulting flow is cold, which means that block is called every time a terminal operator is applied to the resulting flow."*

So I understand that is like a cold code emitted in a hot source packet,  and translated to be used by cold consumer.

The code that resumes all this explanation:

    fun flowFrom(api: CallbackBasedApi): Flow<T> = callbackFlow {
        val callback = object : Callback { // implementation of some callback interface
            override fun onNextValue(value: T) {
                // To avoid blocking you can configure channel capacity using
                // either buffer(Channel.CONFLATED) or buffer(Channel.UNLIMITED) to avoid overfill
                try {
                    sendBlocking(value)
                } catch (e: Exception) {
                    // Handle exception from the channel: failure in flow or premature closing
                }
            }
            override fun onApiError(cause: Throwable) {
                cancel(CancellationException("API Error", cause))
            }
            override fun onCompleted() = channel.close()
        }
        api.register(callback)
        /*
        * Suspends until either 'onCompleted'/'onApiError' from the callback is invoked
        * or flow collector is cancelled (e.g. by 'take(1)' or because a collector's coroutine was cancelled).
        * In both cases, callback will be properly unregistered.
        */
            awaitClose { api.unregister(callback) }
        }


## Example in Andorid ##

I made an example in Android. There is a Button that is hot by nature and a class called "CallbackBasedApi" with a parameter for the constructor:

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

This class has an interface. This interface has to be registerd and unregistered. When is registered We can use it to create values with *callBack.onNextValue()* until values get finished with *callback.onCompleted()*. If the internal operation of the class needs to generate and error we can do it with *callBack.onApiError()*

In the main fragment of the Android app, We create this class in the function *programaFlujo()*

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

When We click in the button the class myApi:CallbackBasedApi call *genera()* as you can see:

        view.tickBtn.setOnClickListener {
            myApi.genera()
        }

The transformation from hot events that are created with the click button are made with the function *flowFrom(myApi)* that takes the class myApi as parameter it register the callback to the hot flows. Check the implementation of the function:

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

All the magic comes from Kotlin *callbackFlow* that first register the callback, then the flow and callback begin to work until *api.unregister()*