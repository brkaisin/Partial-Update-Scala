package be.brkaisin.partialupdate.diff

import be.brkaisin.partialupdate.ExtraGenerators._
import be.brkaisin.partialupdate.core.Partial
import be.brkaisin.partialupdate.diff.PartialDiffComputors._
import be.brkaisin.partialupdate.models._
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Prop, Properties}

final class PartialDiffComputorChecks extends Properties("PartialDiffComputor Checks") {
  implicitly[DummyImplicit]

  def partialDiffComputorPropertyGen[T, PartialFieldType <: Partial[T]](implicit
      pairGen: Gen[(T, T)],
      partialDiffComputor: PartialDiffComputor[T, PartialFieldType]
  ): Prop =
    forAll(pairGen) {
      case (t1, t2) =>
        val diff: PartialFieldType = computePartialDiff(t1, t2)
        diff.applyPartialUpdate(t1) == t2
    }

  property("PartialDiffComputor[Foo, PartialFoo] works") = partialDiffComputorPropertyGen[Foo, PartialFoo]

  property("PartialDiffComputor[Bar, PartialBar] works") = partialDiffComputorPropertyGen[Bar, PartialBar]

  property("PartialDiffComputor[ImmutableFoo, PartialImmutableFoo] works") =
    partialDiffComputorPropertyGen[ImmutableFoo, PartialImmutableFoo]

  property("PartialDiffComputor[Babar, PartialBabar] works") = partialDiffComputorPropertyGen[Babar, PartialBabar]

  property("PartialDiffComputor[IdentifiableFoo, PartialIdentifiableFoo] works") =
    partialDiffComputorPropertyGen[IdentifiableFoo, PartialIdentifiableFoo]

  property("PartialDiffComputor[Baz, PartialBaz] works") = forAll(bazAndUpdateGen) {
    case (t1, t2) =>
      val diff = computePartialDiff(t1, t2)
      // we need to compare sets because the order of the elements is not guaranteed
      diff.applyPartialUpdate(t1).foos.toSet == t2.foos.toSet
  }

  property("PartialDiffComputor[Qux, PartialQux] works") = forAll(quxAndUpdateGen) {
    case (t1, t2) =>
      val diff = computePartialDiff(t1, t2)
      // we need to compare sets because the order of the elements is not guaranteed
      diff
        .applyPartialUpdate(t1)
        .ids
        .toSet == t2.ids.toSet && diff.applyPartialUpdate(t1).foos.toSet == t2.foos.toSet
  }

  // Tests where the implicit [[PartialDiffComputor]] is in the companion object the case class

  property("PartialDiffComputor[BigFoo, PartialBigFoo] works") = partialDiffComputorPropertyGen[BigFoo, PartialBigFoo]

  property("PartialDiffComputor[BigBar, PartialBigBar] works") = partialDiffComputorPropertyGen[BigBar, PartialBigBar]

  property("PartialDiffComputor[Corge, PartialCorge] works") = partialDiffComputorPropertyGen[Corge, PartialCorge]
}
