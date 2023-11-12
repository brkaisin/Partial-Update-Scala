package be.brkaisin.partialupdate.core

/**
  * A [[PartialNestedField]] is a [[Partial]] that can be applied to a non-leaf field of a case class,
  *  i.e. a field that is not a primitive type, but a case class or a collection for example.
  * It can be used to update a field of a case class, or to not update it.
  * @tparam T the type of the value to update
  * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the field
  */
sealed trait PartialNestedField[T, PartialFieldType <: Partial[T]] extends Partial[T]

object PartialNestedField {
  /* The value is updated */
  final case class Updated[T, PartialFieldType <: Partial[T]](value: PartialFieldType)
      extends PartialNestedField[T, PartialFieldType] {
    def applyPartialUpdate(currentValue: T): T = value.applyPartialUpdate(currentValue)
  }

  /* The value is not updated */
  final case class Unchanged[T, PartialFieldType <: Partial[T]]() extends PartialNestedField[T, PartialFieldType] {
    def applyPartialUpdate(currentValue: T): T = currentValue
  }
}
