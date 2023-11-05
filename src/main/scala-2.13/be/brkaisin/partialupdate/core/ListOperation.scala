package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable

/**
  * A [[ListOperation]] is a basic operation on a list. An element can be added, updated or deleted.
  * @tparam Id               the type of the id of the elements of the list
  * @tparam T                the type of the elements of the list
  * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the elements of the list
  */
sealed trait ListOperation[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]

object ListOperation {
  /* The element is added */
  final case class ElemAdded[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id, value: T)
      extends ListOperation[Id, T, PartialFieldType]

  /* The element is updated */
  final case class ElemUpdated[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      id: Id,
      value: PartialFieldType
  ) extends ListOperation[Id, T, PartialFieldType]

  /* The element is deleted */
  final case class ElemDeleted[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id)
      extends ListOperation[Id, T, PartialFieldType]
}
