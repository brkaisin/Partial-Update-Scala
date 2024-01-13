package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialNestedField}
import be.brkaisin.partialupdate.diff.{derivePartialDiffComputor, PartialDiffComputor}

// See the comment in [[PartialBigFoo]] for an explanation of this code.
final case class PartialBigBar(bigFoo: PartialNestedField[BigFoo, PartialBigFoo])
    extends Partial[BigBar]
    with PartialUpdateDerivation[BigBar, PartialBigBar] {
  def applyPartialUpdate(currentValue: BigBar): BigBar = autoDeriveUpdate(currentValue)
}

object PartialBigBar {
  import be.brkaisin.partialupdate.diff.Implicits._
  implicit val partialDiffComputor: PartialDiffComputor[BigBar, PartialBigBar] = derivePartialDiffComputor
}
