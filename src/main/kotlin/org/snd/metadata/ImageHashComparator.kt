package org.snd.metadata

import org.snd.imagehash.AverageHash
import java.awt.image.BufferedImage

object ImageHashComparator {
    private const val similarityThreshold = 0.1

    fun compareImages(image1: BufferedImage, image2: BufferedImage): Boolean {
        val hash1 = AverageHash.hash(image1)
        val hash2 = AverageHash.hash(image2)
        val similarityScore = AverageHash.normalizedHammingDistance(hash1, hash2)
        return similarityScore <= similarityThreshold
    }
}