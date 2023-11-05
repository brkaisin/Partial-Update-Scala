package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialField, PartialUpdator, SimplePartialOptionalField}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PartialFoo(
    string: PartialField[String],
    int: PartialField[Int],
    maybeString: SimplePartialOptionalField[String],
    maybeInt: SimplePartialOptionalField[Int]
) extends Partial[Foo] {
  def applyPartialUpdate(currentValue: Foo): Foo = PartialUpdator[PartialFoo].updated[Foo](this, currentValue.copy())
}

object PartialFoo {
  import be.brkaisin.partialupdate.circe.CirceCodecs._
  implicit val codec: Codec[PartialFoo] = partialCodec[PartialFoo](deriveCodec)
}
