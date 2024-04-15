package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.partialupdate.PartialUpdateDerivation
import be.brkaisin.partialupdate.core.{Partial, PartialField}
import be.brkaisin.partialupdate.models.Corge.StringOrInt

final case class Corge(stringOrInt: StringOrInt)

object Corge {

  sealed trait StringOrInt

  object StringOrInt {
    final case class StringWrapper(value: String) extends StringOrInt

    object StringWrapper {
      final case class PartialStringWrapper(value: PartialField[String])
          extends Partial[StringWrapper]
          with PartialUpdateDerivation[StringWrapper, PartialStringWrapper] {
        def applyPartialUpdate(currentValue: StringWrapper): StringWrapper = autoDeriveUpdate(currentValue)
      }
    }

    final case class IntWrapper(value: Int) extends StringOrInt

    object IntWrapper {
      final case class PartialIntWrapper(value: PartialField[Int])
          extends Partial[IntWrapper]
          with PartialUpdateDerivation[IntWrapper, PartialIntWrapper] {
        def applyPartialUpdate(currentValue: IntWrapper): IntWrapper = autoDeriveUpdate(currentValue)
      }
    }
  }
}
