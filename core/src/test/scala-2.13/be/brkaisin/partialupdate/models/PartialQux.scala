package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialListField, SimplePartialListField}

final case class PartialQux(
    ids: SimplePartialListField[java.util.UUID],
    foos: PartialListField[Foo, PartialFoo]
) extends Partial[Qux]
    with PartialUpdateDerivation[Qux, PartialQux] {
  def applyPartialUpdate(currentValue: Qux): Qux = autoDeriveUpdate(currentValue)
}
