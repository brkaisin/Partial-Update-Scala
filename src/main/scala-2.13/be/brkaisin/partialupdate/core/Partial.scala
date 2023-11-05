package be.brkaisin.partialupdate.core

/**
  * [[Partial]] is the core trait of the library. It represents a partial update of a value of type T.
  * You can also see it as a patch that can be applied to a value of type T.
  * @tparam T the type of the value to update
  */
trait Partial[T] {
  def applyPartialUpdate(currentValue: T): T
}
