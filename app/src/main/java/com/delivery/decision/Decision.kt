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
  val risk=rating(dest)<=2
  if(o.minutes==null) return Advice("資料不足",o.amount.toString()+"元 · "+o.km+"km · "+dest+" · 缺配送時間")
  val raw=o.amount*60.0/o.minutes
  val guaranteed=max(o.amount.toDouble(),r.guarantee*o.minutes/60.0)
  val effective=(if(r.guaranteeLikely) guaranteed else o.amount.toDouble())*60.0/(o.minutes+r.extraMinutes+(if(risk)r.riskMinutes else 0))
  val label=when { effective<r.cautionFloor -> "不建議"; risk && effective<r.guarantee -> "考慮"; effective>=r.guarantee -> "建議接"; else -> "考慮" }
  val mode=if(r.guaranteeLikely)"保底情境" else "原報價情境"
  return Advice(label,o.amount.toString()+"元 · "+o.km+"km · "+dest+" · 地區"+rating(dest)+"/5("+areaNote(dest)+") · 原"+raw.toInt()+"/h · "+mode+" "+effective.toInt()+"/h"+(if(risk)" · 注意折返" else ""))
 }
}
