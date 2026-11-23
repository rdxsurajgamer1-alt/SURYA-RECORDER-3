package com.suryarecorder
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
class OverlayService: Service() {
 private lateinit var wm:WindowManager
 private var bubble:TextView?=null
 private var panel:LinearLayout?=null
 private var ratio="9:16"; private var res="1080"; private var fps="60"; private var audio="MIC"
 private var cw="1080"; private var ch="1920"
 private fun params(w:Int,h:Int,x:Int,y:Int,focus:Boolean=false)=WindowManager.LayoutParams(w,h,
  if(Build.VERSION.SDK_INT>=26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
  if(focus) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
  PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.END; this.x=x; this.y=y }
 override fun onCreate(){ super.onCreate(); wm=getSystemService(WINDOW_SERVICE) as WindowManager
  bubble=TextView(this).apply { text="●"; textSize=30f; gravity=Gravity.CENTER; setTextColor(-1); setBackgroundColor(0xFFEF6C20.toInt()); setOnClickListener{menu()} }
  try{wm.addView(bubble,params(62,62,18,240))}catch(e:Exception){stopSelf()}
 }
 private fun menu(){
  if(panel!=null){close();return}
  val p=LinearLayout(this).apply{orientation=1;setPadding(18,16,18,16);setBackgroundColor(0xFF202633.toInt())}
  p.addView(TextView(this).apply{text="SURYA RECORDER • सेटिंग";textSize=18f;setTextColor(-1)})
  fun choice(label:String,items:List<String>,selected:String,change:(String)->Unit){
   p.addView(TextView(this).apply{text=label;setTextColor(0xFFFFD180.toInt())})
   val s=Spinner(this);s.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,items);s.setSelection(items.indexOf(selected).coerceAtLeast(0))
   s.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
    override fun onNothingSelected(a:android.widget.AdapterView<*>?){}
    override fun onItemSelected(a:android.widget.AdapterView<*>?,v:android.view.View?,n:Int,id:Long){change(items[n])}
   };p.addView(s)
  }
  choice("वीडियो साइज",listOf("9:16","16:9","1:1","4:3","Custom"),ratio){ratio=it}
  choice("Resolution",listOf("720p","1080p","2K / 1440p","4K / 2160p"),when(res){"720"->"720p";"1440"->"2K / 1440p";"2160"->"4K / 2160p";else->"1080p"}){res=when(it){"720p"->"720";"2K / 1440p"->"1440";"4K / 2160p"->"2160";else->"1080"}}
  choice("FPS",listOf("30","60","90"),fps){fps=it}
  choice("Audio",listOf("Microphone","No Audio","Internal Audio","Internal + Microphone"),when(audio){"NONE"->"No Audio";"INTERNAL"->"Internal Audio";"BOTH"->"Internal + Microphone";else->"Microphone"}){audio=when(it){"No Audio"->"NONE";"Internal Audio"->"INTERNAL";"Internal + Microphone"->"BOTH";else->"MIC"}}
  if(ratio=="Custom"){
   val row=LinearLayout(this).apply{orientation=0}
   val w=EditText(this).apply{hint="Width";setText(cw);inputType=2}
   val h=EditText(this).apply{hint="Height";setText(ch);inputType=2}
   row.addView(w,LinearLayout.LayoutParams(0,-2,1f));row.addView(h,LinearLayout.LayoutParams(0,-2,1f));p.addView(row)
   p.addView(Button(this).apply{text="Custom size सेव करें";setOnClickListener{cw=w.text.toString();ch=h.text.toString()}})
  }
  p.addView(Button(this).apply{text="● RECORD";setOnClickListener{
   val i=Intent(this@OverlayService,CaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
   i.putExtra("ratio",ratio);i.putExtra("res",res);i.putExtra("fps",fps);i.putExtra("audio",audio);i.putExtra("cw",cw);i.putExtra("ch",ch)
   startActivity(i);close()
  }})
  p.addView(Button(this).apply{text="मेन्यू बंद करें";setOnClickListener{close()}})
  p.addView(Button(this).apply{text="फ्लोटिंग बटन हटाएँ";setOnClickListener{
   close()
   stopSelf()
  }})
  panel=p;try{wm.addView(p,params(300,-2,10,310,true))}catch(e:Exception){panel=null}
 }
 private fun close(){try{panel?.let{wm.removeView(it)}}catch(e:Exception){};panel=null}
 override fun onDestroy(){close();try{bubble?.let{wm.removeView(it)}}catch(e:Exception){};super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
}
