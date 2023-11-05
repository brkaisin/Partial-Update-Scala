package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialNestedField, PartialUpdator, SimplePartialOptionalField}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PartialBar(foo: PartialNestedField[Foo, PartialFoo], maybeBoolean: SimplePartialOptionalField[Boolean])
    extends Partial[Bar] {

  def applyPartialUpdate(currentValue: Bar): Bar = PartialUpdator[PartialBar].updated[Bar](this, currentValue.copy())
}

object PartialBar {
  import be.brkaisin.partialupdate.circe.CirceCodecs._
  implicit val codec: Codec[PartialBar] = partialCodec[PartialBar](deriveCodec)
}
