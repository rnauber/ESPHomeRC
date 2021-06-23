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
    private var imageStreamLastReq = 0L

    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(getApplication<Application>().applicationContext)

    val liveImage = MutableLiveData<Bitmap>()

    var currentVehicleId
        get() = sharedPreferences.getLong("CurrentVehicleId", 0)
        set(value) {
            if (currentVehicleId != value) {
                sharedPreferences.edit().putLong("CurrentVehicleId", value).apply()
                //trigger update of all affected properties
                liveCamRotation.postValue(camRotation)
                liveControllerSrc.postValue(controllerSrc)
                reconnect()
            }
        }

    val liveCurrentVehicleId = MutableLiveData(0L)

    fun newVehicleId(): Long {
        return (vehicleIds.maxOrNull() ?: 0) + 1
    }

    fun addVehicle(vid: Long, name: String) {
        Log.v("", "Create vehicle $name vid=$vid")
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
            sharedPreferences.all.map { parseSettingKey(it.key)?.first }.filterNotNull()
                .filter { it > 0 }.distinct()

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

    var controllerSrc: String
        get() = getVehicleSetting(currentVehicleId, "controller_src") ?: Controller.DEFAULTSCRIPT
        set(src) {
            if (src != controllerSrc) {
                setVehicleSetting(currentVehicleId, "controller_src", src)
                controller?.updateSrc(src)
                //liveControllerSrc.postValue(src)
            }
        }

    val liveControllerSrc = MutableLiveData(controllerSrc)

    val camRotation: Float
        get() = getVehicleSetting(currentVehicleId, "cam_rotation")?.toFloat() ?: 0f

    val liveCamRotation = MutableLiveData(camRotation)

    fun reconnect() {
        comm?.stop()
        comm = Communication(esphomeapiurl, esphomeapipassword)

        comm?.onImage = { img ->
            val bm = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size())
            liveImage.postValue(bm)

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
        controller =
            Controller(getApplication<Application>().applicationContext, comm!!, vehicleName)

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

        controller?.updateSrc(controllerSrc)

        controller?.triggerRun()
        controller?.resetInput()

    }

    fun updateInput(map: Map<String, Float>) {
        controller?.updateInput(map)
    }

    fun getInput(key: String): Float? {
        return controller?.getInput(key)
    }

    fun requestPing() {
        comm?.requestPing()
        comm?.setHBridge(-1, System.currentTimeMillis().toFloat(), false)
    }


    companion object {
        const val RENEW_STREAMING_MS = 3000
    }
}


data class LogItem(val source: String, val content: String, val timestamp: String) {
    override fun toString(): String = content
}