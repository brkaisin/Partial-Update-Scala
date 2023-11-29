package be.brkaisin.partialupdate.circe

import be.brkaisin.partialupdate.models._
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import CirceCodecs._

object Codecs {
  implicit val fooCodec: Codec[Foo]               = deriveCodec
  implicit val partialFooCodec: Codec[PartialFoo] = partialCodec(deriveCodec)

  implicit val barCodec: Codec[Bar]               = deriveCodec
  implicit val partialBarCodec: Codec[PartialBar] = partialCodec(deriveCodec)

  implicit val immutableFooCodec: Codec[ImmutableFoo]               = deriveCodec
  implicit val partialImmutableFooCodec: Codec[PartialImmutableFoo] = partialCodec(deriveCodec)

  implicit val babarCodec: Codec[Babar]               = deriveCodec
  implicit val partialBabarCodec: Codec[PartialBabar] = partialCodec(deriveCodec)

  implicit val identifiableFooCodec: Codec[IdentifiableFoo]               = deriveCodec
  implicit val partialIdentifiableFooCodec: Codec[PartialIdentifiableFoo] = partialCodec(deriveCodec)

  implicit val bazCodec: Codec[Baz]               = deriveCodec
  implicit val partialBazCodec: Codec[PartialBaz] = partialCodec(deriveCodec)

  implicit val quxCodec: Codec[Qux]               = deriveCodec
  implicit val partialQuxCodec: Codec[PartialQux] = partialCodec(deriveCodec)
}
