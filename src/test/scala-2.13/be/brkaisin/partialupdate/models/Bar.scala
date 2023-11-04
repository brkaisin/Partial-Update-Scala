package be.brkaisin.partialupdate.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Bar(foo: Foo, maybeBoolean: Option[Boolean])

object Bar {
  implicit val codec: Codec[Bar] = deriveCodec
}
