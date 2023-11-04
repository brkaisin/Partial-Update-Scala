package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialOptionalNestedField, PartialUpdator}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PartialBabar(
    maybeFoo: PartialOptionalNestedField[Foo, PartialFoo]
) extends Partial[Babar] {

  def toCompleteUpdated(currentValue: Babar): Babar =
    PartialUpdator[PartialBabar].updated[Babar](this, currentValue.copy())
}

object PartialBabar {
  import be.brkaisin.partialupdate.circe.CirceCodecs._
  implicit val codec: Codec[PartialBabar] = partialCodec[PartialBabar](deriveCodec)
}
