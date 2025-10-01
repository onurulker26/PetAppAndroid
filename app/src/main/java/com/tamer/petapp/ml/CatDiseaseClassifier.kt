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

class CatDiseaseClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var modelInputWidth: Int = 0
    private var modelInputHeight: Int = 0
    private var modelInputChannel: Int = 3
    private var labels: List<String> = emptyList()

    companion object {
        private const val MODEL_PATH = "cat_disease_model.tflite"
        private const val LABEL_PATH = "cat_disease_labels.txt"
        private const val MAX_RESULTS = 1
        private const val THRESHOLD = 0.1f
    }

    init {
        try {
            android.util.Log.d("CatDiseaseClassifier", "Model yükleme başlıyor...")
            val model = loadModelFile()
            android.util.Log.d("CatDiseaseClassifier", "Model dosyası yüklendi, interpreter oluşturuluyor...")
            
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)
            
            // Model boyutlarını al
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape == null || inputShape.size < 3) {
                throw IOException("Model giriş şekli geçersiz")
            }
            
            modelInputHeight = inputShape[1]
            modelInputWidth = inputShape[2]
            
            android.util.Log.d("CatDiseaseClassifier", "Model boyutları: ${modelInputWidth}x${modelInputHeight}")
            
            // Etiketleri yükle
            labels = loadLabels()
            
            if (labels.isEmpty()) {
                throw IOException("Etiket dosyası boş veya okunamadı")
            }
            
            android.util.Log.d("CatDiseaseClassifier", "Model başarıyla yüklendi. Boyutlar: ${modelInputWidth}x${modelInputHeight}, Label sayısı: ${labels.size}")
        } catch (e: IOException) {
            android.util.Log.e("CatDiseaseClassifier", "Model yükleme hatası: ${e.message}", e)
            throw RuntimeException("Model yüklenirken hata oluştu.(${MODEL_PATH}) eksik olabilir.Detay: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("CatDiseaseClassifier", "Beklenmeyen hata: ${e.message}", e)
            throw RuntimeException("Model yüklenirken beklenmeyen hata: ${e.message}")
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        return FileUtil.loadMappedFile(context, MODEL_PATH)
    }

    private fun loadLabels(): List<String> {
        return try {
            android.util.Log.d("CatDiseaseClassifier", "Etiket dosyası yükleniyor: $LABEL_PATH")
            val labels = context.assets.open(LABEL_PATH).bufferedReader().useLines { 
                it.filter { line -> line.trim().isNotEmpty() }.toList() 
            }
            android.util.Log.d("CatDiseaseClassifier", "Etiketler yüklendi: ${labels.size} adet - $labels")
            labels
        } catch (e: Exception) {
            android.util.Log.e("CatDiseaseClassifier", "Etiket dosyası yüklenirken hata: ${e.message}", e)
            emptyList()
        }
    }

    fun analyze(bitmap: Bitmap): List<Classification> {
        if (interpreter == null) {
            throw RuntimeException("Interpreter başlatılmamış")
        }
        
        try {
            // Gelen bitmap'i detaylı kontrol et
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                throw IllegalArgumentException("Geçersiz bitmap boyutları: ${bitmap.width}x${bitmap.height}")
            }
            
            if (bitmap.isRecycled) {
                throw IllegalArgumentException("Bitmap recycled durumda")
            }
            
            android.util.Log.d("CatDiseaseClassifier", "Orijinal bitmap boyutları: ${bitmap.width}x${bitmap.height}")

            // Bitmap'i model giriş boyutuna uygun hale getir
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                modelInputWidth,
                modelInputHeight,
                true
            ).also { scaled ->
                android.util.Log.d("CatDiseaseClassifier", "Ölçeklendirilmiş bitmap boyutları: ${scaled.width}x${scaled.height}")
            }

            // Görüntüyü ön işleme
            val tensorImage = preprocess(scaledBitmap)
            
            // Çıktı tamponunu hazırla
            val probabilityBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, labels.size),
                org.tensorflow.lite.DataType.FLOAT32
            )

            // Modeli çalıştır
            interpreter?.run(tensorImage.buffer, probabilityBuffer.buffer)

            // Scaled bitmap'i temizle
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            // Sonuçları işle
            return processResults(probabilityBuffer)
        } catch (e: Exception) {
            android.util.Log.e("CatDiseaseClassifier", "Analiz hatası: ${e.message}", e)
            throw RuntimeException("Kedi hastalığı analizi sırasında bir hata oluştu: ${e.message}", e)
        }
    }

    private fun preprocess(bitmap: Bitmap): TensorImage {
        try {
            android.util.Log.d("CatDiseaseClassifier", "Görüntü ön işleme başlıyor. Bitmap: ${bitmap.width}x${bitmap.height}")
            
            // Görüntü işleme adımlarını tanımla
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            // TensorImage oluştur ve işle
            val tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
            tensorImage.load(bitmap)
            return imageProcessor.process(tensorImage).also {
                android.util.Log.d("CatDiseaseClassifier", "Görüntü ön işleme tamamlandı")
            }
        } catch (e: Exception) {
            android.util.Log.e("CatDiseaseClassifier", "Ön işleme hatası: ${e.message}", e)
            throw RuntimeException("Kedi hastalığı görüntü ön işleme sırasında hata: ${e.message}", e)
        }
    }

    private fun processResults(probabilityBuffer: TensorBuffer): List<Classification> {
        try {
            android.util.Log.d("CatDiseaseClassifier", "Sonuçlar işleniyor...")
            
            val tensorProcessor = TensorProcessor.Builder()
                .add(NormalizeOp(0f, 1f))
                .build()

            val processedBuffer = tensorProcessor.process(probabilityBuffer)
            
            // Etiket sayısı ve çıktı boyutu kontrolü
            if (labels.isEmpty()) {
                android.util.Log.e("CatDiseaseClassifier", "Etiket listesi boş!")
                return emptyList()
            }

            android.util.Log.d("CatDiseaseClassifier", "Etiket sayısı: ${labels.size}, Çıktı boyutu: ${processedBuffer.buffer.capacity()}")
            
            val labeledProbability = TensorLabel(labels, processedBuffer).mapWithFloatValue

            // Ham sonuçları logla
            labeledProbability.forEach { (label, confidence) ->
                android.util.Log.d("CatDiseaseClassifier", "Ham sonuç - $label: $confidence")
            }

            // Sadece en yüksek olasılıklı sonucu al
            return labeledProbability.entries
                .map { Classification(it.key, it.value) }
                .sortedByDescending { it.confidence }
                .filter { it.confidence > THRESHOLD }
                .take(1)  // Sadece ilk sonucu al
                .also { results ->
                    if (results.isNotEmpty()) {
                        val topResult = results.first()
                        android.util.Log.d("CatDiseaseClassifier", "En yüksek olasılıklı sonuç: ${topResult.label}: ${topResult.confidence}")
                    } else {
                        android.util.Log.w("CatDiseaseClassifier", "Eşik değerini geçen sonuç bulunamadı! Eşik: $THRESHOLD")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("CatDiseaseClassifier", "Sonuç işleme hatası: ${e.message}", e)
            throw RuntimeException("Sonuçlar işlenirken hata oluştu: ${e.message}")
        }
    }

    fun close() {
        interpreter?.close()
    }
} 