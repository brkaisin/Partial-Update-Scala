package be.brkaisin.partialupdate.core

sealed trait PartialNestedField[T, PartialFieldType <: Partial[T]] extends Partial[T]

object PartialNestedField {
  final case class Updated[T, PartialFieldType <: Partial[T]](partial: PartialFieldType)
      extends PartialNestedField[T, PartialFieldType] {
    def toCompleteUpdated(currentValue: T): T = partial.toCompleteUpdated(currentValue)
  }

  final case class Unchanged[T, PartialFieldType <: Partial[T]]() extends PartialNestedField[T, PartialFieldType] {
    def toCompleteUpdated(currentValue: T): T = currentValue
  }
}
