package dev.nauber.esphomerccar

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val imageView=findViewById<ImageView>(R.id.imageView)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->


            Snackbar.make(view, "HIHIHIH", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            //val c = Communication("192.168.0.220", 6053, null)
            val c = Communication("10.0.2.2", 6053, null)
            //var c = Communication("www.google.de", 80)
            c.onLog = { s ->
                Log.v(TAG, "XX " + s)

                Snackbar.make(view, s, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            c.onImage = { img ->
                Snackbar.make(view, "XX " + img.size(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                val bm=BitmapFactory.decodeByteArray(img.toByteArray(),0,img.size())
                imageView.post { imageView.setImageBitmap(bm)}
            }
            c.start()
            c.setImageStream(stream = true, single = false)
            c.listEntities()

        }
    }




//        fullscreenContent.post {
//            fullscreenContent.setText(s)
//        }

}
