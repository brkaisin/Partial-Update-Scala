package be.brkaisin.partialupdate

import io.circe.Encoder
import io.circe.syntax.EncoderOps

package object circe {

  implicit final class RichEncoder[T](val encoder: Encoder[T]) extends AnyVal {
    def addKeyValue[V: Encoder](key: String, value: V): Encoder[T] =
      encoder.mapJson(_.mapObject(_.add(key, value.asJson)))
  }

}
