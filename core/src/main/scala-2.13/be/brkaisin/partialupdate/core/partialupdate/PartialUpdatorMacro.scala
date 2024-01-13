package be.brkaisin.partialupdate.core.partialupdate

import be.brkaisin.partialupdate.core.Partial

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object PartialUpdatorMacro {

  def impl[T, PartialType <: Partial[T]](
      c: blackbox.Context
  )(implicit tt: c.WeakTypeTag[T], pt: c.WeakTypeTag[PartialType]): c.Expr[PartialUpdator[T, PartialType]] = {
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
        val fieldName: c.universe.TermName        = field.name.toTermName
        val partialFieldName: c.universe.TermName = partialField.name.toTermName

        // Check that the field names match
        if (fieldName != partialFieldName)
          c.abort(
            c.enclosingPosition,
            s"Field names don't match: $fieldName != $partialFieldName"
          )

        val fieldType: c.universe.Type        = field.returnType
        val partialFieldType: c.universe.Type = partialField.returnType

        // Check that the field types match

        // 1. Find the type of the field in the parent partial type
        val partialFieldTypeT: c.universe.Type =
          partialFieldType.baseType(weakTypeOf[Partial[_]].typeSymbol).typeArgs.head

        // 2. Check that the field type in the partial is the same as the field type in the class
        if (fieldType != partialFieldTypeT)
          c.abort(
            c.enclosingPosition,
            s"Field types don't match for field $fieldName: $fieldType != $partialFieldTypeT"
          )

        // Generate the code to compute the update for this field
        q"$fieldName = $partialField.applyPartialUpdate(currentValue.$fieldName)"
    }

    // Combine the individual field diffs into a PartialDiffComputor for the whole class
    c.Expr[PartialUpdator[T, PartialType]] {
      // SAM syntax
      q"(currentValue: $tpe, partial: $partialTpe) => new $tpe(..$fieldsDiffTrees)"
    }
  }

  def derivePartialUpdator[T, PartialType <: Partial[T]]: PartialUpdator[T, PartialType] =
    macro impl[T, PartialType]
}
