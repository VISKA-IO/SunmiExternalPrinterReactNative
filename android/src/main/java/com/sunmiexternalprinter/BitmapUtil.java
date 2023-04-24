////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by FernFlower decompiler)
////
//
//package com.sunmiexternalprinter;
//
//import android.graphics.Bitmap;
//import android.graphics.Color;
//
//public class BitmapUtil {
//  public BitmapUtil() {
//  }
//
//  public static byte[] getBytesFromBitmap(Bitmap bitmap, boolean isDithering) {
//    int width = bitmap.getWidth();
//    int height = bitmap.getHeight();
//    int bw = (width - 1) / 8 + 1;
//    byte[] rv = new byte[height * bw + 4];
//    rv[0] = (byte)bw;
//    rv[1] = (byte)(bw >> 8);
//    rv[2] = (byte)height;
//    rv[3] = (byte)(height >> 8);
//    int[] pixels = new int[width * height];
//    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
//    int e;
//    int i;
//    int j;
//    int g;
//    int r;
//    if (isDithering) {
//      int[] gray = new int[height * width];
//
//      for(e = 0; e < height; ++e) {
//        for(i = 0; i < width; ++i) {
//          j = pixels[width * e + i];
//          g = Color.alpha(j);
//          if (g == 0) {
//            j = -1;
//          }
//
//          r = Color.red(j);
//          int g = Color.green(j);
//          int b = Color.blue(j);
//          int red = r * 19595 + g * '际' + b * 7472 >> 16;
//          gray[width * e + i] = red;
//        }
//      }
//
//      for(i = 0; i < height; ++i) {
//        for(j = 0; j < width; ++j) {
//          g = gray[width * i + j];
//          if (g >= 128) {
//            pixels[width * i + j] = -1;
//            e = g - 255;
//          } else {
//            pixels[width * i + j] = -16777216;
//            e = g;
//          }
//
//          if (j < width - 1 && i < height - 1) {
//            gray[width * i + j + 1] += 3 * e / 8;
//            gray[width * (i + 1) + j] += 3 * e / 8;
//            gray[width * (i + 1) + j + 1] += e / 4;
//          } else if (j == width - 1 && i < height - 1) {
//            gray[width * (i + 1) + j] += 3 * e / 8;
//          } else if (j < width - 1 && i == height - 1) {
//            gray[width * i + j + 1] += e / 4;
//          }
//        }
//      }
//    }
//
//    for(int i = 0; i < height; ++i) {
//      for(e = 0; e < width; ++e) {
//        i = pixels[width * i + e];
//        j = (i & 16711680) >> 16;
//        g = (i & '\uff00') >> 8;
//        r = i & 255;
//        byte gray = RGB2Gray(j, g, r);
//        rv[bw * i + e / 8 + 4] = (byte)(rv[bw * i + e / 8 + 4] | gray << 7 - e % 8);
//      }
//    }
//
//    return rv;
//  }
//
//  private static byte RGB2Gray(int r, int g, int b) {
//    return (byte)((int)(0.299D * (double)r + 0.587D * (double)g + 0.114D * (double)b) < 200 ? 1 : 0);
//  }
//}
