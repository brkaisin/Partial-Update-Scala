package be.brkaisin.partialupdate.circe

import be.brkaisin.partialupdate.core._
import be.brkaisin.partialupdate.models._
import be.brkaisin.partialupdate.util.Identifiable
import io.circe.Codec
import io.circe.Decoder.Result
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen, Prop, Properties}

final class CodecsChecks extends Properties("Codecs Checks") {

  /* Generators */
  lazy val stringGen: Gen[String]       = Gen.alphaNumStr
  lazy val intGen: Gen[Int]             = Arbitrary.arbitrary[Int]
  lazy val booleanGen: Gen[Boolean]     = Gen.prob(0.5) // equivalent to Gen.oneOf(true, false) but funnier
  lazy val uuidGen: Gen[java.util.UUID] = Gen.uuid

  def partialFieldGen[T](gen: Gen[T]): Gen[PartialField[T]] =
    Gen.oneOf(
      gen.map(PartialField.Updated(_)),
      Gen.const(PartialField.Unchanged[T]())
    )

  def partialNestedFieldGen[T, PartialFieldType <: Partial[T]](
      gen: Gen[PartialFieldType]
  ): Gen[PartialNestedField[T, PartialFieldType]] =
    Gen.oneOf(
      gen.map(PartialNestedField.Updated[T, PartialFieldType]),
      Gen.const(PartialNestedField.Unchanged[T, PartialFieldType]())
    )

  def partialOptionalFieldGen[T, PartialFieldType <: Partial[T]](
      tGen: Gen[T],
      partialGen: Gen[PartialFieldType]
  ): Gen[PartialOptionalField[T, PartialFieldType]] =
    Gen.oneOf(
      tGen.map(PartialOptionalField.Set[T, PartialFieldType]),
      partialGen.map(PartialOptionalField.Updated[T, PartialFieldType]),
      Gen.const(PartialOptionalField.Unchanged[T, PartialFieldType]()),
      Gen.const(PartialOptionalField.Deleted[T, PartialFieldType]())
    )

  def simplePartialOptionalFieldGen[T](gen: Gen[T]): Gen[SimplePartialOptionalField[T]] =
    partialOptionalFieldGen(gen, gen.map(PartialField.Updated(_)))

  def listOperationGen[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      idGen: Gen[Id],
      tGen: Gen[T],
      partialGen: Gen[PartialFieldType]
  ): Gen[PartialListField.ListOperation[Id, T, PartialFieldType]] =
    Gen.oneOf(
      tGen.map(t => PartialListField.ListOperation.ElemAdded[Id, T, PartialFieldType](t.id, t)),
      for {
        id      <- idGen
        partial <- partialGen
      } yield PartialListField.ListOperation.ElemUpdated[Id, T, PartialFieldType](id, partial),
      idGen.map(PartialListField.ListOperation.ElemDeleted[Id, T, PartialFieldType])
    )

  def partialListFieldGen[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      idGen: Gen[Id],
      tGen: Gen[T],
      partialGen: Gen[PartialFieldType]
  ): Gen[PartialListField[Id, T, PartialFieldType]] =
    Gen.oneOf(
      Gen
        .nonEmptyListOf(listOperationGen(idGen, tGen, partialGen))
        .map(PartialListField.ElemsUpdated(_)),
      Gen.const(PartialListField.Unchanged[Id, T, PartialFieldType]())
    )

  lazy val fooGen: Gen[Foo] = for {
    string      <- stringGen
    int         <- intGen
    maybeString <- Gen.option(stringGen)
    maybeInt    <- Gen.option(intGen)
  } yield Foo(string, int, maybeString, maybeInt)

  lazy val partialFooGen: Gen[PartialFoo] = for {
    string      <- partialFieldGen(stringGen)
    int         <- partialFieldGen(intGen)
    maybeString <- simplePartialOptionalFieldGen(stringGen)
    maybeInt    <- simplePartialOptionalFieldGen(intGen)
    // if only unchanged values, retry. This is a case that should never happen. A more detailed explanation are on the way...
    if !(string == PartialField.Unchanged[String]() && int == PartialField
      .Unchanged[Int]() && maybeString == PartialOptionalField
      .Unchanged[String, PartialField[String]]() && maybeInt == PartialOptionalField
      .Unchanged[Int, PartialField[Int]]())
  } yield PartialFoo(string, int, maybeString, maybeInt)

  lazy val barGen: Gen[Bar] = for {
    foo          <- fooGen
    maybeBoolean <- Gen.option(booleanGen)
  } yield Bar(foo, maybeBoolean)

  lazy val partialBarGen: Gen[PartialBar] = for {
    foo          <- partialNestedFieldGen[Foo, PartialFoo](partialFooGen)
    maybeBoolean <- simplePartialOptionalFieldGen(booleanGen)
  } yield PartialBar(foo, maybeBoolean)

  lazy val babarGen: Gen[Babar] = for {
    maybeFoo <- Gen.option(fooGen)
  } yield Babar(maybeFoo)

  lazy val partialBabarGen: Gen[PartialBabar] = for {
    maybeFoo <- partialOptionalFieldGen[Foo, PartialFoo](fooGen, partialFooGen)
  } yield PartialBabar(maybeFoo)

  lazy val identifiableFooGen: Gen[IdentifiableFoo] = for {
    id     <- uuidGen
    string <- stringGen
    int    <- intGen
  } yield IdentifiableFoo(id, string, int)

  lazy val partialIdentifiableFooGen: Gen[PartialIdentifiableFoo] = for {
    string <- partialFieldGen(stringGen)
    int    <- partialFieldGen(intGen)
  } yield PartialIdentifiableFoo(string, int)

  lazy val bazGen: Gen[Baz] = for {
    foos <- Gen.listOf(identifiableFooGen)
  } yield Baz(foos)

  lazy val partialBazGen: Gen[PartialBaz] = for {
    foos <- partialListFieldGen[java.util.UUID, IdentifiableFoo, PartialIdentifiableFoo](
      uuidGen,
      identifiableFooGen,
      partialIdentifiableFooGen
    )
  } yield PartialBaz(foos)

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

  property("Codec[Foo] works") = codecPropertyGen(fooGen)

  property("Codec[PartialFoo] works") = codecPropertyGen(partialFooGen)

  property("Codec[Bar] works") = codecPropertyGen(barGen)

  property("Codec[PartialBar] works") = codecPropertyGen(partialBarGen)

  property("Codec[Babar] works") = codecPropertyGen(babarGen)

  property("Codec[PartialBabar] works") = codecPropertyGen(partialBabarGen)

  property("Codec[IdentifiableFoo] works") = codecPropertyGen(identifiableFooGen)

  property("Codec[Baz] works") = codecPropertyGen(bazGen)

  property("Codec[PartialBaz] works") = codecPropertyGen(partialBazGen)

}
