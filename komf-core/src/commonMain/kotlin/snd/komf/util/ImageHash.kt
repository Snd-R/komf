package snd.komf.util

import com.twelvemonkeys.image.ResampleOp
import java.awt.Color
import java.awt.image.BufferedImage
import java.math.BigInteger
import javax.imageio.ImageIO
import kotlin.experimental.or
import kotlin.math.ceil

private const val similarityThreshold = 0.1

fun compareImages(image1: ByteArray, image2: ByteArray): Boolean {
    val decoded1 = ImageIO.read(image1.inputStream())
    val decoded2 = ImageIO.read(image2.inputStream())
    if (decoded1 == null || decoded2 == null) return false

    val hash1 = hash(decoded1)
    val hash2 = hash(decoded2)

    val similarityScore = normalizedHammingDistance(hash1, hash2)
    return similarityScore <= similarityThreshold
}

private const val width = 32
private const val height = 32
private const val bitResolution = width * height

// adapted from https://github.com/KilianB/JImageHash/blob/c41bd3daca951e9397dff10a6143cfa981069cab/src/main/java/dev/brachtendorf/jimagehash/hashAlgorithms/AverageHash.java
fun hash(source: BufferedImage): BigInteger {
    val resampled = ResampleOp(width, height, ResampleOp.FILTER_BOX).filter(source, null)

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

// adapted from https://github.com/KilianB/JImageHash/blob/c41bd3daca951e9397dff10a6143cfa981069cab/src/main/java/dev/brachtendorf/jimagehash/hashAlgorithms/HashBuilder.java
private class HashBuilder(bits: Int) {
    private var bytes: ByteArray
    private var arrayIndex = 0
    private var bitIndex = 0
    private var length = 0

    init {
        bytes = ByteArray(ceil(bits / 8.0).toInt())
        arrayIndex = bytes.size - 1
    }

    companion object {
        private val MASK = byteArrayOf(
            1,
            (1 shl 1).toByte(),
            (1 shl 2).toByte(),
            (1 shl 3).toByte(),
            (1 shl 4).toByte(),
            (1 shl 5).toByte(),
            (1 shl 6).toByte(),
            (1 shl 7).toByte()
        )
    }

    fun prependZero() {
        if (bitIndex == 8) endByte()

        bitIndex++
        length++
    }

    fun prependOne() {
        if (bitIndex == 8) endByte()

        bytes[arrayIndex] = bytes[arrayIndex] or MASK[bitIndex]
        bitIndex++
        length++
    }

    private fun endByte() {
        bitIndex = 0
        arrayIndex--
        if (arrayIndex == -1) {
            val temp = ByteArray(bytes.size + 1)
            bytes.copyInto(temp, 1, 0)
            bytes = temp
            arrayIndex = 0
        }
    }

    fun toBigInteger() = BigInteger(1, bytes)
}
