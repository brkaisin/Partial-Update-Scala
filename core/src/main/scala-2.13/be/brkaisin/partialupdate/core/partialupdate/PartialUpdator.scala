package be.brkaisin.partialupdate.core.partialupdate

import be.brkaisin.partialupdate.core._

import scala.language.experimental.macros

/**
  * * [[PartialUpdator]] provides the ability to partially update a value of type T with a [[Partial]] of type PartialType.
  * @tparam T the type of the value to update
  * @tparam PartialType the type of the partial update
  */
trait PartialUpdator[T, PartialType <: Partial[T]] {

  /**
    * Update the value of type T with the partial update of type PartialType
    * @param currentValue the value to update
    * @param partial the partial update
    * @return the updated value
    */
  def update(currentValue: T, partial: PartialType): T

}
