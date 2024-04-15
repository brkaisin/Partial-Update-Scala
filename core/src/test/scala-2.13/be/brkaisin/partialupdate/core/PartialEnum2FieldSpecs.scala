package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models.Corge.StringOrInt.StringWrapper
import be.brkaisin.partialupdate.models.{Corge, PartialCorge}

class PartialEnum2FieldSpecs extends munit.FunSuite {
  val corgeWithV1: Corge = Corge(StringWrapper("string"))
  val corgeWithV2: Corge = Corge(Corge.StringOrInt.IntWrapper(1))

  test("Partial update of no field must return the same value") {
    val partialCorge = PartialCorge()

    assertEquals(partialCorge.applyPartialUpdate(corgeWithV1), corgeWithV1)
    assertEquals(partialCorge.applyPartialUpdate(corgeWithV2), corgeWithV2)
  }

  test("Partial update of the current value of the enum must update the value") {
    val partialCorgeWithV1 = PartialCorge(
      stringOrInt =
        PartialEnum2Field.Value1Updated(StringWrapper.PartialStringWrapper(PartialField.Updated("stringModified")))
    )

    assertEquals(
      partialCorgeWithV1.applyPartialUpdate(corgeWithV1),
      corgeWithV1.copy(stringOrInt = StringWrapper("stringModified"))
    )

    val partialCorgeWithV2 = PartialCorge(
      stringOrInt =
        PartialEnum2Field.Value2Updated(Corge.StringOrInt.IntWrapper.PartialIntWrapper(PartialField.Updated(2)))
    )

    assertEquals(
      partialCorgeWithV2.applyPartialUpdate(corgeWithV2),
      corgeWithV2.copy(stringOrInt = Corge.StringOrInt.IntWrapper(2))
    )
  }

  test("Partial update of a different value of the enum must fail") {
    val partialCorgeWithV1 = PartialCorge(
      stringOrInt =
        PartialEnum2Field.Value2Updated(Corge.StringOrInt.IntWrapper.PartialIntWrapper(PartialField.Updated(2)))
    )
    interceptMessage[IllegalArgumentException](
      "Cannot update value 2 of StringWrapper(string) because it is not of type T2"
    )(partialCorgeWithV1.applyPartialUpdate(corgeWithV1))

    val partialCorgeWithV2 = PartialCorge(
      stringOrInt =
        PartialEnum2Field.Value1Updated(StringWrapper.PartialStringWrapper(PartialField.Updated("stringModified")))
    )
    interceptMessage[IllegalArgumentException](
      "Cannot update value 1 of IntWrapper(1) because it is not of type T1"
    )(partialCorgeWithV2.applyPartialUpdate(corgeWithV2))
  }

  test("Partial update with the other value of the enum must set the value") {
    val partialCorgeWithV1 = PartialCorge(
      stringOrInt = PartialEnum2Field.Value2Set(Corge.StringOrInt.IntWrapper(2))
    )
    assertEquals(
      partialCorgeWithV1.applyPartialUpdate(corgeWithV1),
      corgeWithV1.copy(stringOrInt = Corge.StringOrInt.IntWrapper(2))
    )

    val partialCorgeWithV2 = PartialCorge(
      stringOrInt = PartialEnum2Field.Value1Set(StringWrapper("brandNewString"))
    )
    assertEquals(
      partialCorgeWithV2.applyPartialUpdate(corgeWithV2),
      corgeWithV2.copy(stringOrInt = StringWrapper("brandNewString"))
    )
  }

  test("Setting the value of the enum to the same value type must fail") {
    val partialCorgeWithV1 = PartialCorge(
      stringOrInt = PartialEnum2Field.Value1Set(StringWrapper("brandNewString"))
    )
    interceptMessage[IllegalArgumentException](
      "Cannot set value 1 of StringWrapper(string) because it is already set"
    )(partialCorgeWithV1.applyPartialUpdate(corgeWithV1))

    val partialCorgeWithV2 = PartialCorge(
      stringOrInt = PartialEnum2Field.Value2Set(Corge.StringOrInt.IntWrapper(2))
    )
    interceptMessage[IllegalArgumentException](
      "Cannot set value 2 of IntWrapper(1) because it is already set"
    )(partialCorgeWithV2.applyPartialUpdate(corgeWithV2))
  }
}
