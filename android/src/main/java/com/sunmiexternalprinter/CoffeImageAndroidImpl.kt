package com.sunmiexternalprinter
import android.graphics.Bitmap
import com.github.anastaciocintra.escpos.image.CoffeeImage


/**
 * implements CoffeeImage using Java BufferedImage
 * @see CoffeeImage
 *
 * @see Bitmap
 */
class CoffeeImageAndroidImpl(private val bitmap: Bitmap) : CoffeeImage {
  override fun getWidth(): Int {
    return bitmap.width
  }

  override fun getHeight(): Int {
    return bitmap.height
  }

  override fun getSubimage(x: Int, y: Int, w: Int, h: Int): CoffeeImage {
    return CoffeeImageAndroidImpl(Bitmap.createBitmap(bitmap, x, y, w, h))
  }

  override fun getRGB(x: Int, y: Int): Int {
    return bitmap.getPixel(x, y)
  }
}
