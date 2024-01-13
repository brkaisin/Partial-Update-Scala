package be.brkaisin.partialupdate.diff
import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialField, SimplePartialOptionalField}

// This class is used to test the derivation of a [[PartialDiffComputor]] in the companion object of a case class.
// Together with [[PartialBigBar]], it is used to check that the derivation of a [[PartialDiffComputor]] finds the
// implicit [[PartialDiffComputor]] in the companion object of the case class of the nested field.
final case class PartialBigFoo(int: PartialField[Int], maybeString: SimplePartialOptionalField[String])
    extends Partial[BigFoo]
    with PartialUpdateDerivation[BigFoo, PartialBigFoo] {
  def applyPartialUpdate(currentValue: BigFoo): BigFoo = autoDeriveUpdate(currentValue)
}

object PartialBigFoo {
  import Implicits._
  implicit val partialDiffComputor: PartialDiffComputor[BigFoo, PartialBigFoo] = derivePartialDiffComputor
}
