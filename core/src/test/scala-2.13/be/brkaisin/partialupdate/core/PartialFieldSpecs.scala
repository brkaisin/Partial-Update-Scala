package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models.{Foo, PartialFoo}

class PartialFieldSpecs extends munit.FunSuite {
  val foo: Foo = Foo(
    string = "string",
    int = 1
  )

  test("Partial update of no field must return the same value") {
    val partialFoo = PartialFoo(
      string = PartialField.Unchanged(),
      int = PartialField.Unchanged()
    )

    assertEquals(partialFoo.applyPartialUpdate(foo), foo)
  }

  test("Partial update of one field must only update this field") {
    val partialFoo = PartialFoo(
      string = PartialField.Updated("stringModified"),
      int = PartialField.Unchanged()
    )

    assertEquals(partialFoo.applyPartialUpdate(foo), foo.copy(string = "stringModified"))
  }

  test("Partial update of two fields must (only) update these fields") {
    val partialFoo = PartialFoo(
      string = PartialField.Updated("stringModified"),
      int = PartialField.Updated(2)
    )

    assertEquals(partialFoo.applyPartialUpdate(foo), foo.copy(string = "stringModified", int = 2))
  }
}
