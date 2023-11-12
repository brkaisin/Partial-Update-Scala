package be.brkaisin.partialupdate.core

/**
  * A [[ListOperation]] is a basic operation on a list. An element can be added, updated or deleted. Every other
  * operation (swapping, moving, replacing, etc.) can be expressed as a combination of these three operations.
  *
  * @tparam T                the type of the elements of the list
  * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the elements of the list
  */
sealed trait ListOperation[T, PartialFieldType <: Partial[T]]

object ListOperation {
  /* The element is added */
  final case class ElemAdded[T, PartialFieldType <: Partial[T]](maybeIndex: Option[Int], value: T)
      extends ListOperation[T, PartialFieldType]

  /* The element is updated */
  final case class ElemUpdated[T, PartialFieldType <: Partial[T]](index: Int, value: PartialFieldType)
      extends ListOperation[T, PartialFieldType]

  /* The element is deleted */
  final case class ElemDeleted[T, PartialFieldType <: Partial[T]](index: Int) extends ListOperation[T, PartialFieldType]
}
