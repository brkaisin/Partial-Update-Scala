package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialEnum2Field, PartialField}

final case class PartialCorge(
    stringOrInt: PartialEnum2Field[
      Corge.StringOrInt,
      Corge.StringOrInt.StringWrapper,
      Corge.StringOrInt.StringWrapper.PartialStringWrapper,
      Corge.StringOrInt.IntWrapper,
      Corge.StringOrInt.IntWrapper.PartialIntWrapper,
    ] = PartialEnum2Field.Unchanged()
) extends Partial[Corge]
    with PartialUpdateDerivation[Corge, PartialCorge] {
  def applyPartialUpdate(currentValue: Corge): Corge = autoDeriveUpdate(currentValue)
}
