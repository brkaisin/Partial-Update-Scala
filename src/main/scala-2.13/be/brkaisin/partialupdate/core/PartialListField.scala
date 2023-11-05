package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable

/**
  * A [[PartialListField]] is a [[Partial]] that can be applied to a list of values of type T (which are
  * [[Identifiable]]). It can be used to update a list of values, reorder it, or to not update it.
  * See the comments throughout the code for more details.
  * @tparam Id the type of the id of the elements of the list
  * @tparam T the type of the elements of the list
  * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the elements of the list
  */
sealed trait PartialListField[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]] extends Partial[List[T]]

object PartialListField {

  /* The list is updated */
  final case class ElemsUpdated[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      operations: List[ListOperation[Id, T, PartialFieldType]]
  ) extends PartialListField[Id, T, PartialFieldType] {
    @throws[IllegalArgumentException]("if an element is not in the list when updating or deleting it")
    def applyPartialUpdate(currentValue: List[T]): List[T] =
      operations.foldLeft(currentValue) {
        case (currentCompleteValuesAcc, operation) =>
          operation match {
            case ListOperation.ElemAdded(id, elem) =>
              if (id != elem.id)
                throw new IllegalArgumentException(
                  s"Cannot add element with id ${elem.id} because it is different from the id $id"
                )
              // add the new element
              currentCompleteValuesAcc :+ elem
            case ListOperation.ElemUpdated(id, partialValue) =>
              val index = currentCompleteValuesAcc.indexWhere(_.id == id)
              if (index < 0)
                throw new IllegalArgumentException(
                  s"Cannot update element with id $id because it was not found in the list"
                )
              // update the element
              currentCompleteValuesAcc.updated(
                index,
                partialValue.applyPartialUpdate(currentCompleteValuesAcc(index))
              )
            case ListOperation.ElemDeleted(id) =>
              val index = currentCompleteValuesAcc.indexWhere(_.id == id)
              if (index < 0)
                throw new IllegalArgumentException(
                  s"Cannot delete element with id $id because it was not found in the list"
                )
              // delete the element
              currentCompleteValuesAcc.patch(index, Nil, 1)
          }
      }
  }

  /* The list is reordered */
  final case class ElemsReordered[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](newOrder: List[Id])
      extends PartialListField[Id, T, PartialFieldType] {
    @throws[IllegalArgumentException]("if an element is not in the list when reordering it")
    def applyPartialUpdate(currentValue: List[T]): List[T] =
      // reorder the list, but fail if an Id is not found in the current list
      newOrder.map { id =>
        currentValue
          .find(_.id == id)
          .getOrElse(
            throw new IllegalArgumentException(
              s"Cannot reorder element with id $id because it was not found in the list"
            )
          )
      }
  }

  /* The list is not updated */
  final case class Unchanged[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]()
      extends PartialListField[Id, T, PartialFieldType] {
    def applyPartialUpdate(currentValue: List[T]): List[T] = currentValue
  }

}
