package be.brkaisin.partialupdate.core

sealed trait PartialOptionalField[T] extends Partial[Option[T]]

object PartialOptionalField {

  final case class Updated[T](value: T) extends PartialOptionalField[T] {
    def toCompleteUpdated(currentValue: Option[T]): Option[T] = Some(value)
  }

  final case class Unchanged[T]() extends PartialOptionalField[T] {
    def toCompleteUpdated(currentValue: Option[T]): Option[T] = currentValue
  }

  final case class Deleted[T]() extends PartialOptionalField[T] {
    def toCompleteUpdated(currentValue: Option[T]): Option[T] = None
  }
}
