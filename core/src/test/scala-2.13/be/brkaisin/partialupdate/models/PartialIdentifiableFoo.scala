package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialField, PartialImmutableField, PartialUpdator}

final case class PartialIdentifiableFoo(
    id: PartialImmutableField[java.util.UUID] = PartialImmutableField.Unchanged(),
    string: PartialField[String] = PartialField.Unchanged(),
    int: PartialField[Int] = PartialField.Unchanged()
) extends Partial[IdentifiableFoo] {
  def applyPartialUpdate(currentValue: IdentifiableFoo): IdentifiableFoo =
    PartialUpdator[PartialIdentifiableFoo].updated[IdentifiableFoo](this, currentValue.copy())
}
