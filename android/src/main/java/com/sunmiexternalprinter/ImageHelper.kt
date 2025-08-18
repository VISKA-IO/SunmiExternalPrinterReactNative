package com.sunmiexternalprinter

import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.image.Bitonal
import com.github.anastaciocintra.escpos.image.CoffeeImage
import com.github.anastaciocintra.escpos.image.EscPosImage
import com.github.anastaciocintra.escpos.image.ImageWrapperInterface
import java.io.IOException

/**
 * Utility class for processing and printing large images on thermal printers.
 * 
 * Thermal printers have limitations on the maximum image dimensions they can
 * process in a single operation. This class handles the slicing of large images
 * into smaller segments that can be printed sequentially, ensuring compatibility
 * with various printer models and image sizes.
 * 
 * The class automatically adjusts image dimensions to be compatible with
 * thermal printer requirements (height must be multiples of 24 pixels) and
 * provides methods for both slicing and printing operations.
 * 
 * @param maxWidth Maximum width for image segments
 * @param maxHeight Maximum height for image segments (adjusted to multiples of 24)
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class ImageHelper (maxWidth:Int, maxHeight:Int){
  /** Maximum width for image segments */
  private var maxWidth = maxWidth
  
  /** Maximum height for image segments (adjusted to multiples of 24) */
  private var maxHeight = maxHeight

  /**
   * Initializes the ImageHelper with dimension constraints.
   * 
   * Automatically adjusts maxHeight to ensure compatibility with thermal
   * printer requirements. Height must be at least 24 pixels and a multiple
   * of 24 for proper ESC/POS image processing.
   */
  init {
    var maxHeight = maxHeight
    if (maxHeight < 24) maxHeight = 24
    if (maxHeight % 24 != 0) maxHeight -= maxHeight % 24
    this.maxWidth = maxWidth
    this.maxHeight = maxHeight
  }

  /**
   * Slices a large image into smaller segments suitable for thermal printing.
   * 
   * This method divides an image into smaller pieces based on the maximum
   * dimensions specified during initialization. Each segment will be within
   * the printer's processing capabilities while maintaining image integrity.
   * 
   * @param coffeeImage The image to be sliced into smaller segments
   * @return List of image segments ready for sequential printing
   */
  fun sliceImage(coffeeImage: CoffeeImage): List<CoffeeImage> {
    val listImages: MutableList<CoffeeImage> = ArrayList()
    var x = 0
    var y = 0
    var x_offset = this.maxWidth
    var y_offset = this.maxHeight
    while (true) {
      // safety to not run in out of bound
      if (x > coffeeImage.width - 1) {
        x = coffeeImage.width - 1
      }
      if (x + x_offset > coffeeImage.width) {
        x_offset = coffeeImage.width - x
      }
      if (y >= coffeeImage.height - 1) {
        y = coffeeImage.height - 1
      }
      if (y + y_offset > coffeeImage.height) {
        y_offset = coffeeImage.height - y
      }
      val tmp = coffeeImage.getSubimage(0, y, x_offset, y_offset)
      listImages.add(tmp)
      y += y_offset
      if (y >= coffeeImage.height) break
    }
    return listImages
  }

  /**
   * Processes and prints an image by slicing it into segments and printing sequentially.
   * 
   * This method combines the slicing and printing operations into a single call.
   * It automatically handles large images by breaking them into printer-compatible
   * segments and sending each segment to the ESC/POS printer in sequence.
   * 
   * @param escPos ESC/POS printer instance for output
   * @param image Image to be processed and printed
   * @param wrapper Image wrapper defining the printing method (Raster, Bit, Graphics)
   * @param bitonalAlgorithm Algorithm for converting to black/white (BitonalThreshold, etc.)
   * @throws IOException if printing operations fail
   */
  @Throws(IOException::class)
  fun write(
    escPos: EscPos,
    image: CoffeeImage,
    wrapper: ImageWrapperInterface<*>?,
    bitonalAlgorithm: Bitonal?
  ) {
    val images = sliceImage(image)
    for (img in images) {
      escPos.write(wrapper, EscPosImage(img, bitonalAlgorithm))
    }
  }
}
