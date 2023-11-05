package be.brkaisin.partialupdate.circe

import be.brkaisin.partialupdate.core.PartialListField.ListElemAlteration
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
  implicit def partialFieldDecoder[T](implicit tDecoder: Decoder[T]): Decoder[PartialField[T]] =
    new Decoder[PartialField[T]] {
      def apply(c: HCursor): Result[PartialField[T]] = tryDecode(c)

      final override def tryDecode(c: ACursor): Decoder.Result[PartialField[T]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else tDecoder(c).map(PartialField.Updated(_))
          case _: FailedCursor => Right(PartialField.Unchanged())
        }
    }

  implicit def partialFieldEncoder[T](implicit tEncoder: Encoder[T]): Encoder[PartialField[T]] = {
    case PartialField.Updated(value) => tEncoder(value)
    case PartialField.Unchanged()    => Json.obj() // this value should be dropped by the outside encoder
  }

  /* Partial nested field */
  implicit def partialNestedFieldDecoder[T, PartialFieldType <: Partial[T]](implicit
      partialDecoder: Decoder[PartialFieldType]
  ): Decoder[PartialNestedField[T, PartialFieldType]] =
    new Decoder[PartialNestedField[T, PartialFieldType]] {
      def apply(c: HCursor): Result[PartialNestedField[T, PartialFieldType]] = tryDecode(c)

      final override def tryDecode(c: ACursor): Decoder.Result[PartialNestedField[T, PartialFieldType]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else partialDecoder(c).map(PartialNestedField.Updated(_))
          case _: FailedCursor => Right(PartialNestedField.Unchanged())
        }
    }

  implicit def partialNestedFieldEncoder[T, PartialFieldType <: Partial[T]](implicit
      partialEncoder: Encoder[PartialFieldType]
  ): Encoder[PartialNestedField[T, PartialFieldType]] = {
    case PartialNestedField.Updated(value) => partialEncoder(value)
    case PartialNestedField.Unchanged()    => Json.obj() // this value should be dropped by the outside encoder
  }

  /* Partial optional nested field */
  implicit def partialOptionalFieldDecoder[T, PartialFieldType <: Partial[T]](implicit
      tDecoder: Decoder[T],
      partialDecoder: Decoder[PartialFieldType]
  ): Decoder[PartialOptionalField[T, PartialFieldType]] =
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
                case _ => partialDecoder(c).map(PartialOptionalField.Updated(_))
              }
          case _: FailedCursor => Right(PartialOptionalField.Unchanged[T, PartialFieldType]())
        }
    }

  implicit def partialOptionalFieldEncoder[T, PartialFieldType <: Partial[T]](implicit
      tEncoder: Encoder[T],
      partialEncoder: Encoder[PartialFieldType]
  ): Encoder[PartialOptionalField[T, PartialFieldType]] = {
    case PartialOptionalField.Set(value)     => Json.obj("initialValue" -> tEncoder(value))
    case PartialOptionalField.Updated(value) => partialEncoder(value)
    case PartialOptionalField.Deleted()      => Json.Null
    case PartialOptionalField.Unchanged()    => Json.obj() // this value should be dropped by the outside encoder
  }

  /* List elem alterations */
  sealed trait OperationType {
    val name: String
  }

  private object OperationType {
    case object Add extends OperationType { val name = "add" }
    case object Update extends OperationType { val name = "update" }
    case object Delete extends OperationType { val name = "delete" }
  }

  private val operationKey = "operation"

  implicit def listElemAlterationEncoder[Id: Encoder, T <: Identifiable[T, Id]: Encoder, PartialFieldType <: Partial[
    T
  ]: Encoder]: Encoder[ListElemAlteration[Id, T, PartialFieldType]] = {
    case alteration: ListElemAlteration.ElemAdded[Id, T, PartialFieldType] =>
      deriveEncoder[ListElemAlteration.ElemAdded[Id, T, PartialFieldType]]
        .mapJsonObject(_.add(operationKey, OperationType.Add.name.asJson))(alteration)
    case alteration: ListElemAlteration.ElemUpdated[Id, T, PartialFieldType] =>
      deriveEncoder[ListElemAlteration.ElemUpdated[Id, T, PartialFieldType]]
        .mapJsonObject(_.add(operationKey, OperationType.Update.name.asJson))(alteration)
    case alteration: ListElemAlteration.ElemDeleted[Id, T, PartialFieldType] =>
      deriveEncoder[ListElemAlteration.ElemDeleted[Id, T, PartialFieldType]]
        .mapJsonObject(_.add(operationKey, OperationType.Delete.name.asJson))(alteration)
  }

  implicit def listElemAlterationDecoder[Id: Decoder, T <: Identifiable[T, Id]: Decoder, PartialFieldType <: Partial[
    T
  ]: Decoder]: Decoder[ListElemAlteration[Id, T, PartialFieldType]] =
    (c: HCursor) =>
      c.downField(operationKey).as[String] match {
        case Right(OperationType.Add.name) =>
          deriveDecoder[ListElemAlteration.ElemAdded[Id, T, PartialFieldType]].apply(c)
        case Right(OperationType.Update.name) =>
          deriveDecoder[ListElemAlteration.ElemUpdated[Id, T, PartialFieldType]].apply(c)
        case Right(OperationType.Delete.name) =>
          deriveDecoder[ListElemAlteration.ElemDeleted[Id, T, PartialFieldType]].apply(c)
        case Right(unknownOperation) =>
          Left(DecodingFailure(CustomReason(s"Unknown operation type: $unknownOperation"), c.history))
        case Left(_) =>
          Left(DecodingFailure(MissingField, c.history))
      }

  /* Partial list of identifiables field */
  implicit def partialListFieldDecoder[Id: Decoder, T <: Identifiable[T, Id]: Decoder, PartialFieldType <: Partial[
    T
  ]: Decoder]: Decoder[PartialListField[Id, T, PartialFieldType]] =
    new Decoder[PartialListField[Id, T, PartialFieldType]] {
      def apply(c: HCursor): Result[PartialListField[Id, T, PartialFieldType]] = tryDecode(c)

      final override def tryDecode(
          c: ACursor
      ): Decoder.Result[PartialListField[Id, T, PartialFieldType]] =
        c match {
          case c: HCursor =>
            if (c.value.isNull) Left(DecodingFailure(Reason.WrongTypeExpectation("non-null", c.value), c.history))
            else
              c.as[List[ListElemAlteration[Id, T, PartialFieldType]]]
                .map(PartialListField.ElemsUpdated(_))
                .orElse(c.as[List[Id]].map(PartialListField.ElemsReordered(_)))
          case _: FailedCursor => Right(PartialListField.Unchanged())
        }
    }

  implicit def partialListFieldEncoder[Id: Encoder, T <: Identifiable[T, Id]: Encoder, PartialFieldType <: Partial[
    T
  ]: Encoder]: Encoder[PartialListField[Id, T, PartialFieldType]] = {
    case PartialListField.ElemsUpdated(alterations) =>
      alterations.asJson
    case PartialListField.ElemsReordered(newOrder) => newOrder.asJson
    case PartialListField.Unchanged()              => Json.obj() // this value should be dropped by the outside encoder
  }

  /* Any partial */
  def partialCodec[P <: Partial[_]](derivedCodec: Codec[P]): Codec[P] =
    Codec.from(derivedCodec, derivedCodec.mapJson(_.dropEmptyValues))

}
