package be.brkaisin.partialupdate.core

sealed trait PartialField[T] extends Partial[T]

object PartialField {

  final case class Updated[T](value: T) extends PartialField[T] {
    def toCompleteUpdated(currentValue: T): T = value
  }

  final case class Unchanged[T]() extends PartialField[T] {
    def toCompleteUpdated(currentValue: T): T = currentValue
  }

}
