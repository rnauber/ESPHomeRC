package dev.nauber.esphomerccar


import java.net.Socket
import Api
import android.util.Log
import com.google.protobuf.AbstractMessage
import com.google.protobuf.ByteString
import com.google.protobuf.CodedInputStream
import org.apache.poi.util.HexDump
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean


class Communication(val url: String?, val password: String?) : Runnable {
    val TAG = "Communication"
    val ClientInfoString = "EspHomeRC"

    val t: Thread = Thread(this, "CommunicationThread")

    val stop = AtomicBoolean(false)

    private val maxMsgLen = 1024 * 1024   // prevent OOM

    var onImage: ((ByteString) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null

    private val msgQueue = ConcurrentLinkedQueue<AbstractMessage>()
    private val entitiesServices = ConcurrentHashMap<Int, Api.ListEntitiesServicesResponse>()
    private val entitiesCamera = ConcurrentHashMap<Int, Api.ListEntitiesCameraResponse>()

    fun setImageStream(stream: Boolean, single: Boolean) {
        msgQueue.add(
            Api.CameraImageRequest.newBuilder().setSingle(single).setStream(stream).build()
        )
        msgQueue.add(
            Api.SubscribeStatesRequest.newBuilder().build()
        )
    }

    private fun listEntities() {
        msgQueue.add(Api.ListEntitiesRequest.newBuilder().build())
    }

    fun subscribeLogs() {
        msgQueue.add(
            Api.SubscribeLogsRequest.newBuilder().setLevel(Api.LogLevel.LOG_LEVEL_VERBOSE).build()
        )
    }

    fun setHBridge(index: Int, strength: Float, brake: Boolean) {
        val key = entitiesServices.searchValues(50) { if (it.name == "hbridge") it else null }?.key
            ?: return

        val argIndex = Api.ExecuteServiceArgument.newBuilder().setInt(index)
        val argStrength = Api.ExecuteServiceArgument.newBuilder().setFloat(strength)
        val argBrake = Api.ExecuteServiceArgument.newBuilder().setBool(brake)
        Log.d(TAG, "setHBridge($index, $strength, $brake) key=$key")
        msgQueue.add(
            Api.ExecuteServiceRequest.newBuilder().setKey(key).addArgs(argIndex)
                .addArgs(argStrength)
                .addArgs(argBrake).build()
        )
    }

    private fun sendMessage(m: AbstractMessage, ous: OutputStream) {
        val messageType = when (m) {
            is Api.HelloRequest -> 1
            is Api.ConnectRequest -> 3
            is Api.DisconnectRequest -> 5
            is Api.PingResponse -> 8
            is Api.DeviceInfoRequest -> 9
            is Api.ListEntitiesRequest -> 11
            is Api.SubscribeStatesRequest -> 20
            is Api.SubscribeLogsRequest -> 28
            is Api.ExecuteServiceRequest -> 42
            is Api.CameraImageRequest -> 45
            else -> null
        }
        if (messageType != null)
            sendRawMessage(m.toByteArray(), messageType, ous)

        Log.v(TAG, "TX_MSG${messageType} ${m.javaClass}")
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
        var msg: AbstractMessage? = null
        try {
            msg = when (msgType) {
                2 -> Api.HelloResponse.parseFrom(raw_msg)
                4 -> Api.ConnectResponse.parseFrom(raw_msg)
                6 -> Api.DisconnectResponse.parseFrom(raw_msg)
                7 -> Api.PingRequest.parseFrom(raw_msg)
                10 -> Api.DeviceInfoResponse.parseFrom(raw_msg)
                15 -> Api.ListEntitiesLightResponse.parseFrom(raw_msg)
                19 -> Api.ListEntitiesDoneResponse.parseFrom(raw_msg)
                24 -> Api.LightStateResponse.parseFrom(raw_msg)
                29 -> Api.SubscribeLogsResponse.parseFrom(raw_msg)
                41 -> Api.ListEntitiesServicesResponse.parseFrom(raw_msg)
                43 -> Api.ListEntitiesCameraResponse.parseFrom(raw_msg)
                44 -> Api.CameraImageResponse.parseFrom(raw_msg)
                else -> null
            }
        } catch (e: com.google.protobuf.InvalidProtocolBufferException) {
            e.printStackTrace()
        }
        Log.v(TAG, "RX MSG${msgType} ${msg?.javaClass}")
        return msg
    }

    private fun receiveRawMessage(ins: InputStream): Pair<ByteArray, Int> {
        if (ins.read() != 0x00) {
            Log.e(TAG, "Invalid preamble")
        }

        val length = readVarInt(ins)
        val msgType = readVarInt(ins)

        Log.v(TAG, "RAW_RX try to read MSG${msgType} with ${length} bytes")
        if (length > maxMsgLen) {
            Log.e(TAG, "Ignoring message larger than ${maxMsgLen} bytes: $length bytes.")
            return Pair(ByteArray(0), -1)
        }

        val raw_msg = ByteArray(length)

        var pos = 0
        while (pos < length) {
            val read = ins.read(raw_msg, pos, length - pos)
            Log.d(TAG, "RAW_RX read ${read} bytes pos=$pos")
            if (read > 0)
                pos += read
            else {
                Log.e(TAG, "Reading failed read=$read!")
                return Pair(ByteArray(0), -1)
            }
        }

        Log.v(TAG, "RAW_RX MSG${msgType} ${length}b $raw_msg")
        //Log.d(TAG, HexDump.dump(raw_msg, 0, 0))

        return Pair(raw_msg, msgType)
    }

    fun connect() {
        t.start()
    }

    fun stop() {
        stop.set(true)
    }

    private fun parseUrl(url: String?): Pair<String, Int> {
        if (url == null)
            return  Pair("127.0.0.1", 6053)
        val parts = url.split(":")
        val host = parts.get(0)
        val port = parts.getOrNull(5)?.toInt() ?: 6053
        return Pair(host, port)
    }

    override fun run() {
        try {
            val (host, port) = parseUrl(url)
            val client = Socket(host, port)

            val ins = client.getInputStream()
            val ous = client.getOutputStream()

            sendMessage(Api.HelloRequest.newBuilder().setClientInfo(ClientInfoString).build(), ous)
            val resHello = receiveMessage(ins) as Api.HelloResponse

            val str =
                "Connected to ${client.inetAddress}: ${resHello.serverInfo} API ${resHello.apiVersionMajor}:${resHello.apiVersionMinor}"
            onLog?.invoke(str)
            Log.v(TAG, str)

            sendMessage(Api.ConnectRequest.newBuilder().build(), ous)
            val resConn = receiveMessage(ins) as Api.ConnectResponse
            Log.v(TAG, "ccc " + resConn.invalidPassword)

            sendMessage(Api.DeviceInfoRequest.newBuilder().build(), ous)
            listEntities()

            val camData: MutableMap<Int, ByteString?> = mutableMapOf()

            while (true) {
                if (ins.available() > 0) {
                    val msg = receiveMessage(ins)
                    when (msg) {
                        is Api.PingRequest -> sendMessage(
                            Api.PingResponse.newBuilder().build(), ous
                        )
                        is Api.CameraImageResponse -> {
                            Log.v(
                                TAG,
                                "Image key=${msg.key} done=${msg.done} data.len=${msg.data.size()} "
                            )
                            if (camData[msg.key] != null)
                                camData[msg.key] = camData[msg.key]!!.concat(msg.data)
                            else
                                camData[msg.key] = msg.data
                            if (msg.done and (camData[msg.key] != null)) {
                                onImage?.invoke(camData[msg.key]!!)
                                camData[msg.key] = null
                            }
                        }
                        is Api.DeviceInfoResponse -> onLog?.invoke("Info: " + msg.compilationTime)
                        is Api.LightStateResponse -> Log.v(
                            TAG,
                            "LightStateResponse key=${msg.key} brightness=${msg.brightness}"
                        )
                        is Api.ListEntitiesCameraResponse -> {
                            Log.v(
                                TAG,
                                "ListEntitiesCameraResponse key=${msg.key} name=${msg.name} uniqueId=${msg.uniqueId}"
                            )
                            entitiesCamera.put(msg.key, msg)
                        }
                        is Api.ListEntitiesLightResponse -> Log.v(
                            TAG,
                            "ListEntitiesLightResponse key=${msg.key} name=${msg.name} uniqueId=${msg.uniqueId}"
                        )
                        is Api.ListEntitiesServicesResponse -> {
                            Log.v(
                                TAG,
                                "ListEntitiesServicesResponse key=${msg.key} name=${msg.name} ${msg.argsList}"
                            )
                            entitiesServices.put(msg.key, msg)
                        }

                        is Api.SubscribeLogsResponse -> Log.v(
                            TAG,
                            "SubscribeLogsResponse tag=${msg.tag} ${msg.message}"
                        )

                    }
                }
                val txmsg = msgQueue.poll()
                if (txmsg != null)
                    sendMessage(txmsg, ous)
                if (stop.get())
                    break
                Thread.yield()
            }
            client.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
