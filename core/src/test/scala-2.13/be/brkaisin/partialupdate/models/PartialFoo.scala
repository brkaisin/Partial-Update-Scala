package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialField}

final case class PartialFoo(
    string: PartialField[String],
    int: PartialField[Int]
) extends Partial[Foo]
    with PartialUpdateDerivation[Foo, PartialFoo] {
  def applyPartialUpdate(currentValue: Foo): Foo = autoDeriveUpdate(currentValue)
}
