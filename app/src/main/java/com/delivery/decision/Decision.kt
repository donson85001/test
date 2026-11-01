package com.delivery.decision
import android.content.Context
import kotlin.math.max
data class Offer(val amount:Int,val km:Double,val minutes:Int?,val destination:String?)
data class Advice(val label:String,val details:String)
data class Rules(val guarantee:Int=245,val extraMinutes:Int=5,val riskMinutes:Int=8,val cautionFloor:Int=190,val guaranteeLikely:Boolean=true)
object Decision {
 private val districts=listOf("西屯區","南屯區","北屯區","北區","西區","東區","太平區","大里區")
 // Built-in expert ratings, provisional: delivery history + return-trip exposure.
 private val districtRatings=mapOf("西屯區" to 5,"南屯區" to 4,"北屯區" to 4,"北區" to 3,"西區" to 3,"東區" to 3,"太平區" to 2,"大里區" to 2)
 private fun rating(dest:String):Int=districtRatings[dest] ?: 3
 private fun areaNote(dest:String):String=when(dest) {
  "西屯區"->"優先區"
  "南屯區","北屯區"->"可接續區(樣本少)"
  "太平區","大里區"->"折返風險"
  else->"中立區"
 }
 fun parse(text:String):Offer? {
  val money=Regex("(?:NT\\$|NTD|\\$|＄)\\s*([0-9]{2,4})").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return null
  val km=Regex("([0-9]+(?:[.,][0-9]+)?)\\s*(?:公里|km)",RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.replace(',','.')?.toDoubleOrNull() ?: return null
  val minutes=Regex("([0-9]{1,3})\\s*(?:分鐘|分|min)",RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()
  if(money !in 20..1000 || km !in 0.1..50.0 || (minutes!=null && minutes !in 1..180)) return null
  val destination=districts.flatMap { d -> Regex(Regex.escape(d)).findAll(text).map { it.range.first to d }.toList() }.maxByOrNull { it.first }?.second
  return Offer(money,km,minutes,destination)
 }
 fun rules(context:Context):Rules {
  val p=context.getSharedPreferences("decision_v02",Context.MODE_PRIVATE)
  return Rules(extraMinutes=p.getInt("extra",5),riskMinutes=p.getInt("risk",8),cautionFloor=p.getInt("floor",190),guaranteeLikely=p.getBoolean("guarantee",true))
 }
 fun evaluate(o:Offer,r:Rules):Advice {
  val dest=o.destination ?: "未辨識"
  val areaScore=rating(dest)
  if(o.minutes==null) return Advice("資料不足", "${o.amount}元 · ${o.km}km · ${dest} · 無法確認時間")
  if(o.destination==null) return Advice("確認目的地", "${o.amount}元 · ${o.km}km · ${o.minutes}分 · 目的地不明，不評分")
  // The two-week 245/h guarantee is not a per-order rate. Never display
  // offer-level hourly earnings or impute a guaranteed per-order payout.
  val riskKm=if(areaScore<=2) 3.0 else if(areaScore==3) 1.5 else 0.5
  val estimatedTotalKm=o.km+riskKm
  val pricePerKm=o.amount/estimatedTotalKm
  val label=when {
   areaScore<=2 && pricePerKm<15 -> "不建議"
   pricePerKm<12 -> "不建議"
   pricePerKm<18 -> "考慮"
   areaScore<=2 -> "考慮"
   else -> "可考慮接"
  }
  return Advice(label, "${o.amount}元 · ${o.km}km · ${o.minutes}分 · ${dest} ${areaScore}/5 · 含回程估${"%.1f".format(estimatedTotalKm)}km · ${"%.0f".format(pricePerKm)}元/km（非時薪）")
 }
}
