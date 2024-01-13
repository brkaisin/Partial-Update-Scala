package be.brkaisin.partialupdate.core.partialupdate

import be.brkaisin.partialupdate.core.Partial

/**
  * Trait that can be mixed in to a Partial[T] to automatically derive the applyPartialUpdate method
  * @tparam T the type of the value to update
  * @tparam PartialType the type of the partial update
  */
trait PartialUpdateDerivation[T, PartialType <: Partial[T]] extends Partial[T] { self: PartialType =>
  def autoDeriveUpdate(currentValue: T)(implicit partialUpdator: PartialUpdator[T, PartialType]): T =
    partialUpdator.update(currentValue, self)
}
