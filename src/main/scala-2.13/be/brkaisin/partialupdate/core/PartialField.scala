package be.brkaisin.partialupdate.core

sealed trait PartialField[+T]

object PartialField {

  final case class Updated[+T](value: T) extends PartialField[T]

  final case object Unchanged extends PartialField[Nothing]

}
