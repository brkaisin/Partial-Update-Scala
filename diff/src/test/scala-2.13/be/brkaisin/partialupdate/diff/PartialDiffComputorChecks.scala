package be.brkaisin.partialupdate.diff

import be.brkaisin.partialupdate.Generators._
import be.brkaisin.partialupdate.core.Partial
import be.brkaisin.partialupdate.diff.Implicits._
import be.brkaisin.partialupdate.diff.PartialDiffComputors._
import be.brkaisin.partialupdate.models._
import be.brkaisin.partialupdate.util.Identifiable
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Prop, Properties}

import java.util.UUID
import scala.util.Random

final class PartialDiffComputorChecks extends Properties("PartialDiffComputor Checks") {
  implicitly[DummyImplicit]

  // All the following generators are used to generate pairs of values of the same type, one of which is "updated" from
  // the other. Everything is done to emulate a plausible real-world scenario, instead of being purely random.

  lazy val fooAndUpdateGen: Gen[(Foo, Foo)] = for {
    foo1               <- fooGen
    stringGen          <- Gen.alphaStr
    stringMaybeUpdated <- Gen.oneOf(foo1.string, stringGen)
    intGen             <- intGen
    intMaybeUpdated    <- Gen.oneOf(foo1.int, intGen)
    foo2 <- Gen.oneOf(
      foo1,
      foo1.copy(string = stringMaybeUpdated),
      foo1.copy(int = intMaybeUpdated),
      foo1.copy(string = stringMaybeUpdated, int = intMaybeUpdated)
    )
  } yield (foo1, foo2)

  lazy val barAndUpdateGen: Gen[(Bar, Bar)] = for {
    fooAndUpdate1 <- fooAndUpdateGen
    fooAndUpdate2 <- fooAndUpdateGen
    bar1 = Bar(fooAndUpdate1._1, fooAndUpdate2._1)
    bar2 <- Gen.oneOf(
      bar1,
      bar1.copy(foo1 = fooAndUpdate1._2),
      bar1.copy(foo2 = fooAndUpdate2._2),
      bar1.copy(foo1 = fooAndUpdate1._2, foo2 = fooAndUpdate2._2)
    )
  } yield (bar1, bar2)

  lazy val immutableFooAndUpdateGen: Gen[(ImmutableFoo, ImmutableFoo)] = for {
    string <- stringGen
    bar    <- barGen
    immutableFoo = ImmutableFoo(string, bar)
  } yield (immutableFoo, immutableFoo)

  lazy val babarAndUpdateGen: Gen[(Babar, Babar)] = for {
    fooAndUpdate <- fooAndUpdateGen
    babar1 <- Gen.oneOf(
      Babar(Some(fooAndUpdate._1)),
      Babar(None)
    )
    babar2 <- Gen.oneOf(
      babar1,
      babar1.copy(maybeFoo = Some(fooAndUpdate._2)),
      babar1.copy(maybeFoo = None)
    )
  } yield (babar1, babar2)

  lazy val identifiableFooAndUpdateGen: Gen[(IdentifiableFoo, IdentifiableFoo)] = for {
    fooAndUpdate     <- fooAndUpdateGen
    id               <- Gen.uuid
    identifiableFoo1 <- Gen.const(IdentifiableFoo(id, fooAndUpdate._1.string, fooAndUpdate._1.int))
    identifiableFoo2 <- Gen.oneOf(
      identifiableFoo1,
      IdentifiableFoo(id, fooAndUpdate._1.string, fooAndUpdate._2.int),
      IdentifiableFoo(id, fooAndUpdate._2.string, fooAndUpdate._1.int),
      IdentifiableFoo(id, fooAndUpdate._2.string, fooAndUpdate._2.int)
    )
  } yield (identifiableFoo1, identifiableFoo2)

  def listPermutationGen[T](list: List[T]): Gen[List[T]] =
    Gen.const(Random.shuffle(list))

  def subListOf[T](list: List[T]): Gen[List[T]] =
    Gen.oneOf(list.inits.toList)

  def mixLists[T](list1: List[T], list2: List[T]): Gen[List[T]] = {
    assert(list1.size == list2.size)
    val size = list1.size
    for {
      binaryList <- Gen.listOfN(size, Gen.oneOf(true, false))
    } yield (list1 zip list2 zip binaryList).map {
      case ((elem1, _), true)  => elem1
      case ((_, elem2), false) => elem2
    }
  }

  def mixIdentifiableLists[Id: Ordering, T <: Identifiable[T, Id]](list1: List[T], list2: List[T]): Gen[List[T]] = {
    val list1OrderedById = list1.sortBy(_.id)
    val list2OrderedById = list2.sortBy(_.id)
    mixLists(list1OrderedById, list2OrderedById)
  }

  implicit val uuidOrdering: Ordering[UUID] = Ordering.by(_.toString)

  lazy val bazAndUpdateGen: Gen[(Baz, Baz)] = for {
    (identifiableFoos, identifiableFoosUpdated) <- Gen.listOf(identifiableFooAndUpdateGen).map(_.unzip)
    mixedIdentifiableFoos                       <- mixIdentifiableLists[UUID, IdentifiableFoo](identifiableFoos, identifiableFoosUpdated)
    subListOfMixedIdentifiableFoos              <- subListOf(mixedIdentifiableFoos)
    reorderedIdentifiableFoos                   <- listPermutationGen(identifiableFoos)
    newIdentifiableFoos                         <- Gen.nonEmptyListOf(identifiableFooGen)
    subListOfIdentifiableFoos                   <- subListOf(identifiableFoos)
    baz1 = Baz(identifiableFoos)
    baz2 <- Gen.oneOf(
      baz1, // no change
      Baz(identifiableFoosUpdated), // all elems are updated
      Baz(reorderedIdentifiableFoos), // elems are reordered
      Baz(mixedIdentifiableFoos), // some elems are updated
      Baz(subListOfIdentifiableFoos), // some elems are deleted
      Baz(identifiableFoos ++ newIdentifiableFoos), // some elems are added
      Baz(subListOfIdentifiableFoos ++ newIdentifiableFoos), // some elems are deleted and some are added
      Baz(
        subListOfMixedIdentifiableFoos ++ newIdentifiableFoos
      ) // some elems are deleted, some are updated and some are added
    )
  } yield (baz1, baz2)

  def simpleListAndUpdateGen[T](elemGen: Gen[T]): Gen[(List[T], List[T])] =
    for {
      list1            <- Gen.listOf(elemGen)
      subList          <- subListOf(list1)
      reorderedList    <- listPermutationGen(list1)
      otherList        <- Gen.listOfN(list1.size, elemGen)
      otherListSublist <- subListOf(otherList)
      mixedList        <- mixLists(list1, otherList)
      list2 <- Gen.oneOf(
        list1,
        reorderedList, // elems are reordered
        otherList, // all elems are updated
        list1 ++ otherListSublist, // some elems are added
        subList, // some elems are deleted
        mixedList, // some elems are updated
        subList ++ otherListSublist, // some elems are deleted and some are added
        mixedList ++ otherListSublist // some elems are deleted, some are updated and some are added
      )
    } yield (list1, list2)

  lazy val quxAndUpdateGen: Gen[(Qux, Qux)] = for {
    (ids, idsUpdated)   <- simpleListAndUpdateGen[UUID](Gen.uuid)
    (foos, foosUpdated) <- Gen.listOf(fooAndUpdateGen).map(_.unzip)
    qux1 = Qux(ids, foos)
    qux2 <- Gen.oneOf(
      qux1, // no change
      Qux(idsUpdated, foos), // ids are updated
      Qux(ids, foosUpdated), // foos are updated
      Qux(idsUpdated, foosUpdated) // ids and foos are updated
    )
  } yield (qux1, qux2)

  def partialDiffComputorPropertyGen[T, PartialFieldType <: Partial[T]](
      pairGen: Gen[(T, T)]
  )(implicit partialDiffComputor: PartialDiffComputor[T, PartialFieldType]): Prop =
    forAll(pairGen) {
      case (t1, t2) =>
        val diff: PartialFieldType = partialDiffComputor.computePartialDiff(t1, t2)
        diff.applyPartialUpdate(t1) == t2
    }

  property("PartialDiffComputor[Foo, PartialFoo] works") =
    partialDiffComputorPropertyGen[Foo, PartialFoo](fooAndUpdateGen)

  property("PartialDiffComputor[Bar, PartialBar] works") =
    partialDiffComputorPropertyGen[Bar, PartialBar](barAndUpdateGen)

  property("PartialDiffComputor[ImmutableFoo, PartialImmutableFoo] works") =
    partialDiffComputorPropertyGen[ImmutableFoo, PartialImmutableFoo](immutableFooAndUpdateGen)

  property("PartialDiffComputor[Babar, PartialBabar] works") =
    partialDiffComputorPropertyGen[Babar, PartialBabar](babarAndUpdateGen)

  property("PartialDiffComputor[IdentifiableFoo, PartialIdentifiableFoo] works") =
    partialDiffComputorPropertyGen[IdentifiableFoo, PartialIdentifiableFoo](identifiableFooAndUpdateGen)

  property("PartialDiffComputor[Baz, PartialBaz] works") = forAll(bazAndUpdateGen) {
    case (t1, t2) =>
      val diff = implicitly[PartialDiffComputor[Baz, PartialBaz]].computePartialDiff(t1, t2)
      // we need to compare sets because the order of the elements is not guaranteed
      diff.applyPartialUpdate(t1).foos.toSet == t2.foos.toSet
  }

  property("PartialDiffComputor[Qux, PartialQux] works") = forAll(quxAndUpdateGen) {
    case (t1, t2) =>
      val diff = implicitly[PartialDiffComputor[Qux, PartialQux]].computePartialDiff(t1, t2)
      // we need to compare sets because the order of the elements is not guaranteed
      diff
        .applyPartialUpdate(t1)
        .ids
        .toSet == t2.ids.toSet && diff.applyPartialUpdate(t1).foos.toSet == t2.foos.toSet
  }
}
