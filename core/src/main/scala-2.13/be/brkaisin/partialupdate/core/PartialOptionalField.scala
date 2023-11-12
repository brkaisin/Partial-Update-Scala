package be.brkaisin.partialupdate.core

/**
  * A [[PartialOptionalField]] is a [[Partial]] that can be applied to an optional field of a case class,
  * i.e. a field that is of type Option[T]. It can be used to set the value of the field, to update it,
  * to delete it (set it to [[None]]), or to not update it.
  * @tparam T the type of the value to update
  * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the field
  */
sealed trait PartialOptionalField[T, PartialFieldType <: Partial[T]] extends Partial[Option[T]]

object PartialOptionalField {

  /* The value is set */
  final case class Set[T, PartialFieldType <: Partial[T]](value: T) extends PartialOptionalField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if the value is already set")
    def applyPartialUpdate(currentValue: Option[T]): Option[T] =
      currentValue match {
        case Some(_) => throw new IllegalArgumentException("Cannot set a value that is already set.")
        case None    => Some(value)
      }
  }

  /* The value is updated */
  final case class Updated[T, PartialFieldType <: Partial[T]](value: PartialFieldType)
      extends PartialOptionalField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if the value is not set")
    def applyPartialUpdate(currentValue: Option[T]): Option[T] = {
      val updatedComplete = value.applyPartialUpdate {
        currentValue match {
          case Some(innerValue) => innerValue
          case None =>
            throw new IllegalArgumentException("Cannot update a value that is not set.")
        }
      }
      Some(updatedComplete)
    }
  }

  /* The value is deleted */
  final case class Deleted[T, PartialFieldType <: Partial[T]]() extends PartialOptionalField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if the value is not set")
    def applyPartialUpdate(currentValue: Option[T]): Option[T] =
      currentValue match {
        case Some(_) => None
        case None    => throw new IllegalArgumentException("Cannot delete a value that is not set.")
      }
  }

  /* The value is not updated */
  final case class Unchanged[T, PartialFieldType <: Partial[T]]() extends PartialOptionalField[T, PartialFieldType] {
    def applyPartialUpdate(currentValue: Option[T]): Option[T] = currentValue
  }
}
