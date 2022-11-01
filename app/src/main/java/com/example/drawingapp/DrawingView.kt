package com.example.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet.Motion

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs){
    // Created a class DrawingView of which inherits from class View() to provide a
    // view on which we can draw on

    private var mDrawPath: CustomPath? = null

    private var mCanvasBitmap: Bitmap? = null
    /*
    Bitmap (BMP) is an image file format that can be used to create and store computer graphics.
    A bitmap file displays a small dots in a pattern that, when viewed from afar, creates an
    overall image. A bitmap image is a grid made of rows and columns where a specific cell is
    given a value that fills it in or leaves it blank, thus creating an image out of the data.

    To create a bitmap, an image is broken into the smallest possible units (pixels) and then the
    color information of each pixel (color depth) is stored in bits that are mapped out in rows
    and columns. The complexity of a bitmap image can be increased by varying the color intensity
    of each dot or by increasing the number of rows and columns used to create the image. However,
    when a user magnifies a bitmap image enough, it eventually becomes pixelated as the dots
    resolve into tiny squares of color on a grid
     */

    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null

    private val mPaths = ArrayList<CustomPath>()
    // ArrayList to store the paths that we draw on the screen

    // Code inside the init block is the first to be executed when a class is instantiated
    init {
        setUpDrawing()
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint()
        // Created a new paint with default settings

        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        // Set the color of paint that we will be drawing with

        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        // Outer edges of a join meet in a circular arc

        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        // Cap is the setting for stroke projection. Round projects the stroke
        // in a semicircle with centre from the point at which the path ends
        // Basically, when we stop drawing, the the end of stroke will be rounded

        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        // DITHER_FLAG enables dithering when blitting

        /*
         dithering -> a term used to describe the strategic application of noise to an image.
         It has traditionally been used to improve the appearance of images where the output is
         limited to a particular color range. For example, a 1-bit image is monochrome and capable
         of only using a palette of two colors: black and white. Dithering can be employed to
         create the appearance of multiple shades by varying the distance between dots

         blitting -> To "blit" is to copy bits from one part of a computer's graphical memory to
         another part. This technique deals directly with the pixels of an image, and draws them
         directly to the screen, which makes it a very fast rendering technique that's often
         perfect for fast-paced 2D action games
         */

        // mBrushSize = 20.toFloat()    -> Initial brush / stroke size declared, but is not useful
        //                                 anymore because we scale the brush size wrt screen size
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // This function is called when the screen is inflated, or in this case,
        // when this drawing view is called
        super.onSizeChanged(w, h, oldw, oldh)

        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // Each pixel is stored on 4 bytes. Each channel (RGB and alpha for translucency)
        // is stored with 8 bits of precision (256 possible values.)
        // This configuration is very flexible and offers the best quality.

        canvas = Canvas(mCanvasBitmap!!)    // sets the canvas with the bitmap we created
        // !! used because mCanvasBitmap is a nullable and since we have given it a non
        // null value above, we can convert mCanvasBitmap into a non null type and use it
    }

    // This function is called when we want to draw something
    override fun onDraw(canvas: Canvas?){
        super.onDraw(canvas)

        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        // Draw bitmap on canvas from top left using mCanvasPaint - used safe call

        for (path in mPaths){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas?.drawPath(path, mDrawPaint!!)
            // To redraw all the paths that were drawn before and stored in mPaths
        }

        if (!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            // Sets the stroke width of paint to the brush thickness of the path
            // Basically says how thick the paint should be
            mDrawPaint!!.color = mDrawPath!!.color
            // Sets the color of PAINT to color of path

            canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    // We still don't know when to draw, so we need to fill mDrawPath with a path
    // that is to be drawn
    // This function will be called when the view is touched - for drawing
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y
        // X and Y coordinates of the point on the view at which we touched

        when (event?.action){
            // When we press down on the screen - lambda expression used
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize
                // Sets the color and thickness of the PATH

                mDrawPath!!.reset()
                // resets the path drawn - basically erases the path on the screen
                if (touchX != null && touchY != null) {
                    mDrawPath!!.moveTo(touchX, touchY)
                    mDrawPath!!.lineTo(touchX, touchY)
                }
            }

            // When we move our finger
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null && touchY != null)
                    mDrawPath!!.lineTo(touchX, touchY)
                // Draws line to X and Y coordinate of touch
            }

            // After we remove finger from screen
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)
                // Stored the drawn path
                mDrawPath = CustomPath(color, mBrushSize)
            }

            else -> return false
        }

        invalidate()
        //Invalidate the whole view. If the view is visible, onDraw(Canvas) will be
        // called at some point in the future.
        // invalidate() generally means 'redraw on screen' and results to a call
        // of the view's onDraw() method

        return true
    }

    fun setSizeForBrush(newSize: Float){
        // This scales the brush size to display - bigger screens will by default have
        // thicker brush sizes and smaller screens will have thinner ones so that the look
        // of the app is uniform

        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        newSize, resources.displayMetrics)
        // DIP -> Device Independent Pixels, so TypedValue is independent of device pixels
        // displayMetrics is a structure describing general information about a display
        // such as its size, density, and font scaling

        mDrawPaint!!.strokeWidth = mBrushSize
    }

    // internal -> visible in the same module - in this case, visible only in DrawingView.kt
    // inner -> nested class can access members of outer class
    internal inner class CustomPath (var color: Int, var brushThickness: Float): Path(){

    }
}


