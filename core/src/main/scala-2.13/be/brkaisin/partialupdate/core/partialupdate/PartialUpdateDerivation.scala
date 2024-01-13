package be.brkaisin.partialupdate.core.partialupdate

import be.brkaisin.partialupdate.core.Partial

import scala.language.experimental.macros

trait PartialUpdateDerivation[T, PartialType <: Partial[T]] { self: PartialType =>
  implicit def partialUpdator: PartialUpdator[T, PartialType] = macro PartialUpdatorMacro.impl[T, PartialType]

  def autoDeriveUpdate(currentValue: T)(implicit partialUpdatorI: PartialUpdator[T, PartialType]): T =
    partialUpdatorI.update(currentValue, self)
}
