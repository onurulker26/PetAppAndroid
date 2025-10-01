package com.tamer.petapp.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.MappedByteBuffer

class DogDiseaseClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var modelInputWidth: Int = 0
    private var modelInputHeight: Int = 0
    private var modelInputChannel: Int = 3
    private var labels: List<String> = emptyList()

    companion object {
        private const val MODEL_PATH = "dog_disease_model.tflite"
        private const val LABEL_PATH = "dog_disease_labels.txt"
        private const val MAX_RESULTS = 3
        private const val THRESHOLD = 0.5f
    }

    init {
        try {
            val model = loadModelFile()
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)
            
            // Model boyutlarını al
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            modelInputHeight = inputShape?.get(1) ?: 224
            modelInputWidth = inputShape?.get(2) ?: 224
            
            // Etiketleri yükle
            labels = loadLabels()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        return FileUtil.loadMappedFile(context, MODEL_PATH)
    }

    private fun loadLabels(): List<String> {
        return try {
            android.util.Log.d("DogDiseaseClassifier", "Etiket dosyası yükleniyor: $LABEL_PATH")
            val labels = context.assets.open(LABEL_PATH).bufferedReader().useLines { 
                it.filter { line -> line.trim().isNotEmpty() }.toList() 
            }
            android.util.Log.d("DogDiseaseClassifier", "Etiketler yüklendi: ${labels.size} adet - $labels")
            labels
        } catch (e: Exception) {
            android.util.Log.e("DogDiseaseClassifier", "Etiket dosyası yüklenirken hata: ${e.message}", e)
            emptyList()
        }
    }

    fun analyze(bitmap: Bitmap): List<Classification> {
        try {
            // Gelen bitmap'i kontrol et
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                throw IllegalArgumentException("Geçersiz bitmap boyutları: ${bitmap.width}x${bitmap.height}")
            }

            // Bitmap'i model giriş boyutuna uygun hale getir
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                modelInputWidth,
                modelInputHeight,
                true
            )

            // Görüntüyü ön işleme
            val tensorImage = preprocess(scaledBitmap)
            
            // Çıktı tamponunu hazırla
            val probabilityBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, labels.size),
                org.tensorflow.lite.DataType.FLOAT32
            )

            // Modeli çalıştır
            interpreter?.run(tensorImage.buffer, probabilityBuffer.buffer)

            // Sonuçları işle
            return processResults(probabilityBuffer)
        } catch (e: Exception) {
            throw RuntimeException("Köpek hastalığı analizi sırasında bir hata oluştu: ${e.message}", e)
        }
    }

    private fun preprocess(bitmap: Bitmap): TensorImage {
        try {
            // Görüntü işleme adımlarını tanımla
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            // TensorImage oluştur ve işle
            val tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
            tensorImage.load(bitmap)
            return imageProcessor.process(tensorImage)
        } catch (e: Exception) {
            throw RuntimeException("Köpek hastalığı görüntü ön işleme sırasında hata: ${e.message}", e)
        }
    }

    private fun processResults(probabilityBuffer: TensorBuffer): List<Classification> {
        val tensorProcessor = TensorProcessor.Builder()
            .add(NormalizeOp(0f, 1f))
            .build()

        val processedBuffer = tensorProcessor.process(probabilityBuffer)
        val labeledProbability = TensorLabel(labels, processedBuffer).mapWithFloatValue

        // Sonuçları sırala ve filtrele
        return labeledProbability.entries
            .map { Classification(it.key, it.value) }
            .sortedByDescending { it.confidence }
            .filter { it.confidence > THRESHOLD }
            .take(MAX_RESULTS)
    }

    fun close() {
        interpreter?.close()
    }
} 