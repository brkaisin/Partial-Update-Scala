package be.brkaisin.partialupdate

import be.brkaisin.partialupdate.core.Partial

import scala.language.experimental.macros

package object diff {
  def derivePartialDiffComputor[T, PartialType <: Partial[T]]: PartialDiffComputor[T, PartialType] =
    macro PartialDiffComputorMacro.impl[T, PartialType]

  def computePartialDiff[T, PartialType <: Partial[T]](currentValue: T, newValue: T)(implicit
      partialDiffComputor: PartialDiffComputor[T, PartialType]
  ): PartialType =
    partialDiffComputor.computePartialDiff(currentValue, newValue)

}
