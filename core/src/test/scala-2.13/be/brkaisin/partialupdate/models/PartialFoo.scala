package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialField, PartialUpdator, SimplePartialOptionalField}

final case class PartialFoo(
    string: PartialField[String],
    int: PartialField[Int],
    maybeString: SimplePartialOptionalField[String],
    maybeInt: SimplePartialOptionalField[Int]
) extends Partial[Foo] {
  def applyPartialUpdate(currentValue: Foo): Foo = PartialUpdator[PartialFoo].updated[Foo](this, currentValue.copy())
}
