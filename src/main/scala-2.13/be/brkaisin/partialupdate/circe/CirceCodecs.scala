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

  /* List operations */
  sealed trait OperationType {
    val name: String
  }

  private object OperationType {
    case object Add extends OperationType { val name = "add" }
    case object Update extends OperationType { val name = "update" }
    case object Delete extends OperationType { val name = "delete" }
  }

  private val operationKey = "operation"

  implicit def identifiableListOperationEncoder[Id: Encoder, T <: Identifiable[
    T,
    Id
  ]: Encoder, PartialFieldType <: Partial[
    T
  ]: Encoder]: Encoder[IdentifiableListOperation[Id, T, PartialFieldType]] = {
    case operation: IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType] =>
      deriveEncoder[IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType]]
        .mapJsonObject(_.add(operationKey, OperationType.Add.name.asJson))(operation)
    case operation: IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType] =>
      deriveEncoder[IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType]]
        .mapJsonObject(_.add(operationKey, OperationType.Update.name.asJson))(operation)
    case operation: IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType] =>
      deriveEncoder[IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType]]
        .mapJsonObject(_.add(operationKey, OperationType.Delete.name.asJson))(operation)
  }

  implicit def identifiableListOperationDecoder[Id: Decoder, T <: Identifiable[
    T,
    Id
  ]: Decoder, PartialFieldType <: Partial[
    T
  ]: Decoder]: Decoder[IdentifiableListOperation[Id, T, PartialFieldType]] =
    (c: HCursor) =>
      c.downField(operationKey).as[String] match {
        case Right(OperationType.Add.name) =>
          deriveDecoder[IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType]].apply(c)
        case Right(OperationType.Update.name) =>
          deriveDecoder[IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType]].apply(c)
        case Right(OperationType.Delete.name) =>
          deriveDecoder[IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType]].apply(c)
        case Right(unknownOperation) =>
          Left(DecodingFailure(CustomReason(s"Unknown operation type: $unknownOperation"), c.history))
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
