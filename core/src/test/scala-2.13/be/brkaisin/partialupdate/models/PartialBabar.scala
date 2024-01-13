package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialOptionalField}

final case class PartialBabar(
    maybeFoo: PartialOptionalField[Foo, PartialFoo]
) extends Partial[Babar]
    with PartialUpdateDerivation[Babar, PartialBabar] {
  def applyPartialUpdate(currentValue: Babar): Babar = autoDeriveUpdate(currentValue)
}
