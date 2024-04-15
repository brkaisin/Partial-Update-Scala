package be.brkaisin.partialupdate

import be.brkaisin.partialupdate.Generators._
import be.brkaisin.partialupdate.models.Corge.StringOrInt
import be.brkaisin.partialupdate.models.Corge.StringOrInt.{IntWrapper, StringWrapper}
import be.brkaisin.partialupdate.models._
import be.brkaisin.partialupdate.util.Identifiable
import org.scalacheck.Gen

import java.util.UUID
import scala.util.Random

object ExtraGenerators {
  // Generic generators

  /**
    * Generates a list of the same size as the input list, where the elements are a permutation of the elements of the
    * input list.
    * @param list the input list
    * @tparam T the type of the elements of the list
    * @return a generator that generates a list of the same size as the input list, where the elements are a permutation
    *         of the elements of the input list
    */
  def listPermutationGen[T](list: List[T]): Gen[List[T]] =
    Gen.const(Random.shuffle(list))

  /**
    * Generates a sublist of the input list.
    * @param list the input list
    * @tparam T the type of the elements of the list
    * @return a generator that generates a sublist of the input list
    */
  def subListOfGen[T](list: List[T]): Gen[List[T]] =
    Gen.oneOf(list.inits.toList)

  /**
    * Mixes two lists together, element by element, using a binary list to decide which element to take from which list.
    * @param list1 the first list
    * @param list2 the second list
    * @tparam T the type of the elements of the lists
    * @return a generator that generates a list of the same size as the input lists, where each element is taken from
    *         either list1 or list2
    */
  def mixListsGen[T](list1: List[T], list2: List[T]): Gen[List[T]] = {
    assert(list1.size == list2.size)
    val size = list1.size
    for {
      binaryList <- Gen.listOfN(size, Gen.oneOf(true, false))
    } yield (list1 zip list2 zip binaryList).map {
      case ((elem1, _), true)  => elem1
      case ((_, elem2), false) => elem2
    }
  }

  /**
    * Mixes two lists of [[Identifiable]] elements together, element by element, using a binary list to decide which
    * element to take from which list. The elements of the lists are sorted by their id before being mixed.
    * @param list1 the first list
    * @param list2 the second list
    * @tparam Id the type of the id of the elements of the lists
    * @tparam T the type of the elements of the lists
    * @return a generator that generates a list of the same size as the input lists, where each element is taken from
    *         either list1 or list2
    */
  def mixIdentifiableListsGen[Id: Ordering, T <: Identifiable[T, Id]](list1: List[T], list2: List[T]): Gen[List[T]] = {
    val list1OrderedById = list1.sortBy(_.id)
    val list2OrderedById = list2.sortBy(_.id)
    mixListsGen(list1OrderedById, list2OrderedById)
  }

  /**
    * Generates a pair of lists of the same size, where the second list is an "update" of the first list. The update
    * can be a deletion, an addition, an update, a reordering, or a combination of those.
    * @param elemGen the generator for the elements of the lists
    * @tparam T the type of the elements of the lists
    * @return a generator that generates a pair of lists of the same size, where the second list is an "update" of the
    *         first list
    */
  def simpleListAndUpdateGen[T](elemGen: Gen[T]): Gen[(List[T], List[T])] =
    for {
      list1            <- Gen.listOf(elemGen)
      subList          <- subListOfGen(list1)
      reorderedList    <- listPermutationGen(list1)
      otherList        <- Gen.listOfN(list1.size, elemGen)
      otherListSublist <- subListOfGen(otherList)
      mixedList        <- mixListsGen(list1, otherList)
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

  // Miscellaneous

  implicit val uuidOrdering: Ordering[UUID] = Ordering.by(_.toString)

  lazy val bigFooGen: Gen[BigFoo] = for {
    int         <- intGen
    maybeString <- Gen.option(stringGen)
  } yield BigFoo(int, maybeString)

  lazy val bigBarGen: Gen[BigBar] = for {
    bigFoo <- bigFooGen
  } yield BigBar(bigFoo)

  // All the following generators are used to generate pairs of values of the same type, one of which is "updated" from
  // the other. Everything is done to emulate a plausible real-world scenario, instead of being purely random.

  implicit lazy val fooAndUpdateGen: Gen[(Foo, Foo)] = for {
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

  implicit lazy val barAndUpdateGen: Gen[(Bar, Bar)] = for {
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

  implicit lazy val immutableFooAndUpdateGen: Gen[(ImmutableFoo, ImmutableFoo)] = for {
    string <- stringGen
    bar    <- barGen
    immutableFoo = ImmutableFoo(string, bar)
  } yield (immutableFoo, immutableFoo)

  implicit lazy val babarAndUpdateGen: Gen[(Babar, Babar)] = for {
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

  implicit lazy val identifiableFooAndUpdateGen: Gen[(IdentifiableFoo, IdentifiableFoo)] = for {
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

  implicit lazy val bazAndUpdateGen: Gen[(Baz, Baz)] = for {
    (identifiableFoos, identifiableFoosUpdated) <- Gen.listOf(identifiableFooAndUpdateGen).map(_.unzip)
    mixedIdentifiableFoos                       <- mixIdentifiableListsGen[UUID, IdentifiableFoo](identifiableFoos, identifiableFoosUpdated)
    subListOfMixedIdentifiableFoos              <- subListOfGen(mixedIdentifiableFoos)
    reorderedIdentifiableFoos                   <- listPermutationGen(identifiableFoos)
    newIdentifiableFoos                         <- Gen.nonEmptyListOf(identifiableFooGen)
    subListOfIdentifiableFoos                   <- subListOfGen(identifiableFoos)
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

  implicit lazy val quxAndUpdateGen: Gen[(Qux, Qux)] = for {
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

  implicit lazy val bigFooAndUpdateGen: Gen[(BigFoo, BigFoo)] = for {
    bigFoo1        <- bigFooGen
    intGen         <- intGen
    maybeStringGen <- Gen.option(stringGen)
    bigFoo2 <- Gen.oneOf(
      bigFoo1,
      BigFoo(intGen, maybeStringGen),
      BigFoo(intGen, bigFoo1.maybeString),
      BigFoo(bigFoo1.int, maybeStringGen)
    )
  } yield (bigFoo1, bigFoo2)

  implicit lazy val bigBarAndUpdateGen: Gen[(BigBar, BigBar)] = for {
    bigFooAndUpdate1 <- bigFooAndUpdateGen
    bigFooAndUpdate2 <- bigFooAndUpdateGen
    bigBar1 = BigBar(bigFooAndUpdate1._1)
    bigBar2 <- Gen.oneOf(
      bigBar1,
      BigBar(bigFooAndUpdate1._2),
      BigBar(bigFooAndUpdate2._1),
      BigBar(bigFooAndUpdate2._2)
    )
  } yield (bigBar1, bigBar2)

  lazy val stringWrapperGen: Gen[StringWrapper] = stringGen.map(string => StringWrapper(value = string))
  lazy val intWrapperGen: Gen[IntWrapper]       = intGen.map(int => IntWrapper(value = int))

  lazy val stringOrIntAndUpdateGen: Gen[(StringOrInt, StringOrInt)] = for {
    stringWrapper1 <- stringWrapperGen
    stringWrapper2 <- stringWrapperGen
    intWrapper1    <- intWrapperGen
    intWrapper2    <- intWrapperGen
    stringOrInt1   <- Gen.oneOf(stringWrapper1, intWrapper1)
    stringOrInt2   <- Gen.oneOf(stringOrInt1 /* Unchanged */, stringWrapper2, intWrapper2)
  } yield (stringOrInt1, stringOrInt2)

  implicit lazy val corgeAndUpdateGen: Gen[(Corge, Corge)] =
    stringOrIntAndUpdateGen.map {
      case (left, right) => (Corge(left), Corge(right))
    }
}
