package dev.nauber.esphomerc


import com.faendir.rhino_android.RhinoAndroidHelper
import org.mozilla.javascript.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


class Controller(val context: android.content.Context, val comm: Communication) : Runnable {
    private var script = AtomicReference<String>()
    private var inpUser = ConcurrentHashMap<String, Float>()
    private var inpUserAuxControls = ConcurrentLinkedQueue<String>()
    private val runQueue = LinkedBlockingQueue<String>()

    var onOutput: ((String) -> Unit)? = null
    var onLog: ((String, String) -> Unit)? = null

    private val t = Thread(this)

    init {
        resetInput()
        t.start()
    }

    override fun run() {
        val rhinoAndroidHelper = RhinoAndroidHelper(context)
        val rhinoContext = rhinoAndroidHelper.enterContext()
        rhinoContext.optimizationLevel = 9
        val scope = rhinoContext.initSafeStandardObjects()
        var timeout = INFINITY;

        while (true) {
            var reason = runQueue.poll(timeout, TimeUnit.MILLISECONDS)
            if (reason == null)
                reason = "time"

            if (reason == "stop")
                break

            var output = ""
            try {
                //prepare input

                val inpObject = NativeObject()
                val inpUserObject = NativeObject()
                inpUser.forEach(10) { k, v -> inpUserObject.defineProperty(k, v, 0) }

                inpObject.defineProperty("reason", reason, 0)
                inpObject.defineProperty("timestamp_ms", System.currentTimeMillis(), 0)
                inpObject.defineProperty("user", inpUserObject, 0)

                ScriptableObject.putProperty(scope, "inp", inpObject)

                rhinoContext.evaluateString(scope, script.get(), "<control_script>", 1, null)

                val outp = scope.get("outp") as NativeObject

                val log = outp["log"]
                if (log != null)
                    onLog?.invoke(LOGTAG, log.toString())

                val display = outp["display"]
                if (display != null)
                    output += display.toString() + "\n"

                val hbridge = outp["hbridge"] as? Iterable<*>
                hbridge?.forEach {
                    val hb = it as NativeObject
                    try {
                        val index = Context.jsToJava(hb["index"], ScriptRuntime.IntegerClass) as Int
                        val strength = try {
                            Context.jsToJava(hb["strength"], ScriptRuntime.FloatClass) as Float
                        } catch (e: Exception) {
                            0.0f
                        }
                        val brake = try {
                            Context.jsToJava(hb["brake"], ScriptRuntime.BooleanClass) as Boolean
                        } catch (e: Exception) {
                            false
                        }
                        output += "setHBridge(index = $index, strength = $strength, brake = $brake) \n"
                        comm.setHBridge(index = index, strength = strength, brake = brake)

                    } catch (t: Exception) {
                    }
                }

                val user = outp["user"] as NativeObject

                try {
                    val auxLabels = user["aux_labels"] as? Collection<String>
                    if (auxLabels != null) {
                        inpUserAuxControls.clear()
                        inpUserAuxControls.addAll(auxLabels)
                    }
                } catch (t: TypeCastException) {
                }

                try {
                    val intervalms = user["interval_ms"] as? Int
                    timeout = intervalms?.toLong() ?: INFINITY
                } catch (t: TypeCastException) {
                }

            } catch (t: Throwable) {
                t.printStackTrace()
                output += "${t.message} \n ${t.stackTrace[0]} \n"
            }

            onOutput?.invoke(output)
        }
        onLog?.invoke(LOGTAG, "Control stopped!")
        Context.exit()
    }

    private fun resetInput() {
        inpUser.clear()
        inpUser.putAll(mapOf("x" to 0f, "y" to 0f))
        inpUserAuxControls.forEachIndexed { i, s -> inpUser.put("aux$i", 0f) }
    }

    fun updateInput(map: Map<String, Float>) {
        inpUser.putAll(map)
        runQueue.put("userinput")
    }

    fun getAuxControlLabels(): List<String> {
        return inpUserAuxControls.toList()
    }

    fun triggerRun() {
        runQueue.put("manual")
    }

    fun updateSrc(s: String) {
        script.set(s)
        resetInput()
    }

    fun stop() {
        runQueue.put("stop")
    }

    companion object {
        const val INFINITY = 100_000_000L
        const val LOGTAG = "Control"
        val DEFAULTSCRIPT = """
            outp = {};
            outp.display = "Hi from the controller, I am running because " + 
                            inp["reason"] + " at " + inp["timestamp_ms"] + " ms";
                    
            if (typeof i == "undefined") {
                i = 0;
            }
            
            i++;
            outp.display += " i=" + i +"\n";
            //outp.log="log " + i + "  " + inp.user.x + " " + inp.user.y; 
            
            var fwd = inp.user.y * 2.0 ;
            fwd = Math.max(Math.min(1.0, fwd), -1.0); //limit to +-1.0
            fwd *= inp.user.aux0;
            
            var lr= inp.user.x;
            if (Math.abs(lr) > 0.3)
              lr = lr * 1.1;
            else
              lr = 0.0;
            
            outp.hbridge = [
                           {"index":0, "strength": lr , "brake":false}, 
                           {"index":1, "strength": fwd , "brake":false},
                           {"index":2, "strength":  inp.user.aux1, "brake":false},
            ];
            
            outp.user = {};
            outp.user.aux_labels=["vmax", "light"]; // aux0 = vmax, aux1=light
            outp.user.interval_ms = 10000 ;// regular interval to call this script, 0 means as fast as possible... 
            
    """.trimIndent()

    }
}

