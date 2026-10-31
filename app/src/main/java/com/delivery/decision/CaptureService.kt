package com.delivery.decision
import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
class CaptureService: Service() {
 private var projection:MediaProjection?=null
 private var reader:ImageReader?=null
 private var display:android.hardware.display.VirtualDisplay?=null
 private var wm:WindowManager?=null
 private var overlay:TextView?=null
 private val handler=Handler(Looper.getMainLooper())
 private val recognizer=TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
 private var busy=false
 private var lastText=""
 private val loop=object:Runnable { override fun run() { scan(); handler.postDelayed(this,1800) } }
 override fun onBind(intent:Intent?)=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(projection!=null) return START_NOT_STICKY
  val channel=NotificationChannel("capture","外送辨識中",NotificationManager.IMPORTANCE_LOW)
  (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
  val notification=Notification.Builder(this,"capture").setContentTitle("外送接單助手運作中").setContentText("點選 App 停止畫面辨識").setSmallIcon(android.R.drawable.ic_menu_view).build()
  startForeground(1,notification)
  val code=intent?.getIntExtra("resultCode",Activity.RESULT_CANCELED)?:Activity.RESULT_CANCELED
  @Suppress("DEPRECATION") val data=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("resultData",Intent::class.java) else intent?.getParcelableExtra<Intent>("resultData")
  if(code!=Activity.RESULT_OK || data==null) { stopSelf(); return START_NOT_STICKY }
  val metrics=resources.displayMetrics
  reader=ImageReader.newInstance(metrics.widthPixels,metrics.heightPixels,PixelFormat.RGBA_8888,2)
  val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
  projection=mgr.getMediaProjection(code,data)
  projection?.registerCallback(object:MediaProjection.Callback(){ override fun onStop(){stopSelf()} },handler)
  display=projection?.createVirtualDisplay("DeliveryCapture",metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,handler)
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  overlay=TextView(this).apply { text="辨識中…"; textSize=17f; setTextColor(0xffffffff.toInt()); setBackgroundColor(0xdd202020.toInt()); setPadding(22,14,22,14) }
  val params=WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_SECURE,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL; y=85 }
  wm?.addView(overlay,params)
  handler.post(loop)
  return START_NOT_STICKY
 }
 private fun scan() {
  if(busy)return
  val image=reader?.acquireLatestImage()?:return
  val plane=image.planes[0]; val buffer=plane.buffer
  val bitmap=android.graphics.Bitmap.createBitmap(image.width+((plane.rowStride-plane.pixelStride*image.width)/plane.pixelStride),image.height,android.graphics.Bitmap.Config.ARGB_8888)
  bitmap.copyPixelsFromBuffer(buffer); image.close()
  val cropped=android.graphics.Bitmap.createBitmap(bitmap,0,0,resources.displayMetrics.widthPixels,resources.displayMetrics.heightPixels)
  bitmap.recycle(); busy=true
  recognizer.process(InputImage.fromBitmap(cropped,0)).addOnSuccessListener { result ->
   val t=result.text
   if(t!=lastText) { lastText=t; val offer=Decision.parse(t); val answer=offer?.let { Decision.evaluate(it, Decision.rules(this)) }; overlay?.text=if(answer==null) "等待有效訂單…" else "${answer.label}  ${answer.details}" }
  }.addOnCompleteListener { cropped.recycle(); busy=false }
 }
 override fun onDestroy() { handler.removeCallbacks(loop); display?.release(); reader?.close(); projection?.stop(); recognizer.close(); overlay?.let { wm?.removeView(it) }; super.onDestroy() }
}