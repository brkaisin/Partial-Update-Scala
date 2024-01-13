package be.brkaisin.partialupdate.core

import scala.language.experimental.macros

package object partialupdate {
  implicit def derivePartialUpdator[T, PartialType <: Partial[T]]: PartialUpdator[T, PartialType] =
    macro PartialUpdatorMacro.impl[T, PartialType]
}
