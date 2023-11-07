package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.util.Identifiable
import magnolia1._

import scala.language.experimental.macros

/**
  * [[PartialUpdator]] provides the ability to partially update a value of type C with a [[Partial]].
  * The reason why the type of the complete value is not declared as a type parameter is because magnolia
  * does not support type-class derivation with more than one type parameter. It is also impossible to state
  * that P <:< Partial[C] in the type class definition because of magnolia limitations.
  * @tparam P the type of the [[Partial]].
  */
trait PartialUpdator[P] {

  /**
    * Update a value of type C with a [[Partial]] of type P.
    * @param partial the [[Partial]] to apply
    * @param currentComplete the value to partially update
    * @tparam C the type of the value to partially update
    * @return the updated value
    */
  def updated[C](partial: P, currentComplete: C)(implicit ev: P <:< Partial[C]): C

}

object PartialUpdator {
  type Typeclass[P] = PartialUpdator[P]
  private def partialUpdate[C, P <: Partial[C]](partial: P, currentComplete: C): C =
    partial.applyPartialUpdate(currentComplete)

  implicit def partialFieldUpdator[T]: PartialUpdator[PartialField[T]] =
    new PartialUpdator[PartialField[T]] {
      def updated[C](partial: PartialField[T], currentComplete: C)(implicit ev: PartialField[T] <:< Partial[C]): C =
        partialUpdate(ev(partial), currentComplete)
    }

  implicit def partialNestedFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialNestedField[T, PartialFieldType]] =
    new PartialUpdator[PartialNestedField[T, PartialFieldType]] {
      def updated[C](partial: PartialNestedField[T, PartialFieldType], currentComplete: C)(implicit
          ev: PartialNestedField[T, PartialFieldType] <:< Partial[C]
      ): C = partialUpdate(ev(partial), currentComplete)
    }

  implicit def partialOptionalFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialOptionalField[T, PartialFieldType]] =
    new PartialUpdator[PartialOptionalField[T, PartialFieldType]] {
      def updated[C](partial: PartialOptionalField[T, PartialFieldType], currentComplete: C)(implicit
          ev: PartialOptionalField[T, PartialFieldType] <:< Partial[C]
      ): C = partialUpdate(ev(partial), currentComplete)
    }

  implicit def partialListFieldUpdator[T, PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialListField[T, PartialFieldType]] =
    new PartialUpdator[PartialListField[T, PartialFieldType]] {
      def updated[C](partial: PartialListField[T, PartialFieldType], currentComplete: C)(implicit
          ev: PartialListField[T, PartialFieldType] <:< Partial[C]
      ): C = partialUpdate(ev(partial), currentComplete)
    }

  implicit def partialIdentifiableListFieldUpdator[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]]
      : PartialUpdator[PartialIdentifiableListField[Id, T, PartialFieldType]] =
    new PartialUpdator[PartialIdentifiableListField[Id, T, PartialFieldType]] {
      def updated[C](partial: PartialIdentifiableListField[Id, T, PartialFieldType], currentComplete: C)(implicit
          ev: PartialIdentifiableListField[Id, T, PartialFieldType] <:< Partial[C]
      ): C = partialUpdate(ev(partial), currentComplete)
    }

  // Implementation of join is not pure but modifies the current instance by reflection instead of creating a new
  // one. This is the only way found because magnolia does not support type-class derivation with more than one type
  // parameter. Here, we do not construct an instance of P but we rather use the instance of P to partially update
  // the current instance of C in place because we do not have a "rawConstruct" method for C. For purity in your code,
  // the argument "currentComplete" of method "PartialUpdator.updated" must be a copy of the current instance.
  def join[P](caseClass: CaseClass[Typeclass, P]): Typeclass[P] =
    new PartialUpdator[P] {
      def updated[C](partial: P, currentComplete: C)(implicit ev: P <:< Partial[C]): C =
        caseClass.parameters.foldLeft(currentComplete) { (currentCompleteAcc, param) =>
          val paramValuePartial = param.dereference(partial)

          // Using reflection to get and set the value in the complete object
          val fieldComplete = currentCompleteAcc.getClass.getDeclaredField(param.label)
          fieldComplete.setAccessible(true) // because the field is "private final" in the case class
          val paramValueComplete = fieldComplete.get(currentCompleteAcc)

          // dummy proof that PType is a Partial[AnyRef], which is a necessary condition for the type class derivation to work
          implicit val dummy: param.PType <:< Partial[AnyRef] = ev.asInstanceOf[param.PType <:< Partial[AnyRef]]
          val completedValue                                  = param.typeclass.updated(paramValuePartial, paramValueComplete)

          fieldComplete.set(currentCompleteAcc, completedValue)
          currentCompleteAcc
        }
    }

  implicit def gen[P]: Typeclass[P] = macro Magnolia.gen[P]

  def apply[P](implicit updator: PartialUpdator[P]): PartialUpdator[P] = updator

}
