package be.brkaisin.partialupdate.circe

import be.brkaisin.partialupdate.Generators._
import io.circe.Codec
import io.circe.Decoder.Result
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Prop, Properties}

final class CodecsChecks extends Properties("Codecs Checks") {

  /**
    * Round trip a value through its codec.
    * @param value The value to round trip.
    * @tparam T The type of the value.
    * @return The final encoding result, either a value or an error.
    */
  def roundTrip[T: Codec](value: T): Result[T] =
    value.asJson.as[T]

  /**
    * Test a codec by round tripping a value through it.
    * @tparam T The type of the value.
    * @param value The value to round trip.
    * @return A property that succeeds if the round trip succeeds, that is, if the value is the same after the round trip.
    */
  def testCodec[T: Codec](value: T): Prop =
    Prop(roundTrip(value) == Right(value)) :|
      s"""Value: $value
         |Value encoded: ${value.asJson}
         |Value after round trip: ${roundTrip(value)}
         |""".stripMargin

  def codecPropertyGen[T: Codec](gen: Gen[T]): Prop = forAll(gen)(testCodec(_))

  import Codecs._
  property("Codec[Foo] works") = codecPropertyGen(fooGen)

  property("Codec[PartialFoo] works") = codecPropertyGen(partialFooGen)

  property("Codec[Bar] works") = codecPropertyGen(barGen)

  property("Codec[PartialBar] works") = codecPropertyGen(partialBarGen)

  property("Codec[Babar] works") = codecPropertyGen(babarGen)

  property("Codec[PartialBabar] works") = codecPropertyGen(partialBabarGen)

  property("Codec[IdentifiableFoo] works") = codecPropertyGen(identifiableFooGen)

  property("Codec[Baz] works") = codecPropertyGen(bazGen)

  property("Codec[PartialBaz] works") = codecPropertyGen(partialBazGen)

  property("Codec[Qux] works") = codecPropertyGen(quxGen)

  property("Codec[PartialQux] works") = codecPropertyGen(partialQuxGen)

  property("Codec[ImmutableFoo] works") = codecPropertyGen(immutableFooGen)

  property("Codec[PartialImmutableFoo] works") = codecPropertyGen(partialImmutableFooGen)
}
