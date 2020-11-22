package dev.nauber.esphomerc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager

class ControlCommViewModel : ViewModel() {

    private var comm: Communication? = null

    private var controller: Controller? = null
    private val image = MutableLiveData<Bitmap>()

    fun getImage(): LiveData<Bitmap> {
        return image
    }

    private val log = MutableLiveData<List<LogItem>>(listOf())

    init {
        Log.d("x", "created $this log=$log ")
    }

    fun getLog(): LiveData<List<LogItem>> {
        return log
    }

    private val controllerOut = MutableLiveData<String>()
    fun getControllerOut(): LiveData<String> {
        return controllerOut
    }


    fun reconnect(context: Context) {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val esphomeapiurl = sharedPreferences.getString("esphomeapiurl", "10.0.2.2")
        val password = sharedPreferences.getString("esphomeapipassword", null)

        comm?.stop()
        comm = Communication(esphomeapiurl, password)

        comm?.onImage = { img ->
            val bm = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size())
            image.postValue(bm)
        }

        comm?.onLog = { logsrc, logmsg ->
            val newitem = LogItem(logsrc, logmsg, "")
            log.postValue(log.value?.plus(newitem) ?: listOf(newitem))
        }

        comm?.connect()
        comm?.subscribeLogs()
        comm?.setImageStream(stream = true, single = false)

        controller = Controller(context, comm!!)

        controller?.onLog = { logsrc, logmsg ->
            val newitem = LogItem(logsrc, logmsg, "")
            log.postValue(log.value?.plus(newitem) ?: listOf(newitem))
        }

        controller?.onOutput = { controllerOut.postValue(it) }

    }

    fun updateInput(map: Map<String, Float>) {
        controller?.updateInput(map)
    }

    fun updateControllerSrc(src: String) {
        controller?.script = src
    }
}


data class LogItem(val source: String, val content: String, val timestamp: String) {
    override fun toString(): String = content
}