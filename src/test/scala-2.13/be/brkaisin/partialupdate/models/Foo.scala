package be.brkaisin.partialupdate.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Foo(string: String, int: Int, maybeString: Option[String], maybeInt: Option[Int])

object Foo {
  implicit val codec: Codec[Foo] = deriveCodec
}
