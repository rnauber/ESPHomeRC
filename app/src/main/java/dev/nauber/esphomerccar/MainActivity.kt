package dev.nauber.esphomerccar

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.github.controlwear.virtual.joystick.android.JoystickView


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.topAppBar))

        val imageView = findViewById<ImageView>(R.id.imageView)

        val c = Communication("192.168.0.220", 6053, null)
        //val c = Communication("10.0.2.2", 6053, null)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->

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
            c.setHBridge(0, 0.0f, false)
        }


        val joystick = findViewById<View>(R.id.joystickView) as JoystickView
        joystick.setOnMoveListener { angle, strength ->
            c.setHBridge(0, (joystick.normalizedX - 50f) / 50.0f, false)
            c.setHBridge(1, (joystick.normalizedY - 50f) / -50.0f, false)
        }
    }
}
