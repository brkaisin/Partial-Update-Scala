package be.brkaisin.partialupdate.circe

import be.brkaisin.partialupdate.core._
import be.brkaisin.partialupdate.util.Identifiable
import io.circe.Decoder.Result
import io.circe.DecodingFailure.Reason
import io.circe.DecodingFailure.Reason.{CustomReason, MissingField}
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

import scala.reflect.ClassTag

/**
  * This file contains somewhat opinionated circe codecs for the partial update library.
  * It is possible to use the library with other custom codecs, or with part of the codecs defined here
  * and part of your own codecs.
  * */
object CirceCodecs {
  /* Immutable field */
  implicit def partialImmutableFieldDecoder[T]: Decoder[PartialImmutableField[T]] =
    new Decoder[PartialImmutableField[T]] {
      def apply(c: HCursor): Result[PartialImmutableField[T]] = tryDecode(c)

      final override def tryDecode(c: ACursor): Decoder.Result[PartialImmutableField[T]] =
        c match {
          case _: FailedCursor => Right(PartialImmutableField.Unchanged())
        }
    }

  implicit def partialImmutableFieldEncoder[T]: Encoder[PartialImmutableField[T]] = {
    case PartialImmutableField.Unchanged() => Json.obj() // this value should be dropped by the outside encoder
  }

  /* Partial field */
  implicit def partialFieldDecoder[T: Decoder]: Decoder[PartialField[T]] =
    new Decoder[PartialField[T]] {
      def apply(c: HCursor): Result[PartialField[T]] = tryDecode(c)

      final override def tryDecode(c: ACursor): Decoder.Result[PartialField[T]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else c.as[T].map(PartialField.Updated(_))
          case _: FailedCursor => Right(PartialField.Unchanged())
        }
    }

  implicit def partialFieldEncoder[T: Encoder]: Encoder[PartialField[T]] = {
    case PartialField.Updated(value) => value.asJson
    case PartialField.Unchanged()    => Json.obj() // this value should be dropped by the outside encoder
  }

  /* Partial nested field */
  implicit def partialNestedFieldDecoder[T, PartialFieldType <: Partial[T]: Decoder]
      : Decoder[PartialNestedField[T, PartialFieldType]] =
    new Decoder[PartialNestedField[T, PartialFieldType]] {
      def apply(c: HCursor): Result[PartialNestedField[T, PartialFieldType]] = tryDecode(c)

      final override def tryDecode(c: ACursor): Decoder.Result[PartialNestedField[T, PartialFieldType]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else c.as[PartialFieldType].map(PartialNestedField.Updated(_))
          case _: FailedCursor => Right(PartialNestedField.Unchanged())
        }
    }

  implicit def partialNestedFieldEncoder[T, PartialFieldType <: Partial[T]: Encoder]
      : Encoder[PartialNestedField[T, PartialFieldType]] = {
    case PartialNestedField.Updated(value) => value.asJson
    case PartialNestedField.Unchanged()    => Json.obj() // this value should be dropped by the outside encoder
  }

  /* Partial optional field */
  implicit def partialOptionalFieldDecoder[T: Decoder, PartialFieldType <: Partial[T]: Decoder]
      : Decoder[PartialOptionalField[T, PartialFieldType]] =
    new Decoder[PartialOptionalField[T, PartialFieldType]] {
      def apply(c: HCursor): Result[PartialOptionalField[T, PartialFieldType]] = tryDecode(c)

      final override def tryDecode(c: ACursor): Decoder.Result[PartialOptionalField[T, PartialFieldType]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Right(PartialOptionalField.Deleted[T, PartialFieldType]())
            else
              c.downField("initialValue").as[T] match {
                case Right(initialValue) =>
                  Right(PartialOptionalField.Set(initialValue))
                case _ => c.as[PartialFieldType].map(PartialOptionalField.Updated(_))
              }
          case _: FailedCursor => Right(PartialOptionalField.Unchanged[T, PartialFieldType]())
        }
    }

  implicit def partialOptionalFieldEncoder[T: Encoder, PartialFieldType <: Partial[T]: Encoder]
      : Encoder[PartialOptionalField[T, PartialFieldType]] = {
    case PartialOptionalField.Set(value)     => Json.obj("initialValue" -> value.asJson)
    case PartialOptionalField.Updated(value) => value.asJson
    case PartialOptionalField.Deleted()      => Json.Null
    case PartialOptionalField.Unchanged()    => Json.obj() // this value should be dropped by the outside encoder
  }

  /* List operation */
  sealed trait ListOperationType {
    val name: String
  }

  private object ListOperationType {
    case object Add extends ListOperationType { val name = "add" }
    case object Update extends ListOperationType { val name = "update" }

    case object Delete extends ListOperationType {
      val name = "delete"
    }

    case object DeleteAtIndex extends ListOperationType {
      val name = "deleteAtIndex"
    }
  }

  private val listOperationKey = "operation"

  /* List operation */
  implicit def listOperationEncoder[T: Encoder, PartialFieldType <: Partial[T]: Encoder]
      : Encoder[ListOperation[T, PartialFieldType]] = {
    case operation: ListOperation.ElemAdded[T, PartialFieldType] =>
      deriveEncoder[ListOperation.ElemAdded[T, PartialFieldType]]
        .addKeyValue(listOperationKey, ListOperationType.Add.name)
        .apply(operation)
    case operation: ListOperation.ElemUpdated[T, PartialFieldType] =>
      deriveEncoder[ListOperation.ElemUpdated[T, PartialFieldType]]
        .addKeyValue(listOperationKey, ListOperationType.Update.name)
        .apply(operation)
    case operation: ListOperation.ElemDeleted[T, PartialFieldType] =>
      deriveEncoder[ListOperation.ElemDeleted[T, PartialFieldType]]
        .addKeyValue(listOperationKey, ListOperationType.Delete.name)
        .apply(operation)
    case operation: ListOperation.ElemDeletedAtIndex[T, PartialFieldType] =>
      deriveEncoder[ListOperation.ElemDeletedAtIndex[T, PartialFieldType]]
        .addKeyValue(listOperationKey, ListOperationType.DeleteAtIndex.name)
        .apply(operation)
  }

  implicit def listOperationDecoder[T: Decoder, PartialFieldType <: Partial[T]: Decoder]
      : Decoder[ListOperation[T, PartialFieldType]] =
    (c: HCursor) =>
      c.downField(listOperationKey).as[String] match {
        case Right(ListOperationType.Add.name) =>
          deriveDecoder[ListOperation.ElemAdded[T, PartialFieldType]].apply(c)
        case Right(ListOperationType.Update.name) =>
          deriveDecoder[ListOperation.ElemUpdated[T, PartialFieldType]].apply(c)
        case Right(ListOperationType.Delete.name) =>
          deriveDecoder[ListOperation.ElemDeleted[T, PartialFieldType]].apply(c)
        case Right(ListOperationType.DeleteAtIndex.name) =>
          deriveDecoder[ListOperation.ElemDeletedAtIndex[T, PartialFieldType]].apply(c)
        case Right(unknownOperation) =>
          Left(DecodingFailure(CustomReason(s"Unknown list operation type: $unknownOperation"), c.history))
        case Left(_) =>
          Left(DecodingFailure(MissingField, c.history))
      }

  /* Partial list field */
  implicit def partialListFieldDecoder[T: Decoder, PartialFieldType <: Partial[T]: Decoder]
      : Decoder[PartialListField[T, PartialFieldType]] =
    new Decoder[PartialListField[T, PartialFieldType]] {
      def apply(c: HCursor): Result[PartialListField[T, PartialFieldType]] = tryDecode(c)

      final override def tryDecode(
          c: ACursor
      ): Decoder.Result[PartialListField[T, PartialFieldType]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else
              c.as[List[ListOperation[T, PartialFieldType]]]
                .map(PartialListField.ElemsUpdated(_))
                .orElse(c.as[List[Int]].map(PartialListField.ElemsReordered(_)))
          case _: FailedCursor => Right(PartialListField.Unchanged())
        }
    }

  implicit def partialListFieldEncoder[T: Encoder, PartialFieldType <: Partial[T]: Encoder]
      : Encoder[PartialListField[T, PartialFieldType]] = {
    case PartialListField.ElemsUpdated(operations) =>
      operations.asJson
    case PartialListField.ElemsReordered(newOrder) => newOrder.asJson
    case PartialListField.Unchanged()              => Json.obj() // this value should be dropped by the outside encoder
  }

  /* Identifiable list operation */
  implicit def identifiableListOperationEncoder[Id: Encoder, T <: Identifiable[
    T,
    Id
  ]: Encoder, PartialFieldType <: Partial[
    T
  ]: Encoder]: Encoder[IdentifiableListOperation[Id, T, PartialFieldType]] = {
    case operation: IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType] =>
      deriveEncoder[IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType]]
        .addKeyValue(listOperationKey, ListOperationType.Add.name)
        .apply(operation)
    case operation: IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType] =>
      deriveEncoder[IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType]]
        .addKeyValue(listOperationKey, ListOperationType.Update.name)
        .apply(operation)
    case operation: IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType] =>
      deriveEncoder[IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType]]
        .addKeyValue(listOperationKey, ListOperationType.Delete.name)
        .apply(operation)
  }

  implicit def identifiableListOperationDecoder[Id: Decoder, T <: Identifiable[
    T,
    Id
  ]: Decoder, PartialFieldType <: Partial[
    T
  ]: Decoder]: Decoder[IdentifiableListOperation[Id, T, PartialFieldType]] =
    (c: HCursor) =>
      c.downField(listOperationKey).as[String] match {
        case Right(ListOperationType.Add.name) =>
          deriveDecoder[IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType]].apply(c)
        case Right(ListOperationType.Update.name) =>
          deriveDecoder[IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType]].apply(c)
        case Right(ListOperationType.Delete.name) =>
          deriveDecoder[IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType]].apply(c)
        case Right(unknownOperation) =>
          Left(DecodingFailure(CustomReason(s"Unknown identifiable list operation type: $unknownOperation"), c.history))
        case Left(_) =>
          Left(DecodingFailure(MissingField, c.history))
      }

  /* Partial identifiable list field */
  implicit def partialIdentifiableListFieldDecoder[Id: Decoder, T <: Identifiable[
    T,
    Id
  ]: Decoder, PartialFieldType <: Partial[
    T
  ]: Decoder]: Decoder[PartialIdentifiableListField[Id, T, PartialFieldType]] =
    new Decoder[PartialIdentifiableListField[Id, T, PartialFieldType]] {
      def apply(c: HCursor): Result[PartialIdentifiableListField[Id, T, PartialFieldType]] = tryDecode(c)

      final override def tryDecode(
          c: ACursor
      ): Decoder.Result[PartialIdentifiableListField[Id, T, PartialFieldType]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else
              c.as[List[IdentifiableListOperation[Id, T, PartialFieldType]]]
                .map(PartialIdentifiableListField.ElemsUpdated(_))
                .orElse(c.as[List[Id]].map(PartialIdentifiableListField.ElemsReordered(_)))
          case _: FailedCursor => Right(PartialIdentifiableListField.Unchanged())
        }
    }

  implicit def partialIdentifiableListFieldEncoder[Id: Encoder, T <: Identifiable[
    T,
    Id
  ]: Encoder, PartialFieldType <: Partial[
    T
  ]: Encoder]: Encoder[PartialIdentifiableListField[Id, T, PartialFieldType]] = {
    case PartialIdentifiableListField.ElemsUpdated(operations) =>
      operations.asJson
    case PartialIdentifiableListField.ElemsReordered(newOrder) => newOrder.asJson
    case PartialIdentifiableListField.Unchanged()              => Json.obj() // this value should be dropped by the outside encoder
  }

  implicit def partialEnum2FieldEncoder[T, T1 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ]: Encoder, T2 <: T: ClassTag, PartialFieldType2 <: Partial[T2]: Encoder](implicit
      tEncoder: Encoder[T]
  ): Encoder[PartialEnum2Field[T, T1, PartialFieldType1, T2, PartialFieldType2]] = {
    case PartialEnum2Field.Value1Set(value) =>
      // the encoder we have is an encoder of T, but we want one of T1. And Encoder is invariant in T. Explaining the cast.
      value.asJson(tEncoder.asInstanceOf[Encoder[T1]])
    case PartialEnum2Field.Value2Set(value) =>
      // the encoder we have is an encoder of T, but we want one of T2. And Encoder is invariant in T. Explaining the cast.
      value.asJson(tEncoder.asInstanceOf[Encoder[T2]])
    case PartialEnum2Field.Value1Updated(value) => value.asJson
    case PartialEnum2Field.Value2Updated(value) => value.asJson
    case PartialEnum2Field.Unchanged()          => Json.obj() // this value should be dropped by the outside encoder
  }

  implicit def partialEnum2FieldDecoder[T: Decoder, T1 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ]: Decoder, T2 <: T: ClassTag, PartialFieldType2 <: Partial[T2]: Decoder]
      : Decoder[PartialEnum2Field[T, T1, PartialFieldType1, T2, PartialFieldType2]] =
    new Decoder[PartialEnum2Field[T, T1, PartialFieldType1, T2, PartialFieldType2]] {
      def apply(c: HCursor): Result[PartialEnum2Field[T, T1, PartialFieldType1, T2, PartialFieldType2]] = tryDecode(c)

      final override def tryDecode(
          c: ACursor
      ): Decoder.Result[PartialEnum2Field[T, T1, PartialFieldType1, T2, PartialFieldType2]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else
              c.as[T]
                .map {
                  case t1: T1 => PartialEnum2Field.Value1Set[T, T1, PartialFieldType1, T2, PartialFieldType2](t1)
                  case t2: T2 => PartialEnum2Field.Value2Set[T, T1, PartialFieldType1, T2, PartialFieldType2](t2)
                }
                .orElse {
                  c.as[PartialFieldType1]
                    .map(PartialEnum2Field.Value1Updated[T, T1, PartialFieldType1, T2, PartialFieldType2](_))
                }
                .orElse {
                  c.as[PartialFieldType2]
                    .map(PartialEnum2Field.Value2Updated[T, T1, PartialFieldType1, T2, PartialFieldType2](_))
                }
          case _: FailedCursor => Right(PartialEnum2Field.Unchanged())
        }
    }

  /* Any partial */
  def partialCodec[P <: Partial[_]](derivedCodec: Codec[P]): Codec[P] =
    Codec.from(derivedCodec, derivedCodec.dropEmptyValues)

}
