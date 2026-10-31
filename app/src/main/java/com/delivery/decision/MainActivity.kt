package com.delivery.decision
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.*
class MainActivity : Activity() {
 private val requestCapture=100
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
  val p=getSharedPreferences("decision_v02",MODE_PRIVATE)
  val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(36,54,36,24) }
  val scroll=ScrollView(this);scroll.addView(layout)
  layout.addView(TextView(this).apply { text="外送接單助手 v0.2\n即時 OCR + 兩週保底情境估算\n\n不是逐單保證實領；每天由試算表更新策略。辨識僅在本機進行。騎車時勿操作。";textSize=18f })
  val toggle=CheckBox(this).apply { text="假設本期最後需要保底補差";isChecked=p.getBoolean("guarantee",true);setOnCheckedChangeListener { _,v->p.edit().putBoolean("guarantee",v).apply() } }
  layout.addView(toggle)
  fun setting(title:String,key:String,initial:Int,min:Int,max:Int) {
   layout.addView(TextView(this).apply { text=title;textSize=16f })
   val value=TextView(this).apply { text=p.getInt(key,initial).toString() }
   layout.addView(value)
   layout.addView(SeekBar(this).apply { this.max=max-min;progress=p.getInt(key,initial)-min
    setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
     override fun onProgressChanged(s:SeekBar?,progress:Int,fromUser:Boolean){ val v=progress+min;value.text=v.toString();if(fromUser)p.edit().putInt(key,v).apply() }
     override fun onStartTrackingTouch(s:SeekBar?) {}
     override fun onStopTrackingTouch(s:SeekBar?) {}
    })
   })
  }
  setting("每單額外緩衝分鐘（預設5）","extra",5,0,20)
  setting("太平／大里額外風險分鐘（預設8）","risk",8,0,25)
  setting("不建議門檻 元/時（預設190）","floor",190,120,245)
  layout.addView(TextView(this).apply { text="區域：西屯優先觀察；太平、大里提醒折返。樣本僅40筆，沒有自動地區加價。\n辨識不到分鐘時只顯示資料不足，不猜測。";textSize=14f })
  layout.addView(Button(this).apply { text="1. 授權懸浮視窗";setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName))) } })
  layout.addView(Button(this).apply { text="2. 開始畫面辨識";setOnClickListener {
   if(!Settings.canDrawOverlays(this@MainActivity)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName)))
   else { val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager;startActivityForResult(mgr.createScreenCaptureIntent(),requestCapture) }
  } })
  layout.addView(Button(this).apply { text="停止辨識";setOnClickListener { stopService(Intent(this@MainActivity,CaptureService::class.java)) } })
  setContentView(scroll)
 }
 @Deprecated("Android Activity result callback") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==requestCapture&&resultCode==RESULT_OK&&data!=null) startForegroundService(Intent(this,CaptureService::class.java).putExtra("resultCode",resultCode).putExtra("resultData",data))
 }
}
