package be.brkaisin.partialupdate

import be.brkaisin.partialupdate.util.Identifiable

package object core {
  // type aliases
  type SimplePartialOptionalField[T] = PartialOptionalField[T, PartialField[T]]

  type SimplePartialListField[T] = PartialListField[T, PartialField[T]]

  type SimplePartialIdentifiableListField[Id, T <: Identifiable[T, Id]] =
    PartialIdentifiableListField[Id, T, PartialField[T]]

  type PartialOptionalIdentifiableListField[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]] =
    PartialOptionalField[List[T], PartialIdentifiableListField[Id, T, PartialFieldType]]

  type SimplePartialOptionalIdentifiableListField[Id, T <: Identifiable[T, Id]] =
    PartialOptionalIdentifiableListField[Id, T, PartialField[T]]
}
