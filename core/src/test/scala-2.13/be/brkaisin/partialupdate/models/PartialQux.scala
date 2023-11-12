package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialListField, PartialUpdator, SimplePartialListField}

final case class PartialQux(
    ids: SimplePartialListField[java.util.UUID],
    foos: PartialListField[Foo, PartialFoo]
) extends Partial[Qux] {

  def applyPartialUpdate(currentValue: Qux): Qux = PartialUpdator[PartialQux].updated[Qux](this, currentValue.copy())
}
