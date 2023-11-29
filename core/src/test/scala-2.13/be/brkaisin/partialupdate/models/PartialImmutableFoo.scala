package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialImmutableField, PartialUpdator}

final case class PartialImmutableFoo(
    string: PartialImmutableField[String] = PartialImmutableField.Unchanged(),
    bar: PartialImmutableField[Bar] = PartialImmutableField.Unchanged()
) extends Partial[ImmutableFoo] {
  def applyPartialUpdate(currentValue: ImmutableFoo): ImmutableFoo =
    PartialUpdator[PartialImmutableFoo].updated[ImmutableFoo](this, currentValue.copy())
}
