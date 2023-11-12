package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialNestedField, PartialUpdator, SimplePartialOptionalField}

final case class PartialBar(foo: PartialNestedField[Foo, PartialFoo], maybeBoolean: SimplePartialOptionalField[Boolean])
    extends Partial[Bar] {

  def applyPartialUpdate(currentValue: Bar): Bar = PartialUpdator[PartialBar].updated[Bar](this, currentValue.copy())
}
