package be.brkaisin.partialupdate.core

/**
  * [[PartialImmutableField]] is used to represent a field that is not updatable.
  *
  * @tparam T the type of the value of the field
  */
sealed trait PartialImmutableField[T] extends Partial[T]

object PartialImmutableField {
  final case class Unchanged[T]() extends PartialImmutableField[T] {
    def applyPartialUpdate(currentValue: T): T = currentValue
  }
}
