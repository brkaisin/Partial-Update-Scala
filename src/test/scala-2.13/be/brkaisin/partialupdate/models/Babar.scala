package be.brkaisin.partialupdate.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Babar(maybeFoo: Option[Foo])

object Babar {
  implicit val codec: Codec[Babar] = deriveCodec
}
