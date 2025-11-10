package com.sunmiexternalprinter
import android.graphics.Bitmap
import com.github.anastaciocintra.escpos.image.CoffeeImage

/**
 * Android implementation of the CoffeeImage interface using Android Bitmap.
 * 
 * This class provides a bridge between Android's native Bitmap class and the
 * ESC/POS library's CoffeeImage interface. It enables the use of Android Bitmap
 * objects directly with the ESC/POS image processing and printing functionality.
 * 
 * The implementation wraps Android Bitmap operations to match the CoffeeImage
 * interface requirements, providing methods for dimension access, sub-image
 * extraction, and pixel data retrieval.
 * 
 * @param bitmap The Android Bitmap to wrap with CoffeeImage interface
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 * @see CoffeeImage
 * @see Bitmap
 */
class CoffeeImageAndroidImpl(private val bitmap: Bitmap) : CoffeeImage {
  /**
   * Returns the width of the bitmap in pixels.
   * @return bitmap width in pixels
   */
  override fun getWidth(): Int {
    return bitmap.width
  }

  /**
   * Returns the height of the bitmap in pixels.
   * @return bitmap height in pixels
   */
  override fun getHeight(): Int {
    return bitmap.height
  }

  /**
   * Creates a sub-image from the specified rectangular region.
   * 
   * @param x The x-coordinate of the upper-left corner of the sub-image
   * @param y The y-coordinate of the upper-left corner of the sub-image
   * @param w The width of the sub-image
   * @param h The height of the sub-image
   * @return A new CoffeeImageAndroidImpl containing the specified sub-region
   */
  override fun getSubimage(x: Int, y: Int, w: Int, h: Int): CoffeeImage {
    return CoffeeImageAndroidImpl(Bitmap.createBitmap(bitmap, x, y, w, h))
  }

  /**
   * Returns the RGB color value of the pixel at the specified coordinates.
   * 
   * @param x The x-coordinate of the pixel
   * @param y The y-coordinate of the pixel
   * @return The RGB color value as an integer
   */
  override fun getRGB(x: Int, y: Int): Int {
    return bitmap.getPixel(x, y)
  }
}
