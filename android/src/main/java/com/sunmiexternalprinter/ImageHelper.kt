package com.sunmiexternalprinter

import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.image.Bitonal
import com.github.anastaciocintra.escpos.image.CoffeeImage
import com.github.anastaciocintra.escpos.image.EscPosImage
import com.github.anastaciocintra.escpos.image.ImageWrapperInterface
import java.io.IOException


class ImageHelper (maxWidth:Int, maxHeight:Int){
  private var maxWidth = maxWidth
  private var maxHeight = maxHeight

  init {
    var maxHeight = maxHeight
    if (maxHeight < 24) maxHeight = 24
    if (maxHeight % 24 != 0) maxHeight -= maxHeight % 24
    this.maxWidth = maxWidth
    this.maxHeight = maxHeight
  }
  constructor() : this(576,24)

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
   * just slice the image and print sequentially
   * Slice the image per height and print sequentially
   * with regular escpos write image
   * @param escPos
   * @param image
   * @param wrapper
   * @param bitonalAlgorithm
   * @throws IOException
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
