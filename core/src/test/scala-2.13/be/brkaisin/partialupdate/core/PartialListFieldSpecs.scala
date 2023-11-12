package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models._

import java.util.UUID

class PartialListFieldSpecs extends munit.FunSuite {
  val id1: UUID = UUID.randomUUID()
  val id2: UUID = UUID.randomUUID()
  val id3: UUID = UUID.randomUUID()
  val id4: UUID = UUID.randomUUID()

  val foo1: Foo = Foo(
    string = "string",
    int = 1,
    maybeString = Some("maybeString"),
    maybeInt = Some(2)
  )

  val foo2: Foo = Foo(
    string = "string2",
    int = 3,
    maybeString = Some("maybeString2"),
    maybeInt = Some(4)
  )

  val complete: Qux = Qux(
    ids = List(id1, id2),
    foos = List(foo1, foo2)
  )

  test("Update of a 'simple' partial list field works") {

    // ids (simple partial list field) are updated, foos are unchanged
    val partial = PartialQux(
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
      partial.applyPartialUpdate(complete),
      complete.copy(
        ids = List(id4, id2, id3, id1)
      )
    )

  }

  test("Update of a 'nested' partial list field works") {

    // foos (nested partial list field) are updated, ids are unchanged
    val partial = PartialQux(
      ids = PartialListField.Unchanged(),
      foos = PartialListField.ElemsUpdated(
        List(
          ListOperation.ElemUpdated(
            0,
            PartialFoo(
              string = PartialField.Updated("stringModified"),
              int = PartialField.Updated(12),
              maybeString = PartialOptionalField.Deleted(),
              maybeInt = PartialOptionalField.Deleted()
            )
          ),
          ListOperation.ElemDeleted(1),
          ListOperation.ElemAdded(
            None, // will be added at the end
            Foo(
              string = "string3",
              int = 4,
              maybeString = Some("maybeString3"),
              maybeInt = Some(5)
            )
          ),
          ListOperation.ElemAdded(
            Some(0), // will be added at the beginning
            Foo(
              string = "string4",
              int = 6,
              maybeString = Some("maybeString4"),
              maybeInt = Some(7)
            )
          )
        )
      )
    )

    assertEquals(
      partial.applyPartialUpdate(complete),
      complete.copy(
        foos = List(
          Foo(
            string = "string4",
            int = 6,
            maybeString = Some("maybeString4"),
            maybeInt = Some(7)
          ),
          Foo(
            string = "stringModified",
            int = 12,
            maybeString = None,
            maybeInt = None
          ),
          Foo(
            string = "string3",
            int = 4,
            maybeString = Some("maybeString3"),
            maybeInt = Some(5)
          )
        )
      )
    )
  }

  test("Reorder of a 'simple' partial list field works") {

    // ids (simple partial list field) are reordered, foos are unchanged
    val partial = PartialQux(
      ids = PartialListField.ElemsReordered(
        List(1, 0)
      ),
      foos = PartialListField.Unchanged()
    )

    assertEquals(
      partial.applyPartialUpdate(complete),
      complete.copy(
        ids = List(id2, id1)
      )
    )

  }

  test("Reorder of a 'nested' partial list field works") {

    // foos (nested partial list field) are reordered, ids are unchanged
    val partial = PartialQux(
      ids = PartialListField.Unchanged(),
      foos = PartialListField.ElemsReordered(
        List(1, 0)
      )
    )

    assertEquals(
      partial.applyPartialUpdate(complete),
      complete.copy(
        foos = List(foo2, foo1)
      )
    )

  }
}
