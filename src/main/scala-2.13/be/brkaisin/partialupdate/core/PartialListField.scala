package be.brkaisin.partialupdate.core

/**
  * A [[PartialListField]] is a [[Partial]] that can be applied to a list of values of type T that can not be uniquely
  * identified. It can be used to update a list of values, reorder it, or to not update it.
  * @tparam T the type of the elements of the list
  * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the elements of the list
  */
sealed trait PartialListField[T, PartialFieldType <: Partial[T]] extends Partial[List[T]]

object PartialListField {
  /* The list is updated */
  final case class ElemsUpdated[T, PartialFieldType <: Partial[T]](
      operations: List[ListOperation[T, PartialFieldType]]
  ) extends PartialListField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if index is out of bounds when updating or deleting an element")
    def applyPartialUpdate(currentValue: List[T]): List[T] =
      operations.foldLeft(currentValue) {
        case (currentCompleteValuesAcc, operation) =>
          operation match {
            case ListOperation.ElemAdded(maybeIndex, value) =>
              // add the new element
              maybeIndex match {
                case Some(index) =>
                  currentCompleteValuesAcc.patch(index, List(value), 0)
                case None =>
                  currentCompleteValuesAcc :+ value
              }
            case ListOperation.ElemUpdated(index, value) =>
              if (index < 0 || index >= currentCompleteValuesAcc.size)
                throw new IllegalArgumentException(
                  s"Cannot update element at index $index because it is out of bounds"
                )
              // update the element
              currentCompleteValuesAcc.updated(index, value.applyPartialUpdate(currentCompleteValuesAcc(index)))
            case ListOperation.ElemDeleted(index) =>
              if (index < 0 || index >= currentCompleteValuesAcc.size)
                throw new IllegalArgumentException(
                  s"Cannot delete element at index $index because it is out of bounds"
                )
              // delete the element
              currentCompleteValuesAcc.patch(index, Nil, 1)
          }
      }
  }

  /* The list is reordered */
  final case class ElemsReordered[T, PartialFieldType <: Partial[T]](newOrder: List[Int])
      extends PartialListField[T, PartialFieldType] {
    @throws[IllegalArgumentException]("if an element is not in the list when reordering it")
    def applyPartialUpdate(currentValue: List[T]): List[T] =
      // reorder the list, but fail if newOrder is not a permutation of the indices of the list
      if (newOrder.toSet != currentValue.indices.toSet)
        throw new IllegalArgumentException(
          s"Cannot reorder the list because the new order $newOrder is not a permutation of the indices of the list"
        )
      else
        newOrder.map(currentValue)
  }

  /* The list is not updated */
  final case class Unchanged[T, PartialFieldType <: Partial[T]]() extends PartialListField[T, PartialFieldType] {
    def applyPartialUpdate(currentValue: List[T]): List[T] = currentValue
  }
}
