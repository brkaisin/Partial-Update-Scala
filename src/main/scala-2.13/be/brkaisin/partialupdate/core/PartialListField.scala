package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable

// todo: since the ID is in T, it could be modified... write a new PartialList that separates T and Id
sealed trait PartialListField[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]

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
  ) extends PartialListField[Id, T, PartialFieldType]

  final case class ElemsReordered[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](newOrder: List[Id])
      extends PartialListField[Id, T, PartialFieldType]

  final case object Unchanged extends PartialListField[Nothing, Nothing, Nothing]

  def unchanged[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]
      : PartialListField[Id, T, PartialFieldType] =
    // the casting is necessary because it is impossible to make T covariant in the trait definition
    // because it appears in an invariant position in trait Partial[T]
    Unchanged.asInstanceOf[PartialListField[Id, T, PartialFieldType]]
}
