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
    private var imageStreamLastReq = 0L

    fun getImage(): LiveData<Bitmap> {
        return image
    }

    private val log = MutableLiveData<List<LogItem>>(listOf())

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
        val controllerSrc = sharedPreferences.getString("controller_src", Controller.DEFAULTSCRIPT)

        comm?.stop()
        comm = Communication(esphomeapiurl, password)

        comm?.onImage = { img ->
            val bm = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size())
            image.postValue(bm)

            val now = System.currentTimeMillis()
            if (now - imageStreamLastReq > RENEW_STREAMING_MS) {
                comm?.setImageStream(stream = true, single = false)
                imageStreamLastReq = now
            }
        }

        comm?.onLog = { logsrc, logmsg ->
            val newitem = LogItem(logsrc, logmsg, "")
            log.postValue(log.value?.plus(newitem) ?: listOf(newitem))
        }

        comm?.connect()
        comm?.subscribeLogs()
        comm?.setImageStream(stream = false, single = true)// trigger image request

        controller?.stop()
        controller = Controller(context, comm!!)

        controller?.onLog = { logsrc, logmsg ->
            val newitem = LogItem(logsrc, logmsg, "")
            log.postValue(log.value?.plus(newitem) ?: listOf(newitem))
        }

        controller?.onOutput = { controllerOut.postValue(it) }
        if (controllerSrc != null) {
            controller?.updateSrc(controllerSrc)
        }
        controller?.triggerRun()


    }

    fun updateInput(map: Map<String, Float>) {
        controller?.updateInput(map)
    }

    fun requestPing() {
        comm?.requestPing()
        comm?.setHBridge(-1, System.currentTimeMillis().toFloat(), false)
    }

    

    fun updateControllerSrc(src: String) {
        controller?.updateSrc(src)
    }

    companion object {
        val RENEW_STREAMING_MS = 3000
    }
}


data class LogItem(val source: String, val content: String, val timestamp: String) {
    override fun toString(): String = content
}