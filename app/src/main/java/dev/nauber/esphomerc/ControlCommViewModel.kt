package dev.nauber.esphomerc

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceManager


class ControlCommViewModel(app: Application) : ObservableAndroidViewModel(app) {

    private var comm: Communication? = null

    private var controller: Controller? = null
    private val image = MutableLiveData<Bitmap>()
    private var imageStreamLastReq = 0L

    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(getApplication<Application>().applicationContext)

    private var currentVehicleId_: Long = 0L

    var currentVehicleId: Long
        get() = currentVehicleId_
        set(v) {
            if (currentVehicleId_ != v) {
                currentVehicleId_ = v
                liveCurrentVehicleId.postValue(v)
            }
        }

    val liveCurrentVehicleId = MutableLiveData(currentVehicleId)

    fun newVehicleId(): Long {
        return (vehicleIds.maxOrNull() ?: 0) + 1
    }

    fun addVehicle(vid: Long, name: String) {
        setVehicleSetting(vid, "name", name)
        liveVehicleIds.postValue(vehicleIds)
    }

    fun removeVehicle(vid: Long) {
        val keys =
            sharedPreferences.all.filter { parseSettingKey(it.key)?.first == vid }.map { it.key }
        val s = sharedPreferences.edit()
        keys.forEach { s.remove(it) }
        s.apply()
        liveVehicleIds.postValue(vehicleIds)
    }

    val vehicleIds: List<Long>
        get() =
            sharedPreferences.all.map { parseSettingKey(it.key)?.first }.filterNotNull().distinct()

    val liveVehicleIds = MutableLiveData(vehicleIds)

    private fun parseSettingKey(s: String): Pair<Long, String>? {
        return try {
            val (vid, key) = s.split(":", limit = 2)
            Pair(vid.toLong(), key)
        } catch (e: Exception) {
            null
        }
    }

    private fun genSettingsKey(vid: Long, key: String): String {
        return "${vid}:${key}"
    }

    fun getVehicleSetting(vid: Long, key: String, defValue: String? = null): String? {
        return sharedPreferences.getString(genSettingsKey(vid, key), defValue)
    }

    fun setVehicleSetting(vid: Long, key: String, value: String) {
        sharedPreferences.edit().putString(genSettingsKey(vid, key), value).apply()
    }

    fun getCameraImage(): LiveData<Bitmap> {
        return image
    }

    private val log = MutableLiveData<List<LogItem>>(listOf())

    fun getLog(): LiveData<List<LogItem>> {
        return log
    }

    private val auxControls = MutableLiveData<List<String>>(listOf())

    fun getAuxControls(): LiveData<List<String>> {
        return auxControls
    }

    private val controllerOut = MutableLiveData<String>()
    fun getControllerOut(): LiveData<String> {
        return controllerOut
    }

    val settingsDataStore =
        (object : PreferenceDataStore() {

            override fun putString(key: String, value: String?) {
                setVehicleSetting(currentVehicleId, key, value ?: "")
            }

            override fun getString(key: String, defValue: String?): String {
                return getVehicleSetting(currentVehicleId, key, defValue) ?: ""
            }
        })

    var vehicleName: String
        @Bindable
        get() {
            return getVehicleSetting(currentVehicleId, "name") ?: "Unnamed"
        }
        set(value) {
            val old = vehicleName

            if (old != value) {
                setVehicleSetting(currentVehicleId, "name", value)

                // Notify observers of a new value.
                notifyPropertyChanged(BR.vehicleName)
                liveVehicleIds.postValue(vehicleIds)// dirty hack to trigger an update of the vehicle profiles in the navbar
            }
        }
    var esphomeapiurl: String
        get() {
            return getVehicleSetting(currentVehicleId, "esphomeapiurl") ?: "10.0.2.2"
        }
        set(value) {
            setVehicleSetting(currentVehicleId, "esphomeapiurl", value)
        }

    val esphomeapipassword: String?
        get() = getVehicleSetting(currentVehicleId, "esphomeapipassword")

    val controller_src: String
        get() = getVehicleSetting(currentVehicleId, "controller_src") ?: Controller.DEFAULTSCRIPT

    val camRotation: Float
        get() = getVehicleSetting(currentVehicleId, "cam_rotation")?.toFloat() ?: 0f

    fun reconnect() {
        comm?.stop()
        comm = Communication(esphomeapiurl, esphomeapipassword)

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
        controller = Controller(getApplication<Application>().applicationContext, comm!!)

        controller?.onLog = { logsrc, logmsg ->
            val newitem = LogItem(logsrc, logmsg, "")
            log.postValue(log.value?.plus(newitem) ?: listOf(newitem))
            Log.i("onlog", logmsg)
        }

        controller?.onOutput = {
            controllerOut.postValue(it)

            val auxControlsLast = auxControls.value
            val auxControlsNew = controller?.getAuxControlLabels()
            if ((auxControlsLast != auxControlsNew) && (auxControlsNew != null))
                auxControls.postValue(auxControlsNew!!)
        }

        controller?.updateSrc(controller_src)

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
        sharedPreferences.edit().putString("controller_src", src).apply()
        controller?.updateSrc(src)
    }


    companion object {
        const val RENEW_STREAMING_MS = 3000
    }
}


data class LogItem(val source: String, val content: String, val timestamp: String) {
    override fun toString(): String = content
}