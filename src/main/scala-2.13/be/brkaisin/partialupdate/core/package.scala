package be.brkaisin.partialupdate

import be.brkaisin.partialupdate.util.Identifiable

package object core {
  // type aliases
  type SimplePartialOptionalField[T] = PartialOptionalField[T, PartialField[T]]

  // todo: test it
  type SimplePartialListField[Id, T <: Identifiable[T, Id]] = PartialListField[Id, T, PartialField[T]]

  type PartialOptionalListField[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]] =
    PartialOptionalField[List[T], PartialListField[Id, T, PartialFieldType]]
}
