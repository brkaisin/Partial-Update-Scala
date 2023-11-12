package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialIdentifiableListField, PartialUpdator}

final case class PartialBaz(
    foos: PartialIdentifiableListField[java.util.UUID, IdentifiableFoo, PartialIdentifiableFoo]
) extends Partial[Baz] {

  def applyPartialUpdate(currentValue: Baz): Baz = PartialUpdator[PartialBaz].updated[Baz](this, currentValue.copy())
}
