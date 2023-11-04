package be.brkaisin.partialupdate.core

trait Partial[T] {
  def toCompleteUpdated(currentValue: T): T
}
