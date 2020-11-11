package dev.nauber.esphomerccar

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dev.nauber.esphomerccar.databinding.ActivityMainBinding
import io.github.controlwear.virtual.joystick.android.JoystickView


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    lateinit var controller: Controller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        val imageView = binding.contentMain.imageView

        val c = Communication("192.168.0.220", 6053, null)
        //val c = Communication("10.0.2.2", 6053, null)
        controller = Controller(this, c)

        binding.fab.setOnClickListener { view ->

            Snackbar.make(view, "HIHIHIH", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            c.onLog = { s ->
                Snackbar.make(view, s, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            c.onImage = { img ->
                //val bm = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size())
                //imageView.post { imageView.setImageBitmap(bm) }
                val d = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size()).toDrawable(
                    resources
                )
                imageView.post {
                    imageView.setImageDrawable(d)
                }
                //val source: ImageDecoder.Source = ImageDecoder.createSource(img.asReadOnlyByteBuffer())
                //val drawable: Drawable = ImageDecoder.decodeDrawable(source)
            }
            c.start()
            c.subscribeLogs()
            c.setImageStream(stream = true, single = false)
        }


        val joystick = binding.contentMain.joystickView
        joystick.setOnMoveListener { angle, strength ->
            val x = (joystick.normalizedX - 50f) / 50.0f
            val y = (joystick.normalizedY - 50f) / -50.0f

            controller.updateInput(mapOf("x" to x, "y" to y))


        }
    }
}
