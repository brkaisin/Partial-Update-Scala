package be.brkaisin.partialupdate.core

/**
  * A [[PartialField]] is the simplest [[Partial]] that can be applied to a value of type T.
  * It can be used to update a field of a case class, or to not update it.
  * @tparam T the type of the value to update
  */
sealed trait PartialField[T] extends Partial[T]

object PartialField {

  /* The value is updated */
  final case class Updated[T](value: T) extends PartialField[T] {
    def applyPartialUpdate(currentValue: T): T = value
  }

  /* The value is not updated */
  final case class Unchanged[T]() extends PartialField[T] {
    def applyPartialUpdate(currentValue: T): T = currentValue
  }

}
