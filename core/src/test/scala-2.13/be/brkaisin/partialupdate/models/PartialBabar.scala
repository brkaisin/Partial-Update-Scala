package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialOptionalField, PartialUpdator}

final case class PartialBabar(
    maybeFoo: PartialOptionalField[Foo, PartialFoo]
) extends Partial[Babar] {

  def applyPartialUpdate(currentValue: Babar): Babar =
    PartialUpdator[PartialBabar].updated[Babar](this, currentValue.copy())
}
