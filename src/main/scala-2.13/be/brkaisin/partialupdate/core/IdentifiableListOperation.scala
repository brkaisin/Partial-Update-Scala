package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable

/**
  * A [[IdentifiableListOperation]] is a basic operation on a list of identifiable elements. An element can be added,
  * updated or deleted.
  * @tparam Id               the type of the id of the elements of the list
  * @tparam T                the type of the elements of the list
  * @tparam PartialFieldType the type of the [[Partial]] that can be applied to the elements of the list
  */
sealed trait IdentifiableListOperation[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]

object IdentifiableListOperation {
  /* The element is added */
  final case class ElemAdded[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id, value: T)
      extends IdentifiableListOperation[Id, T, PartialFieldType]

  /* The element is updated */
  final case class ElemUpdated[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      id: Id,
      value: PartialFieldType
  ) extends IdentifiableListOperation[Id, T, PartialFieldType]

  /* The element is deleted */
  final case class ElemDeleted[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](id: Id)
      extends IdentifiableListOperation[Id, T, PartialFieldType]
}
