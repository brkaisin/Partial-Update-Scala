package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialField, PartialOptionalField, PartialUpdator}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PartialFoo(
    string: PartialField[String],
    int: PartialField[Int],
    maybeString: PartialOptionalField[String],
    maybeInt: PartialOptionalField[Int]
) extends Partial[Foo] {
  def toCompleteUpdated(currentValue: Foo): Foo = PartialUpdator[PartialFoo].updated[Foo](this, currentValue.copy())
}

object PartialFoo {
  import be.brkaisin.partialupdate.circe.CirceCodecs._
  implicit val codec: Codec[PartialFoo] = partialCodec[PartialFoo](deriveCodec)
}
