package be.brkaisin.partialupdate.core
import scala.reflect.ClassTag

/**
  * [[PartialEnum2Field]] is a [[Partial]] that can be applied to a value of type T that is a sum type with two
  * possible values T1 and T2. It can be used to update the value or to not update it.
  * ClassTag's are used to fight type erasure.
  * @tparam T the type of the value to update
  * @tparam T1 the type of the first possible value of T
  * @tparam T2 the type of the second possible value of T
  * @tparam PartialFieldType1 the type of the [[Partial]] that can be applied to T1
  * @tparam PartialFieldType2 the type of the [[Partial]] that can be applied to T2
  */
sealed abstract class PartialEnum2Field[T, T1 <: T: ClassTag, T2 <: T: ClassTag, PartialFieldType1 <: Partial[
  T1
], PartialFieldType2 <: Partial[T2]]
    extends Partial[T]

object PartialEnum2Field {
  /* The value is updated to T1 */
  final case class Value1Set[T, T1 <: T: ClassTag, T2 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ], PartialFieldType2 <: Partial[T2]](
      value: T1
  ) extends PartialEnum2Field[T, T1, T2, PartialFieldType1, PartialFieldType2] {
    def applyPartialUpdate(currentValue: T): T =
      currentValue match {
        case _: T1 =>
          throw new IllegalArgumentException(s"Cannot set value 1 of $currentValue because it is already set")
        case _ => value
      }
  }

  /* The value is updated to T2 */
  final case class Value2Set[T, T1 <: T: ClassTag, T2 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ], PartialFieldType2 <: Partial[T2]](value: T2)
      extends PartialEnum2Field[T, T1, T2, PartialFieldType1, PartialFieldType2] {
    def applyPartialUpdate(currentValue: T): T =
      currentValue match {
        case _: T2 =>
          throw new IllegalArgumentException(s"Cannot set value 2 of $currentValue because it is already set")
        case _ => value
      }
  }

  /* The value is of type T1 and is updated */
  final case class Value1Updated[T, T1 <: T: ClassTag, T2 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ], PartialFieldType2 <: Partial[T2]](value: PartialFieldType1)
      extends PartialEnum2Field[T, T1, T2, PartialFieldType1, PartialFieldType2] {
    def applyPartialUpdate(currentValue: T): T =
      currentValue match {
        case value1: T1 => value.applyPartialUpdate(value1)
        case _ =>
          throw new IllegalArgumentException(s"Cannot update value 1 of $currentValue because it is not of type T1")
      }
  }

  /* The value is of type T2 and is updated */
  final case class Value2Updated[T, T1 <: T: ClassTag, T2 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ], PartialFieldType2 <: Partial[T2]](
      value: PartialFieldType2
  ) extends PartialEnum2Field[T, T1, T2, PartialFieldType1, PartialFieldType2] {
    def applyPartialUpdate(currentValue: T): T =
      currentValue match {
        case value2: T2 => value.applyPartialUpdate(value2)
        case _ =>
          throw new IllegalArgumentException(s"Cannot update value 2 of $currentValue because it is not of type T2")
      }
  }

  /* The value is not updated */
  final case class Unchanged[T, T1 <: T: ClassTag, T2 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ], PartialFieldType2 <: Partial[T2]]()
      extends PartialEnum2Field[T, T1, T2, PartialFieldType1, PartialFieldType2] {
    def applyPartialUpdate(currentValue: T): T = currentValue
  }
}
