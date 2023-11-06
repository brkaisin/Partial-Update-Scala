package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialIdentifiableListField, PartialUpdator}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PartialBaz(
    foos: PartialIdentifiableListField[java.util.UUID, IdentifiableFoo, PartialIdentifiableFoo]
) extends Partial[Baz] {

  def applyPartialUpdate(currentValue: Baz): Baz = PartialUpdator[PartialBaz].updated[Baz](this, currentValue.copy())
}

object PartialBaz {
  import be.brkaisin.partialupdate.circe.CirceCodecs._
  implicit val codec: Codec[PartialBaz] = partialCodec(deriveCodec)
}
