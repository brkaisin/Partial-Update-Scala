package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable

// todo: since the ID is in T, it could be modified... write a new PartialList that separates T and Id
sealed trait PartialListField[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]] extends Partial[List[T]]

object PartialListField {

  sealed trait ListElemAlteration[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]

  object ListElemAlteration {
    // todo: here, id is redundant since it is in T
    final case class ElemAdded[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id, value: T)
        extends ListElemAlteration[Id, T, PartialFieldType]

    final case class ElemUpdated[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
        id: Id,
        value: PartialFieldType
    ) extends ListElemAlteration[Id, T, PartialFieldType]

    final case class ElemDeleted[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id)
        extends ListElemAlteration[Id, T, PartialFieldType]
  }

  final case class ElemsUpdated[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      alterations: List[ListElemAlteration[Id, T, PartialFieldType]]
  ) extends PartialListField[Id, T, PartialFieldType] {
    @throws[IllegalArgumentException]("if an element is not in the list when updating or deleting it")
    def toCompleteUpdated(currentValue: List[T]): List[T] = {
      val updatedCompleteValues = alterations.foldLeft(currentValue) {
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
      updatedCompleteValues
    }
  }

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

  final case class Unchanged[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]()
      extends PartialListField[Id, T, PartialFieldType] {
    def toCompleteUpdated(currentValue: List[T]): List[T] = currentValue
  }

}
