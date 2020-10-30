package dev.nauber.esphomerccar

import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->


            Snackbar.make(view, "HIHIHIH", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            var c = Communication("192.168.0.220", 6053, null)
            //var c = Communication("www.google.de", 80)
            c.onLog = { s ->
                Log.v(TAG, "XX " + s)

                Snackbar.make(view, s, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            c.onImage = { i ->
                Log.v(TAG, "XX " + i)

                Snackbar.make(view, "XX " + i, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            c.start()
            c.setImageStream(true,false)

        }
    }




//        fullscreenContent.post {
//            fullscreenContent.setText(s)
//        }

}
