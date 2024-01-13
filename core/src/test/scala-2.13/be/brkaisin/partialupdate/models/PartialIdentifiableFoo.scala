package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialField, PartialImmutableField}

final case class PartialIdentifiableFoo(
    id: PartialImmutableField[java.util.UUID] = PartialImmutableField.Unchanged(),
    string: PartialField[String] = PartialField.Unchanged(),
    int: PartialField[Int] = PartialField.Unchanged()
) extends Partial[IdentifiableFoo]
    with PartialUpdateDerivation[IdentifiableFoo, PartialIdentifiableFoo] {
  def applyPartialUpdate(currentValue: IdentifiableFoo): IdentifiableFoo = autoDeriveUpdate(currentValue)
}
