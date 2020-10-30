package dev.nauber.esphomerccar

import java.io.InputStream
import java.io.OutputStream


fun readVarInt(ins:InputStream): Int {
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

fun writeVarInt(value: Int, ous:OutputStream) {
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
