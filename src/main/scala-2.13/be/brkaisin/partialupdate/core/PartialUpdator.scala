package be.brkaisin.partialupdate.core

import PartialListField.ListElemAlteration
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
        partial match {
          case PartialField.Updated(value) => value.asInstanceOf[C] // T is actually C
          case PartialField.Unchanged      => currentComplete
        }
    }

  implicit def partialOptionalFieldUpdator[T]: PartialUpdator[PartialOptionalField[T]] =
    new PartialUpdator[PartialOptionalField[T]] {
      def updated[C](partial: PartialOptionalField[T], currentComplete: C): C =
        // C is of type Option[T], making the following casts safe
        partial match {
          case PartialOptionalField.Updated(value) => Some(value).asInstanceOf[C]
          case PartialOptionalField.Unchanged      => currentComplete
          case PartialOptionalField.Deleted        => None.asInstanceOf[C]
        }
    }

  implicit def partialNestedFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialNestedField[T, PartialFieldType]] =
    new PartialUpdator[PartialNestedField[T, PartialFieldType]] {
      def updated[C](partial: PartialNestedField[T, PartialFieldType], currentComplete: C): C =
        partial match {
          case PartialNestedField.Updated(partialValue) =>
            partialValue.toCompleteUpdated(currentComplete.asInstanceOf[T]).asInstanceOf[C]
          case PartialNestedField.Unchanged => currentComplete
        }
    }

  @throws[IllegalArgumentException]("if a value is updated before being set")
  implicit def partialOptionalNestedFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialOptionalNestedField[T, PartialFieldType]] =
    new PartialUpdator[PartialOptionalNestedField[T, PartialFieldType]] {
      def updated[C](partial: PartialOptionalNestedField[T, PartialFieldType], currentComplete: C): C =
        // C is of type Option[T], we can cast it
        partial match {
          case PartialOptionalNestedField.Set(value) => Some(value).asInstanceOf[C]
          case PartialOptionalNestedField.Updated(partialValue) =>
            val updatedComplete = partialValue.toCompleteUpdated {
              currentComplete.asInstanceOf[Option[T]] match {
                case Some(innerValue) => innerValue
                case None =>
                  throw new IllegalArgumentException("Cannot update a deleted value. Value must be set first.")
              }
            }
            Some(updatedComplete).asInstanceOf[C]
          case PartialOptionalNestedField.Deleted   => None.asInstanceOf[C]
          case PartialOptionalNestedField.Unchanged => currentComplete
        }
    }

  @throws[IllegalArgumentException]("if an Id is not found in the current list when reordering or updating an element")
  implicit def partialListFieldUpdator[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialListField[Id, T, PartialFieldType]] =
    new PartialUpdator[PartialListField[Id, T, PartialFieldType]] {
      def updated[C](partial: PartialListField[Id, T, PartialFieldType], currentComplete: C): C =
        // C is of type List[T], we can cast it
        partial match {
          case PartialListField.ElemsUpdated(alterations) =>
            val currentCompleteValues = currentComplete.asInstanceOf[List[T]]
            val updatedCompleteValues = alterations.foldLeft(currentCompleteValues) {
              case (currentCompleteValuesAcc, alteration) =>
                alteration match {
                  case ListElemAlteration.ElemAdded(id, elem) =>
                    if (id != elem.id)
                      throw new IllegalArgumentException(
                        s"Cannot add element with id ${elem.id} because it is different from the id $id"
                      )
                    // add the new element
                    currentCompleteValuesAcc :+ elem
                  case ListElemAlteration.ElemUpdated(id, partialValue) =>
                    // update the element
                    val index = currentCompleteValuesAcc.indexWhere(_.id == id)
                    if (index < 0)
                      throw new IllegalArgumentException(
                        s"Cannot update element with id $id because it was not found in the list"
                      )
                    currentCompleteValuesAcc.updated(
                      index,
                      partialValue.toCompleteUpdated(currentCompleteValuesAcc(index))
                    )
                  case ListElemAlteration.ElemDeleted(id) =>
                    // delete the element
                    currentCompleteValuesAcc.filter(_.id != id)
                }
            }
            updatedCompleteValues.asInstanceOf[C]
          case PartialListField.ElemsReordered(newOrder) =>
            // reorder the list, but fail if an Id is not found in the current list
            val currentCompleteValues = currentComplete.asInstanceOf[List[T]]
            val updatedCompleteValues = newOrder.map { id =>
              currentCompleteValues
                .find(_.id == id)
                .getOrElse(
                  throw new IllegalArgumentException(
                    s"Cannot reorder element with id $id because it was not found in the list"
                  )
                )
            }
            updatedCompleteValues.asInstanceOf[C]
          case PartialListField.Unchanged => currentComplete
        }
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
