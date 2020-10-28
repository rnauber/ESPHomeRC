package dev.nauber.esphomerccar


import java.net.Socket
import Api
import android.util.Log
import java.io.*


class Communication(val host: String, val port: Int) : Runnable {
    val TAG = "Communication"


    val t = Thread(this)
    lateinit var on_response: (String) -> Unit

    fun sendMessage(msg: Api.HelloRequest, ous: OutputStream) {
        val message_type = 1


        val encoded = msg.toByteArray()


        fun sendVarInt(value: Int) {
            var v = value
            if (value <= 0x7F) {
                ous.write(value)
                return
            }

            while (v > 0) {
                val temp = v and 0x7F
                v = v shr 7
                if (v > 0)
                    ous.write(temp or 0x80)
                else
                    ous.write(temp)
            }
        }

        ous.write(0)
        sendVarInt(encoded.size)
        sendVarInt(message_type)
        ous.write(encoded)
        ous.flush()
    }


    fun receiveMessage(ins: InputStream) :Api.HelloResponse{

        fun receiveVarInt(): Int {
            var v = 0
            while (true) {
                val raw = ins.read()
                v += raw
                if ((raw and 0x80) == 0)
                    break
                v = v shl 7
            }
            return v
        }

        if (ins.read() != 0x00){
            Log.e(TAG, "Invalid preamble")
            }

        val length = receiveVarInt()
        val msg_type = receiveVarInt()

        val raw_msg = ByteArray(length)
        ins.read(raw_msg)

        Log.e(TAG, "RX_MSG${msg_type} ${length}b $raw_msg")

        return Api.HelloResponse.parseFrom(raw_msg)
//
//        if msg_type not in MESSAGE_TYPE_TO_PROTO:
//        _LOGGER.debug("%s: Skipping message type %s",
//            self._params.address, msg_type)
//        return
//
//        msg = MESSAGE_TYPE_TO_PROTO[msg_type]()
//        try:
//            msg.ParseFromString(raw_msg)
//            except Exception as e:
//            raise APIConnectionError("Invalid protobuf message: {}".format(e))
//            _LOGGER.debug("%s: Got message of type %s: %s",
//                self._params.address, type(msg), msg)
//            for msg_handler in self._message_handlers[:]:
//            msg_handler(msg)
//            await self._handle_internal_messages(msg)

    }

    fun start(on_response: (String) -> Unit) {
        this.on_response = on_response
        t.start()
    }

    override fun run() {
        try {

            var client = Socket(host, port)
            val hello = Api.HelloRequest.newBuilder().setClientInfo("esphome rccar").build()

            val ins = client.getInputStream()
            val ous = client.getOutputStream()

            sendMessage(hello, ous)
            val res = receiveMessage(ins)

            val str =
                "ESPHome ${res.serverInfo}  API ${res.apiVersionMajor}:${res.apiVersionMinor}  "
            Log.v(TAG, str)
            this.on_response("Hi" + str)

//            for (b in ous.toByteArray()) {
//                val st = String.format("%02X", b)
//                Log.v(TAG, st)
//            }

            client.close()


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
