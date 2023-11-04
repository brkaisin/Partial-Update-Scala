package be.brkaisin.partialupdate.core

sealed trait PartialNestedField[T, PartialFieldType <: Partial[T]]

object PartialNestedField {
  final case class Updated[T, PartialFieldType <: Partial[T]](partial: PartialFieldType)
      extends PartialNestedField[T, PartialFieldType]

  final case object Unchanged extends PartialNestedField[Nothing, Nothing]

  def unchanged[T, PartialFieldType <: Partial[T]]: PartialNestedField[T, PartialFieldType] =
    // the casting is necessary because it is impossible to make T covariant in the trait definition
    // because it appears in an invariant position in trait Partial[T]
    Unchanged.asInstanceOf[PartialNestedField[T, PartialFieldType]]
}
