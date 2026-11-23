package com.suryarecorder
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
class MainActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val l=LinearLayout(this).apply { orientation=1; setPadding(28,42,28,28); setBackgroundColor(0xFF10131B.toInt()) }
  l.addView(TextView(this).apply { text="SURYA RECORDER"; textSize=27f; setTextColor(-1) })
  l.addView(TextView(this).apply { text="Start दबाने पर Floating बटन आएगा। रिकॉर्डिंग अपने-आप शुरू नहीं होगी।"; textSize=16f; setTextColor(-1); setPadding(0,18,0,24) })
  l.addView(Button(this).apply { text="START – FLOATING BUTTON"; setOnClickListener {
   if(!Settings.canDrawOverlays(this@MainActivity)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
   else { startService(Intent(this@MainActivity,OverlayService::class.java)); finish() }
  }})
  l.addView(Button(this).apply { text="फ्लोटिंग बटन बंद / हटाएँ"; setOnClickListener {
   stopService(Intent(this@MainActivity,OverlayService::class.java))
   Toast.makeText(this@MainActivity,"Floating बटन हटा दिया गया",Toast.LENGTH_SHORT).show()
  }})
  setContentView(l)
 }
}
