package be.brkaisin.partialupdate

import be.brkaisin.partialupdate.core.Partial

package object diff {

  def computePartialDiff[T, PartialType <: Partial[T]](currentValue: T, newValue: T)(implicit
      partialDiffComputor: PartialDiffComputor[T, PartialType]
  ): PartialType =
    partialDiffComputor.computePartialDiff(currentValue, newValue)

}
