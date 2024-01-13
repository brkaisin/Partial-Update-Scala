package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialImmutableField}

final case class PartialImmutableFoo(
    string: PartialImmutableField[String] = PartialImmutableField.Unchanged(),
    bar: PartialImmutableField[Bar] = PartialImmutableField.Unchanged()
) extends Partial[ImmutableFoo]
    with PartialUpdateDerivation[ImmutableFoo, PartialImmutableFoo] {
  def applyPartialUpdate(currentValue: ImmutableFoo): ImmutableFoo = autoDeriveUpdate(currentValue)
}
