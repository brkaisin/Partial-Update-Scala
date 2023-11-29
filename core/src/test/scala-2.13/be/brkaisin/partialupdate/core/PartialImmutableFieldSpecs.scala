package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models.{Bar, Foo, ImmutableFoo, PartialImmutableFoo}

class PartialImmutableFieldSpecs extends munit.FunSuite {
  val foo1: Foo = Foo(string = "string", int = 1)

  val foo2: Foo = Foo(string = "string", int = 2)

  val immutableFoo: ImmutableFoo = ImmutableFoo(
    string = "string",
    bar = Bar(foo1 = foo1, foo2 = foo2)
  )

  test("Partial update of no field must return the same value") {
    val partialImmutableFoo = PartialImmutableFoo(
      string = PartialImmutableField.Unchanged(),
      bar = PartialImmutableField.Unchanged()
    )

    assertEquals(partialImmutableFoo.applyPartialUpdate(immutableFoo), immutableFoo)
  }
}
