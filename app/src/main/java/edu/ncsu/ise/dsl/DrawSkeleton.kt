package edu.ncsu.ise.dsl

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.PoseLandmark

class DrawSkeleton(context: Context?, attributeSet: AttributeSet?): View(context, attributeSet) {
    companion object {
        val xmul = 3.7f // 3.3f
        val ymul = 3.7f // 3.43f
        val xOffset = -350 // - for left, + for right
        val yOffset = -50 // - for up, + for down
    }

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private val paintLine = Paint().apply {
        color = Color.YELLOW
        isAntiAlias = true
//        style = Paint.Style.STROKE // will change nothing
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 15f
    }

    private val paintOuterCircle = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
    }

    private val paintInnerCircle = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
//        extraCanvas.drawColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) { // View 객체를 상속받는 클래스 DrawSkeleton는 onDraw를 오버라이딩해야..
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    fun clear() {
        extraCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun drawLine(startLandmark: PoseLandmark, endLandmark: PoseLandmark) {
        val start = startLandmark.position
        val end = endLandmark.position

        extraCanvas.drawLine(
            start.x * xmul + xOffset, start.y * ymul + yOffset, end.x * xmul + xOffset, end.y * ymul + yOffset, paintLine
        )
    }

    fun drawCircle(landmark: PoseLandmark){
        val landmark = landmark.position

        extraCanvas.drawCircle(landmark.x * xmul + xOffset, landmark.y * ymul + yOffset, 15f, paintOuterCircle)
        extraCanvas.drawCircle(landmark.x * xmul + xOffset, landmark.y * ymul + yOffset, 10f, paintInnerCircle)
    }

}