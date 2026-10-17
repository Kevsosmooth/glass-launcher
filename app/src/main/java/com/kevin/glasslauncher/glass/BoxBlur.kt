package com.kevin.glasslauncher.glass

/** Separable box blur on ARGB pixels; three passes approximate a Gaussian. Pure function. */
object BoxBlur {
    fun blur(pixels: IntArray, width: Int, height: Int, radius: Int, passes: Int = 3): IntArray {
        require(pixels.size == width * height) { "pixels.size != width * height" }
        val src = pixels.copyOf()
        val tmp = IntArray(src.size)
        repeat(passes) {
            pass(src, tmp, width, height, radius, horizontal = true)
            pass(tmp, src, width, height, radius, horizontal = false)
        }
        return src
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val n = 2 * r + 1
        for (line in 0 until lines) {
            fun at(i: Int): Int {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) line * w + c else c * w + line
            }
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (k in -r..r) {
                val p = src[at(k)]
                a += p ushr 24; rr += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (i in 0 until len) {
                dst[at(i)] = ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (b / n)
                val out = src[at(i - r)]
                val inn = src[at(i + r + 1)]
                a += (inn ushr 24) - (out ushr 24)
                rr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
                g += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
                b += (inn and 0xFF) - (out and 0xFF)
            }
        }
    }
}
