package org.snd.imagehash

import java.math.BigInteger
import kotlin.experimental.or
import kotlin.math.ceil

/**
 * Helper class to quickly create a bitwise byte array representation which can
 * be converted to a big integer object.
 *
 * <p>
 * To maintain the capability to decode the created hash value back to an image
 * the order of the byte array is of utmost importance. Due to hashes ability to
 * contain non 8 compliment bit values and {@link BigInteger}
 * stripping leading zero bits the partial bit value has to be present at the 0
 * slot.
 * <p>
 * The hashbuilder systematically grows the base byte[] array as needed but
 * performs the best if the correct amount of bits are known beforehand.
 *
 * <p>
 * In other terms this class performs the same operation as
 *
 * <pre>
 * <code>
 * 	StringBuilder sb = new StringBuilder();
 * 	sb.append("100011");
 * 	BigInteger b = new BigInteger(sb.toString(),2);
 * </code>
 * </pre>
 *
 * But scales much much better for higher hash values. The order of the bits are
 * flipped using the hashbuilder approach.
 */
// adapted from https://github.com/KilianB/JImageHash/blob/c41bd3daca951e9397dff10a6143cfa981069cab/src/main/java/dev/brachtendorf/jimagehash/hashAlgorithms/HashBuilder.java
class HashBuilder(bits: Int) {
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

    /**
     * Add a zero bit to the hash
     */
    fun prependZero() {
        if (bitIndex == 8) endByte()

        bitIndex++
        length++
    }

    /**
     * Add a one bit to the hash
     */
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
            System.arraycopy(bytes, 0, temp, 1, bytes.size)
            bytes = temp
            arrayIndex = 0
        }
    }

    /**
     * Convert the internal state of the hashbuilder to a big integer object
     *
     * @return a big integer object
     */
    fun toBigInteger() = BigInteger(1, bytes)
}