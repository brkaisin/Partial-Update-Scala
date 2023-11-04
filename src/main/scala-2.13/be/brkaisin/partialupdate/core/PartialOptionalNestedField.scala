package be.brkaisin.partialupdate.core

sealed trait PartialOptionalNestedField[T, PartialFieldType <: Partial[T]]

object PartialOptionalNestedField {

  final case class Set[T, PartialFieldType <: Partial[T]](value: T)
      extends PartialOptionalNestedField[T, PartialFieldType]

  final case class Updated[T, PartialFieldType <: Partial[T]](value: PartialFieldType)
      extends PartialOptionalNestedField[T, PartialFieldType]

  final case object Deleted extends PartialOptionalNestedField[Nothing, Nothing]

  final case object Unchanged extends PartialOptionalNestedField[Nothing, Nothing]

  def deleted[T, PartialFieldType <: Partial[T]]: PartialOptionalNestedField[T, PartialFieldType] =
    // the casting is necessary because it is impossible to make T covariant in the trait definition
    // because it appears in an invariant position in trait Partial[T]
    Deleted.asInstanceOf[PartialOptionalNestedField[T, PartialFieldType]]

  def unchanged[T, PartialFieldType <: Partial[T]]: PartialOptionalNestedField[T, PartialFieldType] =
    // same comment as above
    Unchanged.asInstanceOf[PartialOptionalNestedField[T, PartialFieldType]]
}
