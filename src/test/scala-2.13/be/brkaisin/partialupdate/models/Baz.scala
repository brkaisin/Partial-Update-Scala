package be.brkaisin.partialupdate.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Baz(foos: List[IdentifiableFoo])

object Baz {
  implicit val codec: Codec[Baz] = deriveCodec
}
