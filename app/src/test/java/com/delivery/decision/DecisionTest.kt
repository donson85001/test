package com.delivery.decision

import org.junit.Assert.*
import org.junit.Test

class DecisionTest {
 @Test fun standardOffer() {
  val o=Decision.parse("NT$101\n34 分鐘\n8.3 公里\n西屯區")!!
  assertEquals(101,o.amount)
  assertEquals(34,o.minutes)
  assertEquals(8.3,o.km,0.001)
  assertEquals("西屯區",o.destination)
 }
 @Test fun officialCashOffer() {
  val o=Decision.parse("$64\n14 分鐘\n0.1 公里")!!
  assertEquals(64,o.amount)
  assertEquals(14,o.minutes)
 }
 @Test fun lastDistrictIsDestination() {
  val o=Decision.parse("NT$90\n19 分鐘\n4.4 km\n西屯區 取餐\n太平區 送達")!!
  assertEquals("太平區",o.destination)
  assertTrue(Decision.evaluate(o,Rules()).details.contains("太平區 2/5"))
 }
 @Test fun rejectIncompleteOffer() {
  assertNull(Decision.parse("NT$80 12分鐘"))
 }
}
