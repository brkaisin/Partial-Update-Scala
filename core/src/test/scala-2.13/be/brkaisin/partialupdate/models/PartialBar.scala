package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialNestedField}

final case class PartialBar(foo1: PartialNestedField[Foo, PartialFoo], foo2: PartialNestedField[Foo, PartialFoo])
    extends Partial[Bar]
    with PartialUpdateDerivation[Bar, PartialBar] {
  def applyPartialUpdate(currentValue: Bar): Bar = autoDeriveUpdate(currentValue)
}
