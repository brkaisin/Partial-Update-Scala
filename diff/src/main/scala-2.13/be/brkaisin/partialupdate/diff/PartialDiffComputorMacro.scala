package be.brkaisin.partialupdate.diff

import be.brkaisin.partialupdate.core.Partial

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object PartialDiffComputorMacro {
  def impl[T, PartialType <: Partial[T]](
      c: blackbox.Context
  )(implicit tt: c.WeakTypeTag[T], pt: c.WeakTypeTag[PartialType]): c.Expr[PartialDiffComputor[T, PartialType]] = {
    import c.universe._

    val tpe: c.universe.Type        = weakTypeOf[T]
    val partialTpe: c.universe.Type = weakTypeOf[PartialType]

    // Extract fields from the case class
    val fields: List[c.universe.MethodSymbol] = tpe.decls.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.toList

    val partialFields: List[c.universe.MethodSymbol] = partialTpe.decls.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.toList

    // Construct the partialTpe type field by field by calling the right implicit partialDiffComputor for each field
    val fieldsDiffTrees: List[c.universe.Tree] = fields.zip(partialFields).map {
      case (field, partialField) =>
        val fieldName: c.universe.TermName    = field.name.toTermName
        val fieldType: c.universe.Type        = field.returnType
        val partialFieldType: c.universe.Type = partialField.returnType
        val partialFieldDiffComputorType: c.universe.Type =
          appliedType(weakTypeOf[PartialDiffComputor[_, _]].typeConstructor, fieldType, partialFieldType)

        // Find the implicit PartialDiffComputor for the field
        val partialFieldDiffComputor: c.Tree = c.inferImplicitValue(partialFieldDiffComputorType)

        if (partialFieldDiffComputor.isEmpty)
          c.abort(
            c.enclosingPosition,
            s"No implicit PartialDiffComputor found for field $fieldName of class $tpe. You need to define an implicit " +
              s"PartialDiffComputor[$fieldType, $partialFieldType] and make it available in the scope of the call to derivePartialDiffComputor."
          )

        // Generate the code to compute the difference for this field
        q"""$fieldName = $partialFieldDiffComputor.computePartialDiff(currentValue.$fieldName, newValue.$fieldName)"""
    }

    // Combine the individual field diffs into a PartialDiffComputor for the whole class
    c.Expr[PartialDiffComputor[T, PartialType]] {
      q"""
        new PartialDiffComputor[$tpe, $partialTpe] {
          def computePartialDiff(currentValue: $tpe, newValue: $tpe): $partialTpe =
            new $partialTpe(..$fieldsDiffTrees)
        }
      """
    }
  }

  def derivePartialDiffComputor[T, PartialType <: Partial[T]]: PartialDiffComputor[T, PartialType] =
    macro impl[T, PartialType]
}
