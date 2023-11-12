package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialField, PartialUpdator}

final case class PartialIdentifiableFoo(
    // id is not here because it is not a field that can be updated
    string: PartialField[String],
    int: PartialField[Int]
) extends Partial[IdentifiableFoo] {
  def applyPartialUpdate(currentValue: IdentifiableFoo): IdentifiableFoo =
    PartialUpdator[PartialIdentifiableFoo].updated[IdentifiableFoo](this, currentValue.copy())
}
