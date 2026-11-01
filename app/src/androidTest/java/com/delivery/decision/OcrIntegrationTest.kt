package com.delivery.decision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OcrIntegrationTest {
 @Test fun recognizeRenderedOfferAndEvaluate() {
  val bitmap=Bitmap.createBitmap(900,1400,Bitmap.Config.ARGB_8888)
  val canvas=Canvas(bitmap)
  canvas.drawColor(Color.WHITE)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
   color=Color.BLACK
   textSize=65f
   typeface=Typeface.create("sans-serif",Typeface.BOLD)
  }
  listOf("Uber Eats","NT$101","34 分鐘","8.3 公里","西屯區").forEachIndexed { i,s ->
   canvas.drawText(s,60f,150f+i*175,paint)
  }
  val recognizer=TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
  try {
   val result=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)),60,TimeUnit.SECONDS)
   val offer=Decision.parse(result.text)
   assertNotNull("OCR text: ${result.text}",offer)
   assertEquals(101,offer!!.amount)
   assertEquals(34,offer.minutes)
   assertEquals(8.3,offer.km,0.001)
   assertEquals("西屯區",offer.destination)
   assertTrue(Decision.evaluate(offer,Rules()).details.contains("西屯區 5/5"))
  } finally { recognizer.close(); bitmap.recycle() }
 }
}
