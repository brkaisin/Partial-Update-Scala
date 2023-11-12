package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models.{Foo, PartialFoo, PartialQux, Qux}

import java.util.UUID

class PartialListFieldSpecs extends munit.FunSuite {
  val id1: UUID = UUID.randomUUID()
  val id2: UUID = UUID.randomUUID()
  val id3: UUID = UUID.randomUUID()
  val id4: UUID = UUID.randomUUID()

  val foo1: Foo = Foo(
    string = "string",
    int = 1
  )

  val foo2: Foo = Foo(
    string = "string2",
    int = 3
  )

  val qux: Qux = Qux(
    ids = List(id1, id2),
    foos = List(foo1, foo2)
  )

  test("Partial update of a 'simple' partial list field works when the list is updated") {

    // ids (simple partial list field) are updated, foos are unchanged
    val partialQux = PartialQux(
      ids = PartialListField.ElemsUpdated(
        List(
          ListOperation.ElemUpdated(
            0,
            PartialField.Updated(id3)
          ),
          ListOperation.ElemDeleted(1),
          ListOperation.ElemAdded(
            None, // will be added at the end
            id1
          ),
          ListOperation.ElemAdded(
            Some(0), // will be added at the beginning
            id4
          ),
          ListOperation.ElemAdded(
            Some(1), // will be added at index 1
            id2
          )
        )
      ),
      foos = PartialListField.Unchanged()
    )

    assertEquals(
      partialQux.applyPartialUpdate(qux),
      qux.copy(
        ids = List(id4, id2, id3, id1)
      )
    )

  }

  test("Partial update of a 'nested' partial list field works when the list is updated") {

    // foos (nested partial list field) are updated, ids are unchanged
    val partialQux = PartialQux(
      ids = PartialListField.Unchanged(),
      foos = PartialListField.ElemsUpdated(
        List(
          ListOperation.ElemUpdated(
            0,
            PartialFoo(
              string = PartialField.Updated("stringModified"),
              int = PartialField.Updated(12)
            )
          ),
          ListOperation.ElemDeleted(1),
          ListOperation.ElemAdded(
            None, // will be added at the end
            Foo(
              string = "string3",
              int = 4
            )
          ),
          ListOperation.ElemAdded(
            Some(0), // will be added at the beginning
            Foo(
              string = "string4",
              int = 6
            )
          )
        )
      )
    )

    assertEquals(
      partialQux.applyPartialUpdate(qux),
      qux.copy(
        foos = List(
          Foo(
            string = "string4",
            int = 6
          ),
          Foo(
            string = "stringModified",
            int = 12
          ),
          Foo(
            string = "string3",
            int = 4
          )
        )
      )
    )
  }

  test("Partial update of a 'simple' partial list field works when the list is reordered") {

    // ids (simple partial list field) are reordered, foos are unchanged
    val partialQux = PartialQux(
      ids = PartialListField.ElemsReordered(
        List(1, 0)
      ),
      foos = PartialListField.Unchanged()
    )

    assertEquals(
      partialQux.applyPartialUpdate(qux),
      qux.copy(
        ids = List(id2, id1)
      )
    )

  }

  test("Partial update of a 'nested' partial list field works when the list is reordered") {
    // foos (nested partial list field) are reordered, ids are unchanged
    val partialQux = PartialQux(
      ids = PartialListField.Unchanged(),
      foos = PartialListField.ElemsReordered(
        List(1, 0)
      )
    )

    assertEquals(
      partialQux.applyPartialUpdate(qux),
      qux.copy(
        foos = List(foo2, foo1)
      )
    )

  }
}
