package platform.test.screenshot

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.concurrent.futures.ResolvableFuture
import androidx.test.espresso.Espresso

/*
 * This file was forked from androidx/test/core/view/WindowCapture.kt.
 * TODO(b/195673633): Remove this fork and use the AndroidX version instead.
 */
fun Window.generateBitmapFromPixelCopy(
    boundsInWindow: Rect? = null,
    destBitmap: Bitmap,
    bitmapFuture: ResolvableFuture<Bitmap>
) {
    val isRobolectric = if (Build.FINGERPRINT.contains("robolectric")) true else false
    val onCopyFinished =
        PixelCopy.OnPixelCopyFinishedListener { result ->
            if (result == PixelCopy.SUCCESS) {
                bitmapFuture.set(destBitmap)
            } else {
                bitmapFuture.setException(
                    RuntimeException(String.format("PixelCopy failed: %d", result))
                )
            }
        }

    // We need to flush all the events in the UI queue before taking a screenshot.
    // This can be guaranteed by waiting for an onSuccess callback, as implemented by toBitmap, but
    // in robolectric mode,
    //  we have to use this sync call as the procssing is done on the main looper and therefore we
    // need a sync based solution.
    if (isRobolectric) {
        Espresso.onIdle()
    }

    PixelCopy.request(
        this,
        boundsInWindow,
        destBitmap,
        onCopyFinished,
        Handler(Looper.getMainLooper())
    )
}
