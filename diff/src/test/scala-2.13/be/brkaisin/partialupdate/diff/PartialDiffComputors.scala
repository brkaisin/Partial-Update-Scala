package be.brkaisin.partialupdate.diff

import be.brkaisin.partialupdate.models._
import Implicits._

object PartialDiffComputors {
  implicitly[DummyImplicit]

  implicit val fooPartialDiffComputor: PartialDiffComputor[Foo, PartialFoo] =
    PartialDiffComputorMacro.derivePartialDiffComputor

  implicit val barPartialDiffComputor: PartialDiffComputor[Bar, PartialBar] =
    PartialDiffComputorMacro.derivePartialDiffComputor

  implicit val immutableFooPartialDiffComputor: PartialDiffComputor[ImmutableFoo, PartialImmutableFoo] =
    PartialDiffComputorMacro.derivePartialDiffComputor

  implicit val babarPartialDiffComputor: PartialDiffComputor[Babar, PartialBabar] =
    PartialDiffComputorMacro.derivePartialDiffComputor

  implicit val identifiableFooPartialDiffComputor: PartialDiffComputor[IdentifiableFoo, PartialIdentifiableFoo] =
    PartialDiffComputorMacro.derivePartialDiffComputor

  implicit val bazPartialDiffComputor: PartialDiffComputor[Baz, PartialBaz] =
    PartialDiffComputorMacro.derivePartialDiffComputor

  implicit val quxPartialDiffComputor: PartialDiffComputor[Qux, PartialQux] =
    PartialDiffComputorMacro.derivePartialDiffComputor
}
