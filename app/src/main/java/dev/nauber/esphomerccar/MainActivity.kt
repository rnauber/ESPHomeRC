package dev.nauber.esphomerccar

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.apache.poi.hssf.usermodel.HeaderFooter.file


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.topAppBar))

        val imageView = findViewById<ImageView>(R.id.imageView)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->

            Snackbar.make(view, "HIHIHIH", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            val c = Communication("192.168.0.220", 6053, null)
            //val c = Communication("10.0.2.2", 6053, null)
            //var c = Communication("www.google.de", 80)
            c.onLog = { s ->
                Snackbar.make(view, s, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            c.onImage = { img ->
                //val bm = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size())
                //imageView.post { imageView.setImageBitmap(bm) }
                val d = BitmapFactory.decodeByteArray(img.toByteArray(), 0, img.size()).toDrawable(getResources()
                )
                imageView.post { imageView.setImageDrawable(d)
                }
                //val source: ImageDecoder.Source = ImageDecoder.createSource(img.asReadOnlyByteBuffer())
                //val drawable: Drawable = ImageDecoder.decodeDrawable(source)
            }
            c.start()
            c.listEntities()
            c.subscribeLogs()
            c.setImageStream(stream = true, single = false)
        }
    }
}
