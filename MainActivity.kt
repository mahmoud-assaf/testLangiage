package com.mahmoud.kozbara

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.about_dialog.view.*
import kotlinx.android.synthetic.main.activity_main.*
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS =
    arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
var bitmap: Bitmap? = null
var module: Module? = null
var fabOpened=false
val tone=MediaActionSound()
var withSound=true
lateinit var context: Context
val sharedPreferences by lazy {
    PreferenceManager.getDefaultSharedPreferences(context)
}
class MainActivity : BaseActivity(), ResultDialog.ResultDialogListener,SettingsDialog.SettingsDialogListner {
    override fun onFinished() {
        bar?.visibility= View.INVISIBLE
    }

    var bar: ProgressBar? = null
    var titleActivity: TextView? = null
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context=this
        withSound=sharedPreferences.getBoolean("withSound",true)

        if (withSound) tone.load(MediaActionSound.SHUTTER_CLICK)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)
        //supportActionBar?.setIcon(R.drawable.toolbar_icon)
        bar = findViewById(R.id.progressBar) as ProgressBar
        titleActivity = toolbar.findViewById(R.id.title) as TextView
        supportActionBar?.setDisplayShowTitleEnabled(false)
        titleActivity!!.text = getString(R.string.app_name)
        titleActivity!!.typeface = TypeFaceManager.getInstance(this).getExoBold()

        mainfab.setOnClickListener {
            if (fabOpened)  closeFAB() else openFAB()
        }
        aboutfab.setOnClickListener {
            showAboutDialog()
        }

        settingsfab.setOnClickListener {
            showSettingsDialog()
        }

        helpfab.setOnClickListener {
            showHelpDialog()
        }
        viewFinder = findViewById(R.id.view_finder)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }



        try {
            // creating bitmap from packaged into app android asset 'image.jpg',
            // app/src/main/assets/image.jpg
            //val am = getAssets()
            //bitmap = BitmapFactory.decodeStream(am.open("a14.jpg"))

            //imageView.setImageBitmap(bitmap)
            // loading serialized torchscript module from packaged into app android asset model.pt,
            // app/src/model/assets/model.pt
            module = Module.load(fetchModelFile(this, "mobileModel.pth"))
        } catch (e: IOException) {
            Log.e("PytorchHelloWorld", "Error reading assets", e)
            finish()
        }


    }

    fun openFAB(){
        mainfab.animate()
            .rotation(180.0F)
            .withLayer()
            .setDuration(500L)
            .setInterpolator( OvershootInterpolator(10.0F))
            .start()

        aboutfab.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        settingsfab.animate().translationY(-getResources().getDimension(R.dimen.standard_155))
        helpfab.animate().translationY(-getResources().getDimension(R.dimen.standard_105))
        fabOpened=true

    }

    fun closeFAB(){
        aboutfab.animate().translationY(0f)
        settingsfab.animate().translationY(0f)
        helpfab.animate().translationY(0f)
        mainfab.animate()
            .rotation(0.0F)
            .withLayer()
            .setDuration(500L)
            .setInterpolator( OvershootInterpolator(10.0F))
            .start()
        fabOpened=false
    }
    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
          *//*  R.id.howTo -> {
                startActivity(Intent(this,TutorialActivity::class.java))
                true
            }*//*
            R.id.about -> {
                showAboutDialog()
                true
            }
            R.id.settings->{
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
*/
    private fun showSettingsDialog() {
        var fragmentSettings=SettingsDialog()

        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("fragmentSettings")
        if (prev != null)
        {
            ft.remove(prev)
        }

        ft.addToBackStack(null)
        fragmentSettings.show(ft, "fragmentSettings")
        closeFAB()
    }

    private fun setAppLocale(localeCode:String) {
        val resources = getResources()
        val dm = resources.getDisplayMetrics()
        val config = resources.getConfiguration()

            config.locale = Locale(localeCode.toLowerCase())

        resources.updateConfiguration(config, dm)
    }
/*fun roundF(value:Float):Float{
    val df = DecimalFormat("#.###")
    df.roundingMode = RoundingMode.CEILING
    return df.format(value).toFloat()
}

    fun roundD(value:Double):Double{
        val df = DecimalFormat("#.###")
        df.roundingMode = RoundingMode.CEILING
        return df.format(value).toDouble()
    }
    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }*/

    private fun showHelpDialog() {
        var alertDialog: AlertDialog? = null
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.help_dialog, null)
        val next=dialogView.findViewById<Button>(R.id.buttonnext)

        dialogBuilder.setView(dialogView)
        //  dialog.show()
        dialogBuilder.setCancelable(true)
        alertDialog = dialogBuilder.create()
        next.setOnClickListener {
            alertDialog.dismiss()
        }
        val params = alertDialog.window?.attributes
        params?.width =ViewGroup.LayoutParams.MATCH_PARENT //resources.getDimensionPixelSize(R.dimen.dialogwidthhelp)//ViewGroup.LayoutParams.MATCH_PARENT;

        params?.height = ViewGroup.LayoutParams.MATCH_PARENT // resources.getDimensionPixelSize(R.dimen.dialogheight)//ViewGroup.LayoutParams.WRAP_CONTENT;
        //alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.window?.setGravity(Gravity.BOTTOM)
        alertDialog.show()
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("firstTimeHelp",false).apply()
        closeFAB()

    }
private fun showAboutDialog() {
    var alertDialog: AlertDialog? = null
    val dialogBuilder = AlertDialog.Builder(this)
    val inflater = this.layoutInflater
    val dialogView = inflater.inflate(R.layout.about_dialog, null)
    dialogView.textView2.setMovementMethod(LinkMovementMethod.getInstance())
    dialogView.textView6.setMovementMethod(LinkMovementMethod.getInstance())
    dialogBuilder.setView(dialogView)
    //  dialog.show()
    dialogBuilder.setCancelable(true)
    alertDialog = dialogBuilder.create()
    val params = alertDialog.window?.attributes
    params?.width = resources.getDimensionPixelSize(R.dimen.dialogwidthsettings)//ViewGroup.LayoutParams.MATCH_PARENT;

    params?.height = ViewGroup.LayoutParams.WRAP_CONTENT // resources.getDimensionPixelSize(R.dimen.dialogheight)//ViewGroup.LayoutParams.WRAP_CONTENT;
    //alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    alertDialog.window?.setGravity(Gravity.BOTTOM)
    alertDialog.show()
    closeFAB()

}
    fun shutterSound(){
        if (!withSound) return
        tone.play(MediaActionSound.SHUTTER_CLICK)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

       /* if (requestCode == 111 && resultCode == RESULT_OK) {
            val selectedFile = data?.data //The uri with the location of the file

            //val file=File(selectedFile.toString())
            val path = getPath(this, selectedFile!!, null, null)
            Log.e("pathPaaaath", path)
            try {

                module = Module.load(path)
            } catch (e: IOException) {
                Log.e("PytorchHelloWorld", "Error reading file", e)
                finish()
            }
        }*/
        /*else if (requestCode == 112) {
            if (data != null) {
                var path = getPath(this, data!!.data, null, null)
                val contentURI = data!!.data
                try {
                    // bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                    bitmap = decodeSampledBitmap(path!!, 224, 224)
                    Log.e("scaled w/h", "" + bitmap!!.getWidth() + "/" + bitmap!!.height)
                    bitmap = bitmap!!.scale(224)
                    Log.e("scaled w/h", "" + bitmap!!.getWidth() + "/" + bitmap!!.height)

                    //bitmap= bitmap!!.scale(224)
                    //bitmap=getResizedBitmap(bitmap!!,224,224)

                    //bitmap=compressBitmap(bitmap!!,60)
                    //Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
                    imageView.post {
                        imageView.setImageBitmap(bitmap)
                    }


                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_SHORT).show()
                }

            }
        }*/
    }

    fun getPath(
        context: Context, uri: Uri, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf<String>(column)
        try {
            cursor =
                context.getContentResolver().query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }

    @Throws(IOException::class)
    fun fetchModelFile(context: Context, modelName: String): String {
        val file = File(context.getFilesDir(), modelName)

        if (file.exists() && file.length() > 0) {
            Log.e("pathzzz", file.getAbsolutePath())
            return file.getAbsolutePath()
        }

        context.getAssets().open(modelName).use { input ->
            val outputStream = FileOutputStream(file)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
        Log.e("pathooee", file.getAbsolutePath())
        return file.getAbsolutePath()
    }

  /*  fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    fun decodeSampledBitmap(
        path: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            //BitmapFactory.decodeResource(res, resId, this)
            BitmapFactory.decodeFile(path, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false

            //BitmapFactory.decodeResource(res, resId, this)
            BitmapFactory.decodeFile(path, this)

        }
    }
*/
    private fun startCamera() {


        val previewConfig = PreviewConfig.Builder().apply {
            //setTargetResolution(screenSize)
            setTargetResolution(Size(224, 224))

        }.build()

        val preview = Preview(previewConfig)


        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setTargetResolution(Size(224, 224))

                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
       capture_button.setOnClickListener {
           shutterSound()
           //result.setText("Identifying.....")
           var t1=System.currentTimeMillis()
           bar?.visibility=View.VISIBLE
           //=====================================================================
/*           var b=viewFinder.bitmap;

           Log.e(" capture", "" + b!!.getWidth() + "/" + b!!.height)


           b = b!!.scale(224)
           Log.e("scaled cap2", "" + b!!.getWidth() + "/" + b!!.height)

           val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
               b!!,
               TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
               TensorImageUtils.TORCHVISION_NORM_STD_RGB
           )
           Log.e("module", "start forward..")
           // running the model
           val outputTensor = module!!.forward(IValue.from(inputTensor)).toTensor()
           //Log.e("output", outputTensor.toString())

           // getting tensor content as java array of floats
           val scores = outputTensor.getDataAsFloatArray()

           // searching for the index with maximum score
           var maxScore = -java.lang.Float.MAX_VALUE
           var maxScoreIdx = -1
           val strScore=scores[0].toString()
           val truncated=strScore.substring(0,strScore.indexOf('.')+3)

           val strScore2=scores[1].toString()
           val truncated2=strScore2.substring(0,strScore.indexOf('.')+3)
           Log.e("kozbara/paqdonis", truncated + "/"+ truncated2)

           val sigmKoz=sigmoidOf(scores[0].toDouble())
           val strScoreSigm=sigmKoz.toString()
           val truncated3=strScoreSigm.substring(0,strScoreSigm.indexOf('.')+3)

           val sigmPaq=sigmoidOf(scores[1].toDouble())
           val strScore2Sigm=sigmPaq.toString()
           val truncated4=strScore2Sigm.substring(0,strScore2Sigm.indexOf('.')+3)
           Log.e("sigmoid kozb/paq", truncated3 + "/"+ truncated4)

           // Log.e("scores float", "kozbara/paqdonis :" + roundF(scores[0]) + "/" + roundF(scores[1]))
           // Log.e("scores sigmoid", "kozbara/paqdonis :" + roundD(sigmoidOf(scores[0].toDouble())) + "/" + roundD(sigmoidOf(scores[1].toDouble())))
           for (i in scores.indices) {

               if (scores[i] > maxScore) {
                   maxScore = scores[i]
                   maxScoreIdx = i
               }
           }


           val className = NetClasses().classes[maxScoreIdx]

           // showing className on UI


           viewFinder.post {

               result.setText( "$className\nkozbara/paqdonis :$truncated3/$truncated4")
               var t2=System.currentTimeMillis()
               Log.e("time",(t2-t1).toString())


           }
           return@setOnClickListener*/

           //=====================================================================

           //CameraX.unbind(preview)
            //result.setText("Identifying.....")
            val file = File(
                externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg"
            )

            imageCapture.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        Log.e("CameraXApp", msg, exc)
                        viewFinder.post {
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onImageSaved(file: File) {
                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Log.d("CameraXApp", msg)
                        bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        Log.e(" capture", "" + bitmap!!.getWidth() + "/" + bitmap!!.height)
                      /*  Log.e("main color", getDominantColor(bitmap!!).toString())
                        capture_button.post {
                            capture_button.setBackgroundColor(getDominantColor(bitmap!!))
                        }*/
                        //bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, false);
                        bitmap = bitmap!!.scale(224)
                        //bitmap=decodeSampledBitmap(file.absolutePath,224,224)
                        Log.e("scaled cap2", "" + bitmap!!.getWidth() + "/" + bitmap!!.height)


                        //+++++++++++++++++++++++++++++++++++++++++++

                      /*  val r= Palette.from(bitmap!!).generate().swatches.maxBy {
                            it.population
                        }*/
                        val color = Palette.from(bitmap!!).generate().getDarkVibrantColor(Color.TRANSPARENT)
                        Log.e("rgb",""+color)
                        val red= color shr 16 and 0xFF//Color.red(r!!.rgb)
                        Log.e("red",""+red)
                        val green=color shr 8 and 0xFF//Color.blue(r!!.rgb)
                        Log.e("green",""+green)
                        val blue=color shr 0 and 0xFF //Color.green(r!!.rgb)
                        Log.e("blue",""+blue)
                        var hsv= FloatArray(3)
                        Color.RGBToHSV(red,green,blue,hsv)
                        //logreen=np.array([29, 86, 6])
                        //higreen=np.array([64,255,255])
                        for (f in hsv){
                            Log.e("f",""+f)
                        }

                        var validShot=true
                        if (!( hsv[0]>=20 && hsv[0]<=110) )             validShot=false
                        if (!( (hsv[1]*100)>=50 && (hsv[1]*100)<=255) ) validShot=false
                        if (!( (hsv[2]*100)>=2 && (hsv[2]*100)<=255) ) validShot=false
                        Log.e("green ?", validShot.toString())


                        //++++++++++++++++++++++++++++++++++++++++++++++
                        //bitmap=getResizedBitmap(bitmap!!,224,224)

                        //bitmap=compressBitmap(bitmap!!,60)
                        // Log.e("bitmap", bitmap.toString())
                        //Log.e("bitmap", file.absolutePath)
                        // preparing input tensor
                        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                            bitmap!!,
                            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                            TensorImageUtils.TORCHVISION_NORM_STD_RGB
                        )
                        Log.e("module", "start forward..")
                        // running the model
                        val outputTensor = module!!.forward(IValue.from(inputTensor)).toTensor()
                        //Log.e("output", outputTensor.toString())

                        // getting tensor content as java array of floats
                        val scores = outputTensor.getDataAsFloatArray()

                        // searching for the index with maximum score
                        var maxScore = -java.lang.Float.MAX_VALUE
                        var maxScoreIdx = -1
                        val strScore=scores[0].toString()
                        val truncated=strScore.substring(0,strScore.indexOf('.')+3)

                        val strScore2=scores[1].toString()
                        val truncated2=strScore2.substring(0,strScore.indexOf('.')+3)
                        Log.e("kozbara/paqdonis", truncated + "/"+ truncated2)

                       /* val sigmKoz=sigmoidOf(scores[0].toDouble())
                        val strScoreSigm=sigmKoz.toString()
                        val truncated3=strScoreSigm.substring(0,strScoreSigm.indexOf('.')+3)

                        val sigmPaq=sigmoidOf(scores[1].toDouble())
                        val strScore2Sigm=sigmPaq.toString()
                        val truncated4=strScore2Sigm.substring(0,strScore2Sigm.indexOf('.')+3)
                        Log.e("sigmoid kozb/paq", truncated3 + "/"+ truncated4)*/

                       // Log.e("scores float", "kozbara/paqdonis :" + roundF(scores[0]) + "/" + roundF(scores[1]))
                       // Log.e("scores sigmoid", "kozbara/paqdonis :" + roundD(sigmoidOf(scores[0].toDouble())) + "/" + roundD(sigmoidOf(scores[1].toDouble())))
                        for (i in scores.indices) {

                            if (scores[i] > maxScore) {
                                maxScore = scores[i]
                                maxScoreIdx = i
                            }
                        }

                        val classesKeys= listOf("kozbara","paqdonis")
                        val index=if (scores[0]>scores[1]) 0 else 1
                        val className = NetClasses(context).classes[maxScoreIdx]
                        Log.e("class", className)
                        //var classification=Classification(key = "",classification = className,score =if (scores[0]>scores[1]) scores[0].toString() else scores[1].toString() ,fileURI = file.absolutePath,deviceModel = Build.MODEL,validShot = validShot)

                        // showing className on UI
//*************************************************************************************************
                       val key= Firebase.database.getReference("classifications").push().key!!
                        val  classification=Classification(key = key,classification = className,kScore =scores[0].toString(),pScore =scores[1].toString() ,fileURI = file.absolutePath,deviceModel = Build.MODEL,validShot = validShot)

                        Firebase.database.getReference("classifications").child(key).setValue(classification)


                        val storageRef = Firebase.storage.reference

                        // Create a reference to "mountains.jpg"


                    // Create a reference to 'images/mountains.jpg'
                        val imageName="${classesKeys.get(index)}/${classesKeys.get(index)}_${Build.MODEL}_${maxScore}_${classification.key}.jpg"
                        val classImagesRef = storageRef.child(imageName)
                        val baos = ByteArrayOutputStream()
                        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val data = baos.toByteArray()

                        var uploadTask = classImagesRef.putBytes(data)
                        uploadTask.addOnFailureListener {
                            // Handle unsuccessful uploads
                        }.addOnSuccessListener {
                            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                            // ...
                            Log.e("upload","success")
                            if(file.exists()) file.delete()
                        }

                        viewFinder.post {
                            // Handler().postDelayed({
                           // val kozpercent =roundD(sigmoidOf(scores[0].toDouble()) / (sigmoidOf(scores[0].toDouble()) + sigmoidOf(scores[1].toDouble())))
                            //val paqpercent =roundD(sigmoidOf(scores[1].toDouble()) / (sigmoidOf(scores[0].toDouble()) + sigmoidOf(scores[1].toDouble())))
                            //result.setText( "$className\nkozbara/paqdonis :$truncated3/$truncated4")
                            var t2=System.currentTimeMillis()

                            Log.e("time",(t2-t1).toString())
                            var bundle=Bundle()
                            bundle.putParcelable("classification",classification)
                            var fragmentresult=ResultDialog()
                            fragmentresult.arguments=bundle
                            val ft = supportFragmentManager.beginTransaction()
                            val prev = supportFragmentManager.findFragmentByTag("resultdialog")
                            if (prev != null)
                            {
                                ft.remove(prev)
                            }
                            ft.addToBackStack(null)
                            fragmentresult.show(ft, "resultdialog")
                            bar?.visibility=View.INVISIBLE


                            //if(file.exists()) file.delete()
                            //CameraX.bindToLifecycle(this@MainActivity, preview)

                            //  },500)
                            //result.setText(className)
                            // Toast.makeText(baseContext, className, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture)
      if(getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("firstTimeHelp",true)){
          showHelpDialog()
      }
    }

    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
    }


    /*fun getDominantColor(bitmap:Bitmap):Int {
        val swatchesTemp = Palette.from(bitmap).generate().getSwatches()
        val swatches = ArrayList<Palette.Swatch>(swatchesTemp)
        Collections.sort(swatches, object:Comparator<Palette.Swatch> {
            override fun compare(swatch1:Palette.Swatch, swatch2:Palette.Swatch):Int {
                return swatch2.getPopulation() - swatch1.getPopulation()
            }
        })
        return if (swatches.size > 0) swatches.get(0).getRgb() else 0
    }*/
    private fun sigmoidOf(x: Double): Double {
        return (1 / (1 + Math.pow(Math.E, (-1 * x))))
    }

    // Extension method to resize bitmap to maximum width and height
    fun Bitmap.scale(maxWidthAndHeight: Int): Bitmap {
        var newWidth = 0
        var newHeight = 0

        if (this.width >= this.height) {
            val ratio: Float = this.width.toFloat() / this.height.toFloat()

            newWidth = maxWidthAndHeight
            // Calculate the new height for the scaled bitmap
            newHeight = Math.round(maxWidthAndHeight / ratio)
        } else {
            val ratio: Float = this.height.toFloat() / this.width.toFloat()

            // Calculate the new width for the scaled bitmap
            newWidth = Math.round(maxWidthAndHeight / ratio)
            newHeight = maxWidthAndHeight
        }

        return Bitmap.createScaledBitmap(
            this,
            newWidth,
            newHeight,
            false
        )
    }


    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onSaveClicked(isClicked:Boolean) {
        if (isClicked)  {
            android.os.Process.killProcess(android.os.Process.myPid())
            var i = getBaseContext().getPackageManager().
            getLaunchIntentForPackage(getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish()
        }
    }


}
