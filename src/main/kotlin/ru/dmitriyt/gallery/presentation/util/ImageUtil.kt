package ru.dmitriyt.gallery.presentation.util

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

object ImageUtil {

    fun fixImageByExif(image: BufferedImage, imageInformation: ImageInformation): BufferedImage {
        val op = AffineTransformOp(
            getExifTransformation(imageInformation),
            AffineTransformOp.TYPE_BICUBIC,
        )
        var destinationImage = op.createCompatibleDestImage(
            image,
            if (image.type == BufferedImage.TYPE_BYTE_GRAY) image.colorModel else null
        )
        destinationImage = op.filter(image, destinationImage)
        return destinationImage
    }

    private fun getExifTransformation(info: ImageInformation): AffineTransform {
        val t = AffineTransform()
        when (info.orientation) {
            1 -> Unit
            2 -> {
                t.scale(-1.0, 1.0)
                t.translate(-info.width.toDouble(), 0.0)
            }
            3 -> {
                t.translate(info.width.toDouble(), info.height.toDouble())
                t.rotate(Math.PI)
            }
            4 -> {
                t.scale(1.0, -1.0)
                t.translate(0.0, -info.height.toDouble())
            }
            5 -> {
                t.rotate(-Math.PI / 2)
                t.scale(-1.0, 1.0)
            }
            6 -> {
                t.translate(info.height.toDouble(), 0.0)
                t.rotate(Math.PI / 2)
            }
            7 -> {
                t.scale(-1.0, 1.0)
                t.translate(-info.height.toDouble(), 0.0)
                t.translate(0.0, info.width.toDouble())
                t.rotate(3 * Math.PI / 2)
            }
            8 -> {
                t.translate(0.0, info.width.toDouble())
                t.rotate(3 * Math.PI / 2)
            }
        }
        return t
    }
}