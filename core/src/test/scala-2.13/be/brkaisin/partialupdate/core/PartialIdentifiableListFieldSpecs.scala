package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models._

import java.util.UUID

class PartialIdentifiableListFieldSpecs extends munit.FunSuite {
  val id1: UUID = UUID.randomUUID()
  val id2: UUID = UUID.randomUUID()
  val id3: UUID = UUID.randomUUID()

  val baz: Baz = Baz(
    foos = List(
      IdentifiableFoo(
        id = id1,
        string = "string",
        int = 1
      ),
      IdentifiableFoo(
        id = id2,
        string = "string2",
        int = 3
      )
    )
  )

  test("Partial update of a partial identifiable list field works when the list is updated") {
    val partialBaz = PartialBaz(
      foos = PartialIdentifiableListField.ElemsUpdated(
        List(
          IdentifiableListOperation.ElemUpdated(
            id1,
            PartialIdentifiableFoo(
              string = PartialField.Updated("stringModified"),
              int = PartialField.Updated(12)
            )
          ),
          IdentifiableListOperation.ElemDeleted(id2),
          IdentifiableListOperation.ElemAdded(
            id3,
            IdentifiableFoo(
              id = id3,
              string = "string3",
              int = 4
            )
          )
        )
      )
    )

    assertEquals(
      partialBaz.applyPartialUpdate(baz),
      baz.copy(
        foos = List(
          IdentifiableFoo(
            id = id1,
            string = "stringModified",
            int = 12
          ),
          IdentifiableFoo(
            id = id3,
            string = "string3",
            int = 4
          )
        )
      )
    )
  }

  test("Partial update of a partial identifiable list field works when the list is reordered with a valid order") {
    val partialBaz = PartialBaz(
      foos = PartialIdentifiableListField.ElemsReordered(
        List(
          id2,
          id1
        )
      )
    )

    assertEquals(
      partialBaz.applyPartialUpdate(baz),
      baz.copy(
        foos = List(
          IdentifiableFoo(
            id = id2,
            string = "string2",
            int = 3
          ),
          IdentifiableFoo(
            id = id1,
            string = "string",
            int = 1
          )
        )
      )
    )
  }

  test("Partial update of a partial identifiable list field works when the list is reordered with an invalid order") {
    val partialBaz = PartialBaz(
      foos = PartialIdentifiableListField.ElemsReordered(
        List(
          id3, // id3 is not in the list
          id1
        )
      )
    )

    intercept[IllegalArgumentException] {
      partialBaz.applyPartialUpdate(baz)
    }
  }

  test("Partial update of a partial identifiable list field works when the list is unchanged") {
    val partialBaz = PartialBaz(
      foos = PartialIdentifiableListField.Unchanged()
    )

    assertEquals(
      partialBaz.applyPartialUpdate(baz),
      baz
    )
  }

}
