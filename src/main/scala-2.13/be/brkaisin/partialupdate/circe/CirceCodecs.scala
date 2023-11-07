package be.brkaisin.partialupdate.circe

import be.brkaisin.partialupdate.core._
import be.brkaisin.partialupdate.util.Identifiable
import io.circe.Decoder.Result
import io.circe.DecodingFailure.Reason
import io.circe.DecodingFailure.Reason.{CustomReason, MissingField}
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

/**
  * This file contains circe codecs for the partial update library. It is not included in the library itself
  * because the choice of the json library is left to the user. This file provides opinionated codecs for circe.
  */
object CirceCodecs {
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
    case object Delete extends ListOperationType { val name = "delete" }
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

  /* Any partial */
  def partialCodec[P <: Partial[_]](derivedCodec: Codec[P]): Codec[P] =
    Codec.from(derivedCodec, derivedCodec.mapJson(_.dropEmptyValues))

}
