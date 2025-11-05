package com.ble.kyv.ble

class DataParser(private val parseListener: ParseListener) {
    private var pulseOximeterList: MutableList<Pair<Int, Int>>? = null
    private var mgdl: Float = 0f

    fun readBM1000C(array: ByteArray, mac: String) {
        val data = IntArray(5)
        array.forEachIndexed { index, byte ->
            data[index] = toUnsignedInt(byte)
        }
        val spo2 = data[4]
        val pulseRate = data[3] or (data[2] and 0x40 shl 1)
        val pi = data[0] and 0x0f
        if (spo2 != 127 && pulseRate != 255) {
            parseListener.onBM1000CRead(spo2, pulseRate, mac)
            if (pulseOximeterList != null) {
                if (pulseOximeterList!!.size < 10) {
                    pulseOximeterList?.add(Pair(spo2, pulseRate))
                } else {
                    val resultSPO2 = pulseOximeterList!!.sumBy { it.first } / 10
                    val resultPulseRate = pulseOximeterList!!.sumBy { it.second } / 10
                    parseListener.onBM1000CFinish(resultSPO2, resultPulseRate, mac)
                    pulseOximeterList = null
                }
            } else {
                pulseOximeterList = mutableListOf(Pair(spo2, pulseRate))
            }
        }
    }

    fun readAD805(array: ByteArray, mac: String) {
        val data = IntArray(5)
        array.forEachIndexed { index, byte ->
            data[index] = toUnsignedInt(byte)
        }
        val spo2 = data[4]
        val pulseRate = data[3] or (data[2] and 0x40 shl 1)
        val pi = data[0] and 0x0f
        if (spo2 != 44 && pulseRate != 44) {
            parseListener.onAD805Read(spo2, pulseRate, mac)
            if (pulseOximeterList != null) {
                if (pulseOximeterList!!.size < 10) {
                    pulseOximeterList?.add(Pair(spo2, pulseRate))
                } else {
                    val resultSPO2 = pulseOximeterList!!.sumBy { it.first } / 10
                    val resultPulseRate = pulseOximeterList!!.sumBy { it.second } / 10
                    parseListener.onAD805Finish(resultSPO2, resultPulseRate, mac)
                    pulseOximeterList = null
                }
            } else {
                pulseOximeterList = mutableListOf(Pair(spo2, pulseRate))
            }
        }
    }

    fun readCF516(array: ByteArray, mac: String) {
        val kg = (toUnsignedInt(array[3]) + (toUnsignedInt(array[4]) * 256)).toDouble() / 100.0
        val lb = kg * 2.20462

        if (array[9] == 1.toByte()) {
            parseListener.onCF516Read(lb, mac)
        } else {
            parseListener.onCF516Finish(lb, mac)
        }
    }

    fun readLD575(array: ByteArray, mac: String) {
        val unsignedArray = array.map { toUnsignedInt(it) }
        if (unsignedArray.size < 8) return
        println("readLD575 = ${unsignedArray[2]}")
        when (unsignedArray[3]) {
            73 -> parseListener.onU807Finish(unsignedArray[6] + 30, unsignedArray[7] + 30, unsignedArray[5], mac)
            10 -> parseListener.onU807Read(unsignedArray[7], unsignedArray[6], mac)
        }
    }

    fun clearBM1000CBuffer() {
        pulseOximeterList = null
    }

    fun readU807(array: ByteArray, mac: String) {
        val unsignedArray = array.map { toUnsignedInt(it) }
        println("readU807 = ${unsignedArray[2]}")
        when (unsignedArray[2]) {
            252 -> parseListener.onU807Finish(unsignedArray[3], unsignedArray[4], unsignedArray[5], mac)
            251 -> parseListener.onU807Read(unsignedArray[3], unsignedArray[4], mac)
        }
    }

    fun readContour(array: ByteArray, mac: String) {
        mgdl =  toUnsignedInt(array[12]).toFloat()
        parseListener.onGlucoseFinish(mgdl, mac)
    }

    fun repeatContour(mac: String) {
        parseListener.onGlucoseFinish(mgdl, mac)
        mgdl = 0f
    }

    private fun toUnsignedInt(x: Byte): Int {
        return x.toInt() and 0xff
    }

    interface ParseListener {
        fun onCF516Read(weight: Double, mac: String)
        fun onCF516Finish(weight: Double, mac: String)
        fun onAD805Read(spo2: Int, pulseRate: Int, mac: String)
        fun onAD805Finish(spo2: Int, pulseRate: Int, mac: String)
        fun onBM1000CRead(spo2: Int, pulseRate: Int, mac: String)
        fun onBM1000CFinish(spo2: Int, pulseRate: Int, mac: String)
        fun onU807Read(pressureH: Int, pressureL: Int, mac: String)
        fun onU807Finish(sys: Int, dia: Int, pul: Int, mac: String)
        fun onGlucoseFinish(mgdl: Float, mac: String)
    }
}