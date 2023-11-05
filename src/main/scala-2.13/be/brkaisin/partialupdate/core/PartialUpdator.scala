package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable
import magnolia._

import scala.language.experimental.macros

/**
  * [[PartialUpdator]] provides the ability to update a value of type T with a [[Partial]].
  * @tparam P the type of the [[Partial]]. The reason why the type of the complete value
  * is not declared as a type parameter is because magnolia does not support type-class derivation
  * with more than one type parameter.
  */
trait PartialUpdator[P] {

  /**
    * Update a value of type T with a [[Partial]].
    * @param partial the [[Partial]] to apply
    * @param currentComplete the value to update
    * @tparam C the type of the value to update
    * @return the updated value
    */
  def updated[C](partial: P, currentComplete: C): C

}

object PartialUpdator {
  type Typeclass[P] = PartialUpdator[P]

  implicit def partialFieldUpdator[T]: PartialUpdator[PartialField[T]] =
    new PartialUpdator[PartialField[T]] {
      def updated[C](partial: PartialField[T], currentComplete: C): C =
        // C is of type T
        partial.toCompleteUpdated(currentComplete.asInstanceOf[T]).asInstanceOf[C]
    }

  implicit def partialNestedFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialNestedField[T, PartialFieldType]] =
    new PartialUpdator[PartialNestedField[T, PartialFieldType]] {
      def updated[C](partial: PartialNestedField[T, PartialFieldType], currentComplete: C): C =
        // C is of type T
        partial.toCompleteUpdated(currentComplete.asInstanceOf[T]).asInstanceOf[C]
    }

  implicit def partialOptionalFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialOptionalField[T, PartialFieldType]] =
    new PartialUpdator[PartialOptionalField[T, PartialFieldType]] {
      def updated[C](partial: PartialOptionalField[T, PartialFieldType], currentComplete: C): C =
        // C is of type Option[T]
        partial.toCompleteUpdated(currentComplete.asInstanceOf[Option[T]]).asInstanceOf[C]
    }

  implicit def partialListFieldUpdator[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialListField[Id, T, PartialFieldType]] =
    new PartialUpdator[PartialListField[Id, T, PartialFieldType]] {
      def updated[C](partial: PartialListField[Id, T, PartialFieldType], currentComplete: C): C =
        // C is of type List[T]
        partial.toCompleteUpdated(currentComplete.asInstanceOf[List[T]]).asInstanceOf[C]
    }

  // Implementation of combine is not pure but modifies the current instance by reflection instead of creating a new
  // one. This is the only way found because magnolia does not support type-class derivation with more than one type
  // parameter. Here, we do not construct an instance of [[P]] but we rather use the instance of [[P]] to update the
  // current instance of [[C]], for which we do not have a "rawConstruct" method. For purity in your code, the argument
  // "currentComplete" of method [[PartialUpdator.updated]] must be a copy of the current instance.
  def combine[P](caseClass: CaseClass[Typeclass, P]): Typeclass[P] =
    new PartialUpdator[P] {
      def updated[C](partial: P, currentComplete: C): C =
        caseClass.parameters.foldLeft(currentComplete) { (currentCompleteAcc, param) =>
          // copy currentComplete to avoid modifying it
          val paramValuePartial = param.dereference(partial)

          // Using reflection to get and set the value in the complete object
          val fieldComplete = currentCompleteAcc.getClass.getDeclaredField(param.label)
          fieldComplete.setAccessible(true) // because the field is "private final" in the case class
          val paramValueComplete = fieldComplete.get(currentCompleteAcc)

          val completedValue = param.typeclass.updated(paramValuePartial, paramValueComplete)

          fieldComplete.set(currentCompleteAcc, completedValue)
          currentCompleteAcc
        }
    }

  implicit def gen[P]: Typeclass[P] = macro Magnolia.gen[P]

  def apply[P](implicit updator: PartialUpdator[P]): PartialUpdator[P] = updator
}
