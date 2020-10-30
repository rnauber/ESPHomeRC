package dev.nauber.esphomerccar


import java.net.Socket
import Api
import android.util.Log
import com.google.protobuf.AbstractMessage
import com.google.protobuf.ByteString
import java.io.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.typeOf


class Communication(val host: String, val port: Int, val password: String?) : Runnable {
    val TAG = "Communication"
    val t = Thread(this)

    var onImage: ((ByteString) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null

    val msgQueue = ConcurrentLinkedQueue<AbstractMessage>()

    fun setImageStream(stream: Boolean, single: Boolean) {
        msgQueue.add(
            Api.CameraImageRequest.newBuilder().setSingle(single).setStream(stream).build()
        )
        msgQueue.add(
            Api.SubscribeStatesRequest.newBuilder().build()
        )

    }


    private fun sendMessage(m: AbstractMessage, ous: OutputStream) {

        val message_type = when (m) {
            is Api.HelloRequest -> 1
            is Api.ConnectRequest -> 3
            is Api.DisconnectRequest-> 5
            is Api.PingResponse -> 8
            is Api.DeviceInfoRequest -> 9
            is Api.SubscribeStatesRequest -> 20
            is Api.CameraImageRequest -> 45
            else -> 0
        }

        sendRawMessage(m.toByteArray(), message_type, ous)

        Log.v(TAG, "TX_MSG${message_type} ${m.javaClass}")
    }

    private fun sendRawMessage(raw: ByteArray, message_type: Int, ous: OutputStream) {
        ous.write(0)
        writeVarInt(raw.size, ous)
        writeVarInt(message_type, ous)
        ous.write(raw)
        ous.flush()
    }


    private fun receiveMessage(ins: InputStream): AbstractMessage? {
        val (raw_msg, msgType) = receiveRawMessage(ins)
        val msg = when (msgType) {
            2 ->  Api.HelloResponse.parseFrom(raw_msg)
            4 ->  Api.ConnectResponse.parseFrom(raw_msg)
            6 ->  Api.DisconnectResponse.parseFrom(raw_msg)
            7 ->  Api.PingRequest.parseFrom(raw_msg)
            10 ->  Api.DeviceInfoResponse.parseFrom(raw_msg)
            24 -> Api.LightStateResponse.parseFrom(raw_msg)
            44 ->  Api.CameraImageResponse.parseFrom(raw_msg)
            else -> null
        }
        Log.e(TAG, "RX MSG${msgType} ${msg?.javaClass}")
        return msg
    }

    private fun receiveRawMessage(ins: InputStream): Pair<ByteArray, Int> {
        if (ins.read() != 0x00) {
            Log.e(TAG, "Invalid preamble")
        }

        val length = readVarInt(ins)
        val msgType = readVarInt(ins)

        Log.v(TAG, "RAW_RX try to read MSG${msgType} ${length} ")
        val raw_msg = ByteArray(length)
        val res = ins.read(raw_msg)

        Log.v(TAG, "RAW_RX MSG${msgType} ${length}b $raw_msg res=${res}")

        return Pair(raw_msg, msgType)
    }

    fun start() {
        t.start()
    }

    override fun run() {
        try {

            val client = Socket(host, port)

            val ins = client.getInputStream()
            val ous = client.getOutputStream()

            sendMessage(Api.HelloRequest.newBuilder().setClientInfo("esphome rccar").build(),ous)
            val resHello = receiveMessage(ins) as Api.HelloResponse

            val str =
                "Connected to ${client.inetAddress}: ${resHello.serverInfo} API ${resHello.apiVersionMajor}:${resHello.apiVersionMinor}"
            onLog?.invoke(str)
            Log.v(TAG, str)

            sendMessage(Api.ConnectRequest.newBuilder().build(), ous)
            val resConn = receiveMessage(ins) as Api.ConnectResponse
            Log.v(TAG, "ccc " + resConn.invalidPassword)

            sendMessage(Api.DeviceInfoRequest.newBuilder().build(), ous)

            while (true) {
                val msg = receiveMessage(ins)
                when (msg) {
                    is Api.PingRequest -> sendMessage(Api.PingResponse.newBuilder().build(), ous)
                    is Api.CameraImageResponse -> {
                        Log.v(TAG,"Image key=${msg.key} done=${msg.done} data.len=${msg.data.size()} ")
                        onImage?.let { it(msg.data)}
                    }
                    is Api.DeviceInfoResponse -> onLog?.invoke("Info: " + msg.compilationTime)
                    is Api.LightStateResponse ->  Log.v(TAG,"LightStateResponse key=${msg.key} brightness=${msg.brightness}")
                }
                val txmsg = msgQueue.poll()
                if (txmsg != null)
                    sendMessage(txmsg, ous)
            }
            client.close()


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
