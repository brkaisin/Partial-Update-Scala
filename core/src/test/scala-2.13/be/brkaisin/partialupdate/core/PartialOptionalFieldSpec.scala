package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models.{Babar, Foo, PartialBabar, PartialFoo}

class PartialOptionalFieldSpec extends munit.FunSuite {
  val babarWithFoo: Babar = Babar(
    maybeFoo = Some(
      Foo(
        string = "string",
        int = 1
      )
    )
  )

  val babarWithoutFoo: Babar = Babar(
    maybeFoo = None
  )

  test("Partial update of Some optional value must update the value") {
    val partialBabar = PartialBabar(
      maybeFoo = PartialOptionalField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified"),
          int = PartialField.Updated(2)
        )
      )
    )

    assertEquals(
      partialBabar.applyPartialUpdate(babarWithFoo),
      babarWithFoo.copy(
        maybeFoo = Some(babarWithFoo.maybeFoo.get.copy(string = "stringModified", int = 2))
      )
    )
  }

  test("Setting the optional nested field from a None value works") {
    val brandNewFoo = Foo(
      string = "stringSet",
      int = 12
    )
    val partialBabarWithSomeFromNone = PartialBabar(maybeFoo = PartialOptionalField.Set(brandNewFoo))

    assertEquals(
      partialBabarWithSomeFromNone.applyPartialUpdate(babarWithoutFoo),
      babarWithoutFoo.copy(maybeFoo = Some(brandNewFoo))
    )
  }

  test("Setting the optional nested field from Some value fails") {
    val brandNewFoo = Foo(
      string = "stringSet",
      int = 12
    )
    val partialBabarWithSomeFromSome = PartialBabar(maybeFoo = PartialOptionalField.Set(brandNewFoo))

    interceptMessage[IllegalArgumentException](
      "Cannot set a value that is already set."
    )(partialBabarWithSomeFromSome.applyPartialUpdate(babarWithFoo))
  }

  test("Partial update of a None optional value must fail") {
    val partialBabar = PartialBabar(
      maybeFoo = PartialOptionalField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified"),
          int = PartialField.Updated(2)
        )
      )
    )
    interceptMessage[IllegalArgumentException](
      "Cannot update a value that is not set."
    )(partialBabar.applyPartialUpdate(babarWithoutFoo))
  }

  test("Delete a partial optional nested field works") {
    val partialBabar = PartialBabar(
      maybeFoo = PartialOptionalField.Deleted()
    )

    assertEquals(
      partialBabar.applyPartialUpdate(babarWithFoo),
      babarWithFoo.copy(maybeFoo = None)
    )
  }

  test("Delete a partial optional nested field from a None value fails") {
    val partialBabar = PartialBabar(
      maybeFoo = PartialOptionalField.Deleted()
    )

    interceptMessage[IllegalArgumentException](
      "Cannot delete a value that is not set."
    )(partialBabar.applyPartialUpdate(babarWithoutFoo))
  }
}
