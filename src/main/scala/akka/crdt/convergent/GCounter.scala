/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.crdt.convergent

import play.api.libs.json._

import java.util.UUID

/**
 * Implements a ConvergentReplicatedDataType 'Growing Counter' also called a 'G-Counter'.
 *
 * A G-Counter is a increment-only counter (inspired by vector clocks) in
 * which only increment and merge are possible. Incrementing the counter
 * adds 1 to the count for the current actor. Divergent histories are
 * resolved by taking the maximum count for each actor (like a vector
 * clock merge). The value of the counter is the sum of all actor counts.
 */
case class GCounter(
  id: String = UUID.randomUUID.toString,
  private[crdt] val state: Map[String, Int] = Map.empty[String, Int]) extends ConvergentReplicatedDataTypeCounter {

  val `type`: String = "g-counter"

  def value: Int = state.values.sum

  def +(node: String, delta: Int = 1): GCounter = {
    if (delta < 0) throw new IllegalArgumentException("Can't decrement a GCounter")
    if (state.contains(node)) copy(state = state + (node -> (state(node) + delta)))
    else copy(state = state + (node -> delta))
  }

  def merge(that: GCounter): GCounter = {
    that.state.foldLeft(this) { (acc, record) => acc + (record._1, record._2) }
  }

  override def toString: String = Json.stringify(GCounter.format.writes(this))
}

object GCounter {
  implicit object format extends Format[GCounter] {
    def reads(json: JsValue): JsResult[GCounter] = JsSuccess(GCounter(
      (json \ "id").as[String],
      (json \ "state").as[Map[String, Int]]
    ))

    def writes(counter: GCounter): JsValue = JsObject(Seq(
      "type"  -> JsString(counter.`type`),
      "id"    -> JsString(counter.id),
      "state" -> Json.toJson(counter.state)
    ))
  }
}