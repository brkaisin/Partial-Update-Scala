package be.brkaisin.partialupdate

import be.brkaisin.partialupdate.core._
import be.brkaisin.partialupdate.models._
import be.brkaisin.partialupdate.util.Identifiable
import org.scalacheck.{Arbitrary, Gen}

import java.util.UUID
import scala.reflect.ClassTag

object Generators {

  /* Generators */
  lazy val stringGen: Gen[String]   = Gen.alphaNumStr
  lazy val intGen: Gen[Int]         = Arbitrary.arbitrary[Int]
  lazy val booleanGen: Gen[Boolean] = Gen.prob(0.5) // equivalent to Gen.oneOf(true, false) but funnier
  lazy val uuidGen: Gen[UUID]       = Gen.uuid

  def immutableFieldGen[T]: Gen[PartialImmutableField[T]] =
    Gen.const(PartialImmutableField.Unchanged[T]())

  def partialFieldGen[T](gen: Gen[T]): Gen[PartialField[T]] =
    Gen.oneOf(
      gen.map(PartialField.Updated(_)),
      Gen.const(PartialField.Unchanged[T]())
    )

  // sometimes, generating a partial field that can be unchanged is not what we want. For example,
  // if a nested element of a list is updated, it makes no sense to say that it was updated with
  // an unchanged value. This generator is here to avoid this case.
  def notUnchangedPartialFieldGen[T](gen: Gen[T]): Gen[PartialField[T]] =
    gen.map(PartialField.Updated(_))

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

  def listOperationGen[T, PartialFieldType <: Partial[T]](
      tGen: Gen[T],
      partialGen: Gen[PartialFieldType]
  ): Gen[ListOperation[T, PartialFieldType]] =
    Gen.oneOf(
      for {
        maybeIndex <- Gen.option(intGen)
        t          <- tGen
      } yield ListOperation.ElemAdded[T, PartialFieldType](maybeIndex, t),
      for {
        index   <- intGen
        partial <- partialGen
      } yield ListOperation.ElemUpdated[T, PartialFieldType](index, partial),
      intGen.map(ListOperation.ElemDeletedAtIndex[T, PartialFieldType]),
      tGen.map(ListOperation.ElemDeleted[T, PartialFieldType])
    )

  def partialListFieldGen[T, PartialFieldType <: Partial[T]](
      tGen: Gen[T],
      notUnchangedPartialGen: Gen[PartialFieldType]
  ): Gen[PartialListField[T, PartialFieldType]] =
    Gen.oneOf(
      Gen
        .nonEmptyListOf(listOperationGen(tGen, notUnchangedPartialGen))
        .map(PartialListField.ElemsUpdated(_)),
      Gen.nonEmptyListOf(intGen).map(PartialListField.ElemsReordered[T, PartialFieldType]),
      Gen.const(PartialListField.Unchanged[T, PartialFieldType]())
    )

  def identifiableListOperationGen[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      idGen: Gen[Id],
      tGen: Gen[T],
      partialGen: Gen[PartialFieldType]
  ): Gen[IdentifiableListOperation[Id, T, PartialFieldType]] =
    Gen.oneOf(
      tGen.map(t => IdentifiableListOperation.ElemAdded[Id, T, PartialFieldType](t.id, t)),
      for {
        id      <- idGen
        partial <- partialGen
      } yield IdentifiableListOperation.ElemUpdated[Id, T, PartialFieldType](id, partial),
      idGen.map(IdentifiableListOperation.ElemDeleted[Id, T, PartialFieldType])
    )

  def partialIdentifiableListFieldGen[Id, T <: Identifiable[T, Id], PartialFieldType <: Partial[T]](
      idGen: Gen[Id],
      tGen: Gen[T],
      partialGen: Gen[PartialFieldType]
  ): Gen[PartialIdentifiableListField[Id, T, PartialFieldType]] =
    Gen.oneOf(
      Gen
        .nonEmptyListOf(identifiableListOperationGen(idGen, tGen, partialGen))
        .map(PartialIdentifiableListField.ElemsUpdated(_)),
      Gen.nonEmptyListOf(idGen).map(PartialIdentifiableListField.ElemsReordered[Id, T, PartialFieldType]),
      Gen.const(PartialIdentifiableListField.Unchanged[Id, T, PartialFieldType]())
    )

  def partialEnum2FieldGen[T, T1 <: T: ClassTag, PartialFieldType1 <: Partial[
    T1
  ], T2 <: T: ClassTag, PartialFieldType2 <: Partial[T2]](
      gen1NotUnchanged: Gen[T1], // must generate a value that is not Unchanged, since for update
      genPartial1: Gen[PartialFieldType1],
      gen2NotUnchanged: Gen[T2], // must generate a value that is not Unchanged, since for update
      genPartial2: Gen[PartialFieldType2]
  ): Gen[PartialEnum2Field[T, T1, PartialFieldType1, T2, PartialFieldType2]] =
    Gen.oneOf(
      gen1NotUnchanged.map(PartialEnum2Field.Value1Set[T, T1, PartialFieldType1, T2, PartialFieldType2]),
      gen2NotUnchanged.map(PartialEnum2Field.Value2Set[T, T1, PartialFieldType1, T2, PartialFieldType2]),
      genPartial1.map(PartialEnum2Field.Value1Updated[T, T1, PartialFieldType1, T2, PartialFieldType2]),
      genPartial2.map(PartialEnum2Field.Value2Updated[T, T1, PartialFieldType1, T2, PartialFieldType2]),
      Gen.const(PartialEnum2Field.Unchanged[T, T1, PartialFieldType1, T2, PartialFieldType2]())
    )

  lazy val fooGen: Gen[Foo] = for {
    string <- stringGen
    int    <- intGen
  } yield Foo(string, int)

  lazy val partialFooGen: Gen[PartialFoo] = for {
    string <- partialFieldGen(stringGen)
    int    <- partialFieldGen(intGen)
    // if only unchanged values, retry. This is a case that should never happen. A more detailed explanation are on the way...
    if !(string == PartialField.Unchanged[String]() && int == PartialField.Unchanged[Int]())
  } yield PartialFoo(string, int)

  lazy val barGen: Gen[Bar] = for {
    foo1 <- fooGen
    foo2 <- fooGen
  } yield Bar(foo1, foo2)

  lazy val partialBarGen: Gen[PartialBar] = for {
    foo1 <- partialNestedFieldGen[Foo, PartialFoo](partialFooGen)
    foo2 <- partialNestedFieldGen[Foo, PartialFoo](partialFooGen)
  } yield PartialBar(foo1, foo2)

  lazy val immutableFooGen: Gen[ImmutableFoo] = for {
    string <- stringGen
    bar    <- barGen
  } yield ImmutableFoo(string, bar)

  lazy val partialImmutableFooGen: Gen[PartialImmutableFoo] = for {
    string <- immutableFieldGen[String]
    bar    <- immutableFieldGen[Bar]
  } yield PartialImmutableFoo(string, bar)

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
    id     <- immutableFieldGen[UUID]
    string <- partialFieldGen(stringGen)
    int    <- partialFieldGen(intGen)
  } yield PartialIdentifiableFoo(id, string, int)

  lazy val bazGen: Gen[Baz] = for {
    foos <- Gen.listOf(identifiableFooGen)
  } yield Baz(foos)

  lazy val partialBazGen: Gen[PartialBaz] = for {
    foos <- partialIdentifiableListFieldGen[UUID, IdentifiableFoo, PartialIdentifiableFoo](
      uuidGen,
      identifiableFooGen,
      partialIdentifiableFooGen
    )
  } yield PartialBaz(foos)

  lazy val quxGen: Gen[Qux] = for {
    ids  <- Gen.listOf(uuidGen)
    foos <- Gen.listOf(fooGen)
  } yield Qux(ids, foos)

  lazy val partialQuxGen: Gen[PartialQux] = for {
    ids  <- partialListFieldGen[UUID, PartialField[UUID]](uuidGen, notUnchangedPartialFieldGen(uuidGen))
    foos <- partialListFieldGen[Foo, PartialFoo](fooGen, partialFooGen)
  } yield PartialQux(ids, foos)

  lazy val corgeGen: Gen[Corge] = for {
    stringOrInt <- Gen.oneOf(
      stringGen.map(Corge.StringOrInt.StringWrapper(_)),
      intGen.map(Corge.StringOrInt.IntWrapper(_))
    )
  } yield Corge(stringOrInt)

  lazy val partialCorgeGen: Gen[PartialCorge] = for {
    stringOrInt <-
      partialEnum2FieldGen[
        Corge.StringOrInt,
        Corge.StringOrInt.StringWrapper,
        Corge.StringOrInt.StringWrapper.PartialStringWrapper,
        Corge.StringOrInt.IntWrapper,
        Corge.StringOrInt.IntWrapper.PartialIntWrapper
      ](
        stringGen.map(Corge.StringOrInt.StringWrapper(_)),
        notUnchangedPartialFieldGen(stringGen).map(Corge.StringOrInt.StringWrapper.PartialStringWrapper),
        intGen.map(Corge.StringOrInt.IntWrapper(_)),
        notUnchangedPartialFieldGen(intGen).map(Corge.StringOrInt.IntWrapper.PartialIntWrapper)
      )
  } yield PartialCorge(stringOrInt)
}
