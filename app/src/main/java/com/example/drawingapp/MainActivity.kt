package com.example.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    // Created a variable of the class DrawingView

    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // resultCode == RESULT_OK checks if we get the data back
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackGround: ImageView = findViewById(R.id.iv_background)
                imageBackGround.setImageURI(result.data?.data)
                /* result.data will give the URI (location path) of the resultant image that
                we have selected and result.data?.data will give us the image.
                We are not copying the image from the device into the application, rather
                the image is set based on where it is located on the device*/

                drawingView?.setBackgroundColor(0)
            }
        }

    /** create an ActivityResultLauncher with MultiplePermissions since we are requesting
     * both read and write
     */
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value    // Boolean value

                if (isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission Granted. Now you can read the storage files",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Intent for going to gallery if gallery permission is given
                    // URI is the path of the image that we select
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this@MainActivity,
                            "Oops, you just denied the permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this@MainActivity,
                            "Oops, you just denied the permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(5.toFloat())
        drawingView?.setColor("#FFFFFFFF")

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        // Stores the components of linear layout into an array

        mImageButtonCurrentPaint = linearLayoutPaintColors[0] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    // Gets the Frame layout
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    /* Since we have to pass a view for getting the Bitmap,
                     and as the frame layout contains the background image and the
                     drawing view, we can pass the flDrawingView to the function
                     And since the saveBitmapFile() method takes a sandwiched bitmap as a parameter
                     we can pass getBitmapFromView parameter into it */
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }

    }

    /**
     * Method is used to launch the dialog to select different brush sizes.
     */
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        // Created a Dialog for brush thickness

        brushDialog.setTitle("Brush Size: ")

        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
            // Dialog disappears after selection of brush size
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(15.toFloat())
            brushDialog.dismiss()
        }

        val veryLargeBtn: ImageButton = brushDialog.findViewById(R.id.ib_very_large_brush)
        veryLargeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    /**
     * Method is called when color is clicked from pallet_normal.
     *
     * @param view ImageButton on which click took place.
     */
    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {   // Checks if we clicked on a different paint
            val imageButton = view as ImageButton  //Typecasts the view to an ImageButton
            val colorTag = imageButton.tag.toString()
            // The tag of the color kept in imageButton (ex: #000000) is
            // converted into a string and stored

            drawingView?.setColor(colorTag)
            // Passes the color to the setColor function, which will set the color of drawing

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
                //  Changes the border of selected paint to highlight it
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                // Changes the border of previously selected paint to normal
            )

            //Current view is updated with selected view in the form of ImageButton.
            mImageButtonCurrentPaint = view
        }
    }

    /**
     * We are calling this method to check the permission status
     */
    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether you have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }

    //create a method to requestStorage permission
    private fun requestStoragePermission() {
        // Check if the permission was denied and show rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog(
                "Drawing App",
                "Drawing App needs access to External Storage for setting background image"
            )
        } else {
            // You can directly ask for the permission.
            // if it has not been denied then request for permission
            // The registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    /**  create rationale dialog
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    /**
     * Create bitmap from view and returns it
     */
    private fun getBitmapFromView(view: View): Bitmap {
        // The bitmap is the background image, the view which contains our canvas - where we can paint on
        // So, the bitmap is basically an image sandwich made of these components

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background

        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw black background on the canvas
            canvas.drawColor(Color.BLACK)
        }

        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    // Created a Byte Array Output Stream to output our image
                    // Buffer capacity is initially 32 bytes, but can be increased if necessary
                    val bytes = ByteArrayOutputStream()

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    /**
                     * Write a compressed version of the bitmap to the specified outputstream.
                     * If this returns true, the bitmap can be reconstructed by passing a
                     * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
                     * all Formats support all bitmap configs directly, so it is possible that
                     * the returned bitmap from BitmapFactory could be in a different bitdepth,
                     * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
                     * pixels).
                     *
                     * @param format   The format of the compressed image
                     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
                     *                 small size, 100 meaning compress for max quality. Some
                     *                 formats, like PNG which is lossless, will ignore the
                     *                 quality setting
                     * @param stream   The outputstream to write the compressed data.
                     * @return true if successfully compressed to the specified stream.
                     */

                    // f is file in which the png format compressed bitmap is stored
                    // here externalCacheDir?.absoluteFile.toString() is the file path
                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "DrawingApp_"
                                + System.currentTimeMillis() / 1000 + ".png"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.

                    val fo = FileOutputStream(f)
                    // Creates a file output stream to write to the file represented by the specified object.
                    fo.write(bytes.toByteArray())
                    // Writes bytes from the specified byte array to this file output stream.
                    fo.close()
                    /* Closes this file output stream and releases any system resources associated
                     with this stream. This file output stream may no longer be used for writing bytes. */

                    result = f.absolutePath // The file absolute path is return as a result.

                    //We switch from io to ui thread to show a toast
                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File Saved Successfully: $result",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the data.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    // Progress dialog to run while the image is being saved
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /* Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progess)

        customProgressDialog?.setCancelable(false)
        // Start the dialog and view it on the screen
        customProgressDialog?.show()
    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
}
