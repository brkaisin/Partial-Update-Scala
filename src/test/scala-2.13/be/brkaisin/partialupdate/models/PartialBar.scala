package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialNestedField, PartialOptionalField, PartialUpdator}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PartialBar(foo: PartialNestedField[Foo, PartialFoo], maybeBoolean: PartialOptionalField[Boolean])
    extends Partial[Bar] {

  def toCompleteUpdated(currentValue: Bar): Bar = PartialUpdator[PartialBar].updated[Bar](this, currentValue.copy())
}

object PartialBar {
  import be.brkaisin.partialupdate.circe.CirceCodecs._
  implicit val codec: Codec[PartialBar] = partialCodec[PartialBar](deriveCodec)
}
