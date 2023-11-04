package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.util.Identifiable
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class IdentifiableFoo(id: java.util.UUID, string: String, int: Int)
    extends Identifiable[IdentifiableFoo, java.util.UUID]

object IdentifiableFoo {
  implicit val codec: Codec[IdentifiableFoo] = deriveCodec
}
