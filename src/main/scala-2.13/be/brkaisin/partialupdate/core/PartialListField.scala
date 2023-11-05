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

  /**
    * A [[ListElemAlteration]] is an alteration that can be applied to an element of a list. An element can be added,
    * updated or deleted.
    * @tparam Id the type of the id of the elements of the list
    * @tparam T the type of the elements of the list
    * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the elements of the list
    */
  sealed trait ListElemAlteration[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]

  object ListElemAlteration {
    /* The element is added */
    final case class ElemAdded[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id, value: T)
        extends ListElemAlteration[Id, T, PartialFieldType]

    /* The element is updated */
    final case class ElemUpdated[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
        id: Id,
        value: PartialFieldType
    ) extends ListElemAlteration[Id, T, PartialFieldType]

    /* The element is deleted */
    final case class ElemDeleted[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id)
        extends ListElemAlteration[Id, T, PartialFieldType]
  }

  /* The list is updated */
  final case class ElemsUpdated[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      alterations: List[ListElemAlteration[Id, T, PartialFieldType]]
  ) extends PartialListField[Id, T, PartialFieldType] {
    @throws[IllegalArgumentException]("if an element is not in the list when updating or deleting it")
    def toCompleteUpdated(currentValue: List[T]): List[T] =
      alterations.foldLeft(currentValue) {
        case (currentCompleteValuesAcc, alteration) =>
          alteration match {
            case ListElemAlteration.ElemAdded(id, elem) =>
              if (id != elem.id)
                throw new IllegalArgumentException(
                  s"Cannot add element with id ${elem.id} because it is different from the id $id"
                )
              // add the new element
              currentCompleteValuesAcc :+ elem
            case ListElemAlteration.ElemUpdated(id, partialValue) =>
              // update the element
              val index = currentCompleteValuesAcc.indexWhere(_.id == id)
              if (index < 0)
                throw new IllegalArgumentException(
                  s"Cannot update element with id $id because it was not found in the list"
                )
              currentCompleteValuesAcc.updated(
                index,
                partialValue.toCompleteUpdated(currentCompleteValuesAcc(index))
              )
            case ListElemAlteration.ElemDeleted(id) =>
              // delete the element
              currentCompleteValuesAcc.filter(_.id != id)
          }
      }
  }

  /* The list is reordered */
  final case class ElemsReordered[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](newOrder: List[Id])
      extends PartialListField[Id, T, PartialFieldType] {
    @throws[IllegalArgumentException]("if an element is not in the list when reordering it")
    def toCompleteUpdated(currentValue: List[T]): List[T] =
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
    def toCompleteUpdated(currentValue: List[T]): List[T] = currentValue
  }

}
