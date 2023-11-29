package be.brkaisin.partialupdate.diff

import be.brkaisin.partialupdate.core._
import be.brkaisin.partialupdate.util.Identifiable

object Implicits {
  implicit val dummy: DummyImplicit =
    DummyImplicit.dummyImplicit // to prevent IJ from removing the import of Implicits._

  @throws[IllegalStateException]("if the immutable field has been updated")
  implicit def immutableFieldDiffComputor[T]: PartialDiffComputor[T, PartialImmutableField[T]] =
    (currentValue: T, newValue: T) =>
      if (currentValue == newValue) PartialImmutableField.Unchanged()
      else throw new IllegalStateException("Immutable field has been updated")

  implicit def partialFieldDiffComputor[T]: PartialDiffComputor[T, PartialField[T]] =
    (currentValue: T, newValue: T) =>
      if (currentValue == newValue) PartialField.Unchanged()
      else PartialField.Updated(newValue)

  implicit def partialNestedFieldDiffComputor[T, PartialFieldType <: Partial[T]](implicit
      partialDiffComputor: PartialDiffComputor[T, PartialFieldType]
  ): PartialDiffComputor[T, PartialNestedField[T, PartialFieldType]] =
    (currentValue: T, newValue: T) =>
      if (currentValue == newValue) PartialNestedField.Unchanged()
      else PartialNestedField.Updated(partialDiffComputor.computePartialDiff(currentValue, newValue))

  implicit def partialOptionalFieldDiffComputor[T, PartialFieldType <: Partial[T]](implicit
      partialDiffComputor: PartialDiffComputor[T, PartialFieldType]
  ): PartialDiffComputor[Option[T], PartialOptionalField[T, PartialFieldType]] =
    (currentValue: Option[T], newValue: Option[T]) =>
      (currentValue, newValue) match {
        case (Some(currentValue), Some(newValue)) if currentValue != newValue =>
          PartialOptionalField.Updated(partialDiffComputor.computePartialDiff(currentValue, newValue))
        case (Some(_), None) =>
          PartialOptionalField.Deleted()
        case (None, Some(newValue)) =>
          PartialOptionalField.Set(newValue)
        case _ => PartialOptionalField.Unchanged()
      }

  implicit def partialListFieldDiffComputor[T, PartialFieldType <: Partial[T]]
      : PartialDiffComputor[List[T], PartialListField[T, PartialFieldType]] =
    (currentValue: List[T], newValue: List[T]) =>
      if (currentValue == newValue) PartialListField.Unchanged()
      else if (currentValue.toSet == newValue.toSet)
        // list is reordered
        PartialListField.ElemsReordered(currentValue.map(newValue.indexOf))
      else {
        // list is updated
        val newElems     = newValue.toSet.diff(currentValue.toSet)
        val deletedElems = currentValue.toSet.diff(newValue.toSet)
        // since items are not identifiable, it is not possible to know which element has been updated
        // so we only consider additions and deletions
        val operations = deletedElems.toList.map { elem =>
          ListOperation.ElemDeleted[T, PartialFieldType](elem)
        } ++ newElems.toList.map { elem =>
          ListOperation.ElemAdded[T, PartialFieldType](Some(newValue.indexOf(elem)), elem)
        }
        PartialListField.ElemsUpdated(operations)
      }

  implicit def partialIdentifiableListFieldDiffComputor[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      implicit partialDiffComputor: PartialDiffComputor[T, PartialFieldType]
  ): PartialDiffComputor[List[T], PartialIdentifiableListField[Id, T, PartialFieldType]] =
    (currentValue: List[T], newValue: List[T]) =>
      if (currentValue == newValue) PartialIdentifiableListField.Unchanged()
      else if (currentValue.toSet == newValue.toSet)
        // list is reordered
        PartialIdentifiableListField.ElemsReordered(newValue.map(_.id))
      else {
        // list is updated
        val newElems = newValue.filter(newItem => !currentValue.exists(_.id == newItem.id))
        val updatedElems: List[(T, T)] = for {
          oldItem <- currentValue
          newItem <- newValue.find(_.id == oldItem.id)
          if oldItem != newItem
        } yield (oldItem, newItem)

        val deletedElemsIds = currentValue.map(_.id).diff(newValue.map(_.id))

        val operations = updatedElems.map {
          case (oldItem, newItem) =>
            IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType](
              oldItem.id,
              partialDiffComputor.computePartialDiff(oldItem, newItem)
            )
        } ++ newElems.map { elem =>
          IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType](elem.id, elem)
        } ++ deletedElemsIds.map { elemId =>
          IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType](elemId)
        }
        PartialIdentifiableListField.ElemsUpdated(operations)
      }

}
