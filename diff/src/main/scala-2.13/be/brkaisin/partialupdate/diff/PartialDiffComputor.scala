package be.brkaisin.partialupdate.diff

import be.brkaisin.partialupdate.core.Partial

/**
  * [[PartialDiffComputor]] is used to compute the difference between two values of type T.
  * @tparam T the type of the value to compute the difference of
  * @tparam PartialType the type of the [[Partial]] that represents the difference
  */
trait PartialDiffComputor[T, PartialType <: Partial[T]] {
  def computePartialDiff(currentValue: T, newValue: T): PartialType
}
