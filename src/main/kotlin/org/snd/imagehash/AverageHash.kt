package org.snd.imagehash

import com.twelvemonkeys.image.ResampleOp
import java.awt.Color
import java.awt.image.BufferedImage
import java.math.BigInteger

// adapted from https://github.com/KilianB/JImageHash/blob/c41bd3daca951e9397dff10a6143cfa981069cab/src/main/java/dev/brachtendorf/jimagehash/hashAlgorithms/AverageHash.java
// Calculate a hash value based on the average luminosity in an image.
object AverageHash {
    private const val width = 32
    private const val height = 32
    private const val bitResolution = width * height

    fun hash(source: BufferedImage): BigInteger {
        val resampler = ResampleOp(width, height, ResampleOp.FILTER_LANCZOS)
        val resampled = resampler.filter(source, null)

        val luma = getLuma(resampled)
        val avgPixelValue = luma.map { it.average() }.average()

        val hash = HashBuilder(bitResolution)
        luma.asSequence().flatMap { it.asSequence() }
            .forEach { value ->
                if (value < avgPixelValue) hash.prependZero()
                else hash.prependOne()
            }

        return hash.toBigInteger()
    }

    fun normalizedHammingDistance(hash1: BigInteger, hash2: BigInteger): Double {
        return hammingDistance(hash1, hash2) / bitResolution.toDouble()
    }

    private fun hammingDistance(hash1: BigInteger, hash2: BigInteger): Int {
        return (hash1.xor(hash2)).bitCount()
    }

    private fun getLuma(image: BufferedImage): Array<IntArray> {
        val luma = Array(image.width) { IntArray(image.height) }
        for (y in 0..<image.height) {
            for (x in 0..<image.width) {
                val color = Color(image.getRGB(x, y))
                val lum = color.red * 0.299 + color.green * 0.587 + color.blue * 0.114
                luma[x][y] = if (lum > 255) 255 else lum.toInt()
            }
        }
        return luma
    }
}