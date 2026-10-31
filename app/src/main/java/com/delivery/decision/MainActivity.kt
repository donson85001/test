package com.delivery.decision
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
class MainActivity : Activity() {
 private val requestCapture = 100
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,70,40,30) }
  layout.addView(TextView(this).apply { text = "外送接單助手 0.1\n\n畫面僅在本機辨識，不上傳截圖。\n啟動後會出現浮動建議，接單仍由你操作。\n\n注意：目前是測試版，請勿在騎車時操作。"; textSize = 18f })
  layout.addView(Button(this).apply { text = "1. 授權懸浮視窗"; setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } })
  layout.addView(Button(this).apply { text = "2. 開始畫面辨識"; setOnClickListener { if (!Settings.canDrawOverlays(this@MainActivity)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } else { val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager; startActivityForResult(manager.createScreenCaptureIntent(), requestCapture) } } })
  layout.addView(Button(this).apply { text = "停止辨識"; setOnClickListener { stopService(Intent(this@MainActivity, CaptureService::class.java)) } })
  setContentView(layout)
 }
 @Deprecated("Android Activity result callback") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==requestCapture && resultCode==RESULT_OK && data!=null) { val i=Intent(this,CaptureService::class.java).putExtra("resultCode",resultCode).putExtra("resultData",data); startForegroundService(i) }
 }
}