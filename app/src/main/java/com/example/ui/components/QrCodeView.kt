package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pure Kotlin QR Code generator (Byte mode, ECC Level L, Versions 1..6)
 * so APK download links render as real scannable QR codes with zero extra dependencies.
 */
object SimpleQrEncoder {

    fun encode(data: String): Array<BooleanArray> {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val version = when {
            bytes.size <= 17 -> 1
            bytes.size <= 32 -> 2
            bytes.size <= 53 -> 3
            bytes.size <= 78 -> 4
            bytes.size <= 106 -> 5
            else -> 6
        }
        val cappedBytes = if (bytes.size > 134) bytes.copyOf(134) else bytes
        val totalDataCodewords = when (version) {
            1 -> 19
            2 -> 34
            3 -> 55
            4 -> 80
            5 -> 108
            else -> 136
        }
        val eccCount = when (version) {
            1 -> 7
            2 -> 10
            3 -> 15
            4 -> 20
            5 -> 26
            else -> 36 // Version 6 uses 2 blocks of 18 -> handled below
        }

        // Build bitstream: mode (0100), length (8 bits), data, terminator, pad
        val bits = ArrayList<Int>(totalDataCodewords * 8)
        fun addBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        addBits(0b0100, 4) // Byte mode
        addBits(cappedBytes.size, 8)
        for (b in cappedBytes) {
            addBits(b.toInt() and 0xFF, 8)
        }
        val maxBits = totalDataCodewords * 8
        for (i in 0 until minOf(4, maxBits - bits.size)) bits.add(0)
        while (bits.size % 8 != 0) bits.add(0)
        var padToggle = true
        while (bits.size < maxBits) {
            addBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        val dataCodewords = IntArray(totalDataCodewords) { i ->
            var v = 0
            for (b in 0 until 8) v = (v shl 1) or bits[i * 8 + b]
            v
        }

        val allCodewords = if (version == 6) {
            // Version 6-L has 2 blocks of 68 data codewords, 18 ECC each
            val b1 = dataCodewords.copyOfRange(0, 68)
            val b2 = dataCodewords.copyOfRange(68, 136)
            val e1 = computeReedSolomon(b1, 18)
            val e2 = computeReedSolomon(b2, 18)
            val interleaved = ArrayList<Int>(172)
            for (i in 0 until 68) {
                interleaved.add(b1[i]); interleaved.add(b2[i])
            }
            for (i in 0 until 18) {
                interleaved.add(e1[i]); interleaved.add(e2[i])
            }
            interleaved.toIntArray()
        } else {
            val ecc = computeReedSolomon(dataCodewords, eccCount)
            dataCodewords + ecc
        }

        val size = version * 4 + 17
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, black: Boolean, func: Boolean = true) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = black
                if (func) isFunction[r][c] = true
            }
        }

        fun drawFinder(top: Int, left: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = top + r
                    val cc = left + c
                    if (rr !in 0 until size || cc !in 0 until size) continue
                    val black = (r in 0..6 && c in 0..6) &&
                        (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                    setModule(rr, cc, black)
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            setModule(6, i, i % 2 == 0)
            setModule(i, 6, i % 2 == 0)
        }

        // Alignment pattern for version >= 2
        if (version >= 2) {
            val pos = size - 7
            for (r in -2..2) {
                for (c in -2..2) {
                    val black = maxOf(kotlin.math.abs(r), kotlin.math.abs(c)) != 1
                    setModule(pos + r, pos + c, black)
                }
            }
        }

        // Reserve format info areas
        for (i in 0..8) {
            if (!isFunction[8][i]) setModule(8, i, false)
            if (!isFunction[i][8]) setModule(i, 8, false)
        }
        for (i in size - 8 until size) {
            setModule(8, i, false)
            setModule(i, 8, false)
        }
        setModule(size - 8, 8, true) // Dark module

        // Place data bits (zigzag)
        val totalBits = allCodewords.size * 8
        var bitIdx = 0
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right--
            for (vert in 0 until size) {
                val upward = ((right + 1) and 2) == 0
                val row = if (upward) size - 1 - vert else vert
                for (j in 0 until 2) {
                    val col = right - j
                    if (!isFunction[row][col]) {
                        val bit = if (bitIdx < totalBits) {
                            ((allCodewords[bitIdx / 8] ushr (7 - (bitIdx % 8))) and 1) == 1
                        } else {
                            false
                        }
                        // Apply mask pattern 0: (row + col) % 2 == 0
                        val masked = if ((row + col) % 2 == 0) !bit else bit
                        modules[row][col] = masked
                        bitIdx++
                    }
                }
            }
            right -= 2
        }

        // Write Format Info for ECC L (01) + Mask 0 (000) -> 15-bit BCH = 0x77C4
        val formatBits = 0b111011111000100
        for (i in 0 until 15) {
            val black = ((formatBits ushr i) and 1) == 1
            // Vertical
            when {
                i < 6 -> setModule(i, 8, black)
                i < 8 -> setModule(i + 1, 8, black)
                else -> setModule(size - 15 + i, 8, black)
            }
            // Horizontal
            when {
                i < 8 -> setModule(8, size - 1 - i, black)
                i < 9 -> setModule(8, 15 - i, black)
                else -> setModule(8, 14 - i, black)
            }
        }

        return modules
    }

    private fun computeReedSolomon(data: IntArray, eccCount: Int): IntArray {
        val poly = IntArray(eccCount + 1)
        poly[eccCount] = 1
        var alpha = 1
        for (i in 0 until eccCount) {
            for (j in 0 until eccCount) {
                poly[j] = gfMultiply(poly[j], alpha) xor poly[j + 1]
            }
            poly[eccCount] = gfMultiply(poly[eccCount], alpha)
            alpha = gfMultiply(alpha, 2)
        }
        val divisor = IntArray(eccCount) { poly[it + 1] }

        val rem = IntArray(eccCount)
        for (b in data) {
            val factor = b xor rem[0]
            System.arraycopy(rem, 1, rem, 0, eccCount - 1)
            rem[eccCount - 1] = 0
            for (i in 0 until eccCount) {
                rem[i] = rem[i] xor gfMultiply(divisor[i], factor)
            }
        }
        return rem
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }
}

@Composable
fun QrCodeView(
    content: String,
    size: Dp = 104.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(content) { SimpleQrEncoder.encode(content) }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val n = matrix.size
            val cellSize = this.size.width / n
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color(0xFF080C14),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
