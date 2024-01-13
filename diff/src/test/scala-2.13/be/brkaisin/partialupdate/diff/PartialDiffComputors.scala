package be.brkaisin.partialupdate.diff

import be.brkaisin.partialupdate.models._
import Implicits._

object PartialDiffComputors {
  implicitly[DummyImplicit]

  implicit val fooPartialDiffComputor: PartialDiffComputor[Foo, PartialFoo] =
    derivePartialDiffComputor

  implicit val barPartialDiffComputor: PartialDiffComputor[Bar, PartialBar] =
    derivePartialDiffComputor

  implicit val immutableFooPartialDiffComputor: PartialDiffComputor[ImmutableFoo, PartialImmutableFoo] =
    derivePartialDiffComputor

  implicit val babarPartialDiffComputor: PartialDiffComputor[Babar, PartialBabar] =
    derivePartialDiffComputor

  implicit val identifiableFooPartialDiffComputor: PartialDiffComputor[IdentifiableFoo, PartialIdentifiableFoo] =
    derivePartialDiffComputor

  implicit val bazPartialDiffComputor: PartialDiffComputor[Baz, PartialBaz] =
    derivePartialDiffComputor

  implicit val quxPartialDiffComputor: PartialDiffComputor[Qux, PartialQux] =
    derivePartialDiffComputor
}
