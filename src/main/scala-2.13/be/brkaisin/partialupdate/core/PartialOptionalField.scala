package be.brkaisin.partialupdate.core

sealed trait PartialOptionalField[T, PartialFieldType <: Partial[T]] extends Partial[Option[T]]

object PartialOptionalField {

  final case class Set[T, PartialFieldType <: Partial[T]](value: T) extends PartialOptionalField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if the value is already set")
    def toCompleteUpdated(currentValue: Option[T]): Option[T] =
      currentValue match {
        case Some(_) => throw new IllegalArgumentException("Cannot set a value that is already set.")
        case None    => Some(value)
      }
  }

  final case class Updated[T, PartialFieldType <: Partial[T]](value: PartialFieldType)
      extends PartialOptionalField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if the value is not set")
    def toCompleteUpdated(currentValue: Option[T]): Option[T] = {
      val updatedComplete = value.toCompleteUpdated {
        currentValue match {
          case Some(innerValue) => innerValue
          case None =>
            throw new IllegalArgumentException("Cannot update a value that is not set.")
        }
      }
      Some(updatedComplete)
    }
  }

  final case class Deleted[T, PartialFieldType <: Partial[T]]() extends PartialOptionalField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if the value is not set")
    def toCompleteUpdated(currentValue: Option[T]): Option[T] =
      currentValue match {
        case Some(_) => None
        case None    => throw new IllegalArgumentException("Cannot delete a value that is not set.")
      }
  }

  final case class Unchanged[T, PartialFieldType <: Partial[T]]() extends PartialOptionalField[T, PartialFieldType] {
    def toCompleteUpdated(currentValue: Option[T]): Option[T] = currentValue
  }
}
