package be.brkaisin.partialupdate.core

sealed trait PartialOptionalField[+T]

object PartialOptionalField {

  final case class Updated[+T](value: T) extends PartialOptionalField[T]

  final case object Unchanged extends PartialOptionalField[Nothing]

  final case object Deleted extends PartialOptionalField[Nothing]
}
