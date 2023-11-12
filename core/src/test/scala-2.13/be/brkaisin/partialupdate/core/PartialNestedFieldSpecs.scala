package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models.{Bar, Foo, PartialBar, PartialFoo}

class PartialNestedFieldSpecs extends munit.FunSuite {
  val bar: Bar = Bar(
    foo1 = Foo(
      string = "string1",
      int = 1
    ),
    foo2 = Foo(
      string = "string2",
      int = 2
    )
  )

  test("Partial update of no field must return the same value") {
    val partialBar = PartialBar(
      foo1 = PartialNestedField.Unchanged(),
      foo2 = PartialNestedField.Unchanged()
    )

    assertEquals(partialBar.applyPartialUpdate(bar), bar)
  }

  test("Partial update of one field must only update this field") {
    val partialBar = PartialBar(
      foo1 = PartialNestedField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified"),
          int = PartialField.Unchanged()
        )
      ),
      foo2 = PartialNestedField.Unchanged()
    )

    assertEquals(partialBar.applyPartialUpdate(bar), bar.copy(foo1 = bar.foo1.copy(string = "stringModified")))
  }

  test("Partial update of two fields must (only) update these fields") {
    val partialBar = PartialBar(
      foo1 = PartialNestedField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified"),
          int = PartialField.Updated(3)
        )
      ),
      foo2 = PartialNestedField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified2"),
          int = PartialField.Updated(4)
        )
      )
    )

    assertEquals(
      partialBar.applyPartialUpdate(bar),
      bar.copy(
        foo1 = bar.foo1.copy(string = "stringModified", int = 3),
        foo2 = bar.foo2.copy(string = "stringModified2", int = 4)
      )
    )
  }

}
