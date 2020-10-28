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

            var c = Communication("192.168.0.220", 6053)
            //var c = Communication("www.google.de", 80)
            c.start() { s ->
                Log.v(TAG, "XX " + s)

                Snackbar.make(view, s, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }


        }
    }

//        fullscreenContent.post {
//            fullscreenContent.setText(s)
//        }

}
