package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialIdentifiableListField}

final case class PartialBaz(
    foos: PartialIdentifiableListField[java.util.UUID, IdentifiableFoo, PartialIdentifiableFoo]
) extends Partial[Baz]
    with PartialUpdateDerivation[Baz, PartialBaz] {
  def applyPartialUpdate(currentValue: Baz): Baz = autoDeriveUpdate(currentValue)
}
