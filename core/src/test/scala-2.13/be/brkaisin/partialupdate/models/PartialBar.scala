package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialNestedField, PartialUpdator}

final case class PartialBar(foo1: PartialNestedField[Foo, PartialFoo], foo2: PartialNestedField[Foo, PartialFoo])
    extends Partial[Bar] {

  def applyPartialUpdate(currentValue: Bar): Bar = PartialUpdator[PartialBar].updated[Bar](this, currentValue.copy())
}
