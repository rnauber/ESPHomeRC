package dev.nauber.esphomerc


import com.faendir.rhino_android.RhinoAndroidHelper
import org.mozilla.javascript.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference


class Controller(val context: android.content.Context, val comm: Communication) : Runnable {
    private var script = AtomicReference<String>()
    private var inpUser = ConcurrentHashMap<String, Float>()
    private val runQueue = LinkedBlockingQueue<String>()

    var onOutput: ((String) -> Unit)? = null
    var onLog: ((String, String) -> Unit)? = null

    private val t = Thread(this)

    init {
        inpUser.putAll(mapOf("x" to 0f, "y" to 0f, "aux0" to 0f))
        t.start()
    }

    override fun run() {
        val rhinoAndroidHelper = RhinoAndroidHelper(context)
        val rhinoContext = rhinoAndroidHelper.enterContext();
        rhinoContext.optimizationLevel = 9;
        val scope = rhinoContext.initSafeStandardObjects();

        while (true) {
            val reason = runQueue.take()
            if (reason == "stop")
                break

            var output = ""
            try {
                //prepare input

                val inpObject = NativeObject()
                val inpUserObject = NativeObject()
                inpUser.forEach(10) { k, v -> inpUserObject.defineProperty(k, v, 0) }

                inpObject.defineProperty("reason", reason, 0)
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
                        } catch (e: EvaluatorException) {
                            0.0f
                        }
                        val brake = try {
                            Context.jsToJava(hb["brake"], ScriptRuntime.BooleanClass) as Boolean
                        } catch (e: EvaluatorException) {
                            false
                        }
                        output += "setHBridge(index = $index, strength = $strength, brake = $brake) \n"
                        comm.setHBridge(index = index, strength = strength, brake = brake)

                    } catch (t: EvaluatorException) {
                    }
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

    fun updateInput(map: Map<String, Float>) {
        inpUser.putAll(map)
        runQueue.put("userinput")
    }

    fun triggerRun() {
        runQueue.put("manual")
    }

    fun updateSrc(s: String) {
        script.set(s)
    }

    fun stop() {
        runQueue.put("stop")
    }

    companion object {
        const val LOGTAG = "Control"
        val DEFAULTSCRIPT = """

        outp = {};
        outp.display = "Hi from the controller, I am running because " + inp["reason"] ;
                
        if (typeof i == "undefined") {
            i = 0;
        }
        i++;
        outp.display += " i=" + i +"\n";
        
        outp.hbridge = [
        //                {"index":0, "strength": inp.user.x * 0.6, "brake":false}, 
        //                {"index":1, "strength": inp.user.y * 0.6, "brake":false},
        //                {"index":2, "strength": inp.user.y * 0.4, "brake":false},
                       ]
        
    """.trimIndent()

    }
}

