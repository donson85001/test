package com.delivery.decision
data class Offer(val amount:Int,val km:Double,val minutes:Int?)
object Decision {
 fun parse(text:String):Offer? {
  val money=Regex("(?:NT\\$|NTD|\\$|＄)\\s*([0-9]{2,4})").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return null
  val km=Regex("([0-9]+(?:[.,][0-9]+)?)\\s*(?:公里|km|KM)").find(text)?.groupValues?.get(1)?.replace(',','.')?.toDoubleOrNull() ?: return null
  val minutes=Regex("([0-9]{1,3})\\s*(?:分鐘|分|min)").find(text)?.groupValues?.get(1)?.toIntOrNull()
  if(money !in 20..1000 || km !in 0.1..50.0 || (minutes!=null && minutes !in 1..180)) return null
  return Offer(money,km,minutes)
 }
 fun evaluate(o:Offer):Pair<String,String> {
  val required=when { o.km<=3 ->45; o.km<=5 ->65; o.km<=7 ->85; else ->110 }
  val hourly=o.minutes?.let { o.amount*60/it }
  val label=when { o.amount<required -> "不建議"; hourly!=null && hourly<180 -> "不建議"; hourly!=null && hourly<225 -> "考慮"; o.km>7 -> "考慮"; else -> "建議接" }
  return label to "${o.amount}元 · ${o.km}公里"+(hourly?.let { " · 約${it}元/時" }?:" · 時間未知")
 }
}