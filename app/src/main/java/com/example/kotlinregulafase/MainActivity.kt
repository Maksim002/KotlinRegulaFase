package com.example.kotlinregulafase

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.params.Face
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.regula.facesdk.FaceReaderService
import com.regula.facesdk.enums.eInputFaceType
import com.regula.facesdk.results.LivenessResponse
import com.regula.facesdk.results.MatchFacesResponse
import com.regula.facesdk.structs.Image
import com.regula.facesdk.structs.MatchFacesRequest
import com.timelysoft.tsjdomcom.utils.LoadingAlert

class MainActivity : AppCompatActivity() {
    private var imageView1: ImageView? = null
    private var imageView2: ImageView? = null

    private lateinit var alert: LoadingAlert
//    private var buttonMatch: Button? = null
    private var buttonLiveness: Button? = null
    private var buttonClear: Button? = null
    private var addButtonIm: Button? = null

    private var textViewSimilarity: TextView? = null
    private var textViewLiveness: TextView? = null

    private var imageUri: Uri? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Инцилизация Прилоудера
        alert = LoadingAlert(this)

        FaceReaderService.Instance().setServiceUrl("https://faceapi.molbulak.com")

        imageView1 = findViewById(R.id.imageView1)
        imageView1!!.layoutParams.height = 400

        imageView2 = findViewById(R.id.imageView2)
        imageView2!!.layoutParams.height = 400

        buttonLiveness = findViewById(R.id.buttonLiveness)
        buttonClear = findViewById(R.id.buttonClear)
        addButtonIm = findViewById(R.id.buttonGalery)

        textViewSimilarity = findViewById(R.id.textViewSimilarity)
        textViewLiveness = findViewById(R.id.textViewLiveness)


        addButtonIm!!.setOnClickListener {
            openGallery(PICK_IMAGE_2)
        }

        buttonLiveness!!.setOnClickListener(View.OnClickListener { v: View? -> startLiveness() })

        //Очитка imageView полуй
        buttonClear!!.setOnClickListener(View.OnClickListener { v: View? ->
            imageView1!!.setImageDrawable(null)
            imageView2!!.setImageDrawable(null)
            textViewSimilarity!!.text = "Сходство: null"
            textViewLiveness!!.text = "Фото: null"
        })
    }

    private fun getImageBitmap(imageView: ImageView?): Bitmap {
        imageView!!.invalidate()
        val drawable = imageView.drawable as BitmapDrawable

        return drawable.bitmap
    }

    private fun openGallery(id: Int) {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, id)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Добовление с галереи фото
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_2) {
            imageUri = data!!.data
            imageView2!!.setImageURI(imageUri)
            imageView2!!.tag = eInputFaceType.ift_DocumentPrinted
            textViewSimilarity!!.text = "Сходство: null"
        }
    }

    //После сравнения 2 фотографий отоброжает сообщение сходства илим иначе
    private fun matchFaces(first: Bitmap, second: Bitmap) {
        val matchRequest = MatchFacesRequest()
        val firstImage = Image()
        firstImage.setImage(first)
        firstImage.imageType = (imageView1!!.tag as Int)
        matchRequest.images.add(firstImage)

        val secondImage = Image()
        secondImage.setImage(second)
        secondImage.imageType = (imageView2!!.tag as Int)
        matchRequest.images.add(secondImage)

        FaceReaderService.Instance().matchFaces(matchRequest) { i: Int, matchFacesResponse: MatchFacesResponse?, s: String? ->
            if (matchFacesResponse?.matchedFaces != null) {
                val similarity = matchFacesResponse.matchedFaces[0].similarity
                if (similarity.toString() == "NaN"){
                    //Если сравнивается живое лицо && придметом
                    textViewSimilarity!!.text = "Сходство: 0.0%"
                }else{
                    //Если сравнивается живое лицо && фото
                    textViewSimilarity!!.text = "Сходство: " + String.format("%.2f", similarity * 100) + "%"
                }
            } else {
                // если error очищает поле
                textViewSimilarity!!.text = "Сходство: null"
            }

            // Снимает блакировку с кнопак
            addButtonIm!!.isEnabled = true
            buttonLiveness!!.isEnabled = true
            buttonClear!!.isEnabled = true
            alert.hide()
        }
    }

    private fun startLiveness() {
        //Метод сканироет лицо проверяет на живность
        FaceReaderService.Instance().startLivenessMatching(this@MainActivity, 1) { livenessResponse: LivenessResponse? ->
            if (livenessResponse != null && livenessResponse.bitmap != null) {
                //Инцилизация Прилоудера
                //Если сканирование прошло успешно
                if (livenessResponse.liveness == 0) {
                    imageView1!!.setImageBitmap(livenessResponse.bitmap)
                    imageView1!!.tag = eInputFaceType.ift_Live
                    comparingPhotos()
                    textViewLiveness!!.text = "Фото: пройденный"
                } else {
                    imageView1!!.setImageBitmap(null)
                    textViewLiveness!!.text = "Фото: неизвестный"
                }
            } else {
                imageView1!!.setImageBitmap(null)
                textViewLiveness!!.text = "Фото: null"
            }
            textViewSimilarity!!.text = "Фото: null"
            FaceReaderService.Instance().stopLivenessProcessing(this);
        }
    }

    // Метод сравнивает 2 фотографии imageView1 && imageView2
    private fun comparingPhotos(){
        if (imageView1!!.drawable != null && imageView2!!.drawable != null) {
            alert.show()
            textViewSimilarity!!.text = "Обработка..."

            matchFaces(getImageBitmap(imageView1), getImageBitmap(imageView2))
            // Блакирует кнопки
            addButtonIm!!.isEnabled = false
            buttonLiveness!!.isEnabled = false
            buttonClear!!.isEnabled = false

        } else {
            Toast.makeText(this@MainActivity, "Наличие обоих изображений является обязательным", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PICK_IMAGE_2 = 2
    }
}