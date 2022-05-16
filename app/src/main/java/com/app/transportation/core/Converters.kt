package com.app.transportation.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur


fun Context.dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
//fun Context.spToPx(sp: Int) = (sp * resources.displayMetrics.scaledDensity).toInt()

//fun Context.pxToDp(px: Int) = (px / resources.displayMetrics.density).toInt()
//fun Context.pxToSp(px: Int) = (px / resources.displayMetrics.scaledDensity).toInt()


fun decodeSampledBitmapFromResource(
    path: ByteArray?,
    reqWidth: Int, reqHeight: Int
): Bitmap? {
    // Читаем с inJustDecodeBounds=true для определения размеров
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(path, 0, path!!.size, options)

    // Вычисляем inSampleSize
    options.inSampleSize = calculateInSampleSize(
        options, reqWidth,
        reqHeight
    )

    // Читаем с использованием inSampleSize коэффициента
    options.inJustDecodeBounds = false
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    return BitmapFactory.decodeByteArray(path, 0, path!!.size, options)
}

fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int, reqHeight: Int
): Int {
    // Реальные размеры изображения
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        // Вычисляем наибольший inSampleSize, который будет кратным двум
        // и оставит полученные размеры больше, чем требуемые
        while (halfHeight / inSampleSize > reqHeight
            && halfWidth / inSampleSize > reqWidth
        ) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun blurRenderScript(context: Context?, smallBitmap: Bitmap, radius: Int): Bitmap? {
    var smallBitmap = smallBitmap
    try {
        smallBitmap = RGB565toARGB888(smallBitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val bitmap = Bitmap.createBitmap(
        smallBitmap.width, smallBitmap.height,
        Bitmap.Config.ARGB_8888
    )
    val renderScript = RenderScript.create(context)
    val blurInput = Allocation.createFromBitmap(renderScript, smallBitmap)
    val blurOutput = Allocation.createFromBitmap(renderScript, bitmap)
    val blur = ScriptIntrinsicBlur.create(
        renderScript,
        Element.U8_4(renderScript)
    )
    blur.setInput(blurInput)
    blur.setRadius(radius.toFloat()) // radius must be 0 < r <= 25
    blur.forEach(blurOutput)
    blurOutput.copyTo(bitmap)
    renderScript.destroy()
    return bitmap
}

@Throws(Exception::class)
private fun RGB565toARGB888(img: Bitmap): Bitmap {
    val numPixels = img.width * img.height
    val pixels = IntArray(numPixels)

    //Get JPEG pixels.  Each int is the color values for one pixel.
    img.getPixels(pixels, 0, img.width, 0, 0, img.width, img.height)

    //Create a Bitmap of the appropriate format.
    val result = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)

    //Set RGB pixels.
    result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
    return result
}