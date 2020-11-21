package dev.nauber.esphomerc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager

class ControlCommViewModel   : ViewModel() {

    private var comm:Communication? = null

    private var controller: Controller?=null
    private val image = MutableLiveData<Bitmap>()

    fun getImage(): LiveData<Bitmap> {
        return image
    }


    fun start(context: Context){

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val esphomeapiurl = sharedPreferences.getString("esphomeapiurl", "10.0.2.2")
        val password = sharedPreferences.getString("esphomeapipassword", null)
        comm = Communication(esphomeapiurl, password)


        comm?.onImage = { img ->
            val bm = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size())
            image.postValue(bm)
        }

        comm?.connect()
        comm?.subscribeLogs()
        comm?.setImageStream(stream = true, single = false)

        controller= Controller(context, comm!!)

    }

    fun updateInput(map: Map<String, Float>) {
        controller?.updateInput(map)
    }
}