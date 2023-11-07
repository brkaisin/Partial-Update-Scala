package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.core.{Partial, PartialListField, PartialUpdator, SimplePartialListField}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class PartialQux(
    ids: SimplePartialListField[java.util.UUID],
    foos: PartialListField[Foo, PartialFoo]
) extends Partial[Qux] {

  def applyPartialUpdate(currentValue: Qux): Qux = PartialUpdator[PartialQux].updated[Qux](this, currentValue.copy())
}

object PartialQux {
  import be.brkaisin.partialupdate.circe.CirceCodecs._
  implicit val codec: Codec[PartialQux] = partialCodec(deriveCodec)
}
