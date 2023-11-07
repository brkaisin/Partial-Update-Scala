package be.brkaisin.partialupdate.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Qux(ids: List[java.util.UUID], foos: List[Foo])

object Qux {
  implicit val codec: Codec[Qux] = deriveCodec
}
