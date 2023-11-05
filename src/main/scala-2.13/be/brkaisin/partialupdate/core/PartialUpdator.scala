package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable
import magnolia._

import scala.language.experimental.macros

trait PartialUpdator[P] {

  def updated[C](partial: P, currentComplete: C): C

}

object PartialUpdator {
  type Typeclass[P] = PartialUpdator[P]

  implicit def partialFieldUpdator[T]: PartialUpdator[PartialField[T]] =
    new PartialUpdator[PartialField[T]] {
      def updated[C](partial: PartialField[T], currentComplete: C): C =
        partial.toCompleteUpdated(currentComplete.asInstanceOf[T]).asInstanceOf[C]
    }

  implicit def partialOptionalFieldUpdator[T]: PartialUpdator[PartialOptionalField[T]] =
    new PartialUpdator[PartialOptionalField[T]] {
      def updated[C](partial: PartialOptionalField[T], currentComplete: C): C =
        // C is of type Option[T], making the following casts safe
        partial.toCompleteUpdated(currentComplete.asInstanceOf[Option[T]]).asInstanceOf[C]
    }

  implicit def partialNestedFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialNestedField[T, PartialFieldType]] =
    new PartialUpdator[PartialNestedField[T, PartialFieldType]] {
      def updated[C](partial: PartialNestedField[T, PartialFieldType], currentComplete: C): C =
        partial.toCompleteUpdated(currentComplete.asInstanceOf[T]).asInstanceOf[C]
    }

  implicit def partialOptionalNestedFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialOptionalNestedField[T, PartialFieldType]] =
    new PartialUpdator[PartialOptionalNestedField[T, PartialFieldType]] {
      def updated[C](partial: PartialOptionalNestedField[T, PartialFieldType], currentComplete: C): C =
        // C is of type Option[T], we can cast it
        partial.toCompleteUpdated(currentComplete.asInstanceOf[Option[T]]).asInstanceOf[C]
    }

  implicit def partialListFieldUpdator[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialListField[Id, T, PartialFieldType]] =
    new PartialUpdator[PartialListField[Id, T, PartialFieldType]] {
      def updated[C](partial: PartialListField[Id, T, PartialFieldType], currentComplete: C): C =
        // C is of type List[T], we can cast it
        partial.toCompleteUpdated(currentComplete.asInstanceOf[List[T]]).asInstanceOf[C]
    }

  // Implementation of combine is not pure but modifies the current instance by reflection instead of creating a new
  // one (with the method "rawConstruct"). This is the only way found. For purity in your code, the argument
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
