package be.brkaisin.partialupdate.core

import be.brkaisin.partialupdate.models._
import io.circe.parser._

class PartialUpdateSpecs extends munit.FunSuite {
  val complete: Foo = Foo(
    string = "string",
    int = 1,
    maybeString = Some("maybeString"),
    maybeInt = Some(2)
  )

  test("Partial update of no field must return the same value") {
    val partial = PartialFoo(
      string = PartialField.Unchanged(),
      int = PartialField.Unchanged(),
      maybeString = PartialOptionalField.Unchanged(),
      maybeInt = PartialOptionalField.Unchanged()
    )

    assertEquals(partial.applyPartialUpdate(complete), complete)
  }

  test("Partial update of one field must only update this field") {
    val partial = PartialFoo(
      string = PartialField.Updated("stringModified"),
      int = PartialField.Unchanged(),
      maybeString = PartialOptionalField.Unchanged(),
      maybeInt = PartialOptionalField.Unchanged()
    )

    assertEquals(partial.applyPartialUpdate(complete), complete.copy(string = "stringModified"))
  }

  test("Partial update of two fields must only update these fields") {
    val partial = PartialFoo(
      string = PartialField.Updated("stringModified"),
      int = PartialField.Updated(12),
      maybeString = PartialOptionalField.Unchanged(),
      maybeInt = PartialOptionalField.Unchanged()
    )

    assertEquals(partial.applyPartialUpdate(complete), complete.copy(string = "stringModified", int = 12))
  }

  test("Partial deletion of one optional field must only delete this field") {
    val partial = PartialFoo(
      string = PartialField.Unchanged(),
      int = PartialField.Unchanged(),
      maybeString = PartialOptionalField.Deleted(),
      maybeInt = PartialOptionalField.Unchanged()
    )

    assertEquals(partial.applyPartialUpdate(complete), complete.copy(maybeString = None))
  }

  test("Partial deletion of two optional fields must only delete these fields") {
    val partial = PartialFoo(
      string = PartialField.Unchanged(),
      int = PartialField.Unchanged(),
      maybeString = PartialOptionalField.Deleted(),
      maybeInt = PartialOptionalField.Deleted()
    )

    assertEquals(partial.applyPartialUpdate(complete), complete.copy(maybeString = None, maybeInt = None))
  }

  test("Update of a partial nested field works") {
    val complete: Bar = Bar(
      foo = Foo(
        string = "string",
        int = 1,
        maybeString = Some("maybeString"),
        maybeInt = Some(2)
      ),
      maybeBoolean = Some(true)
    )

    val partial = PartialBar(
      foo = PartialNestedField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified"),
          int = PartialField.Updated(12),
          maybeString = PartialOptionalField.Unchanged(),
          maybeInt = PartialOptionalField.Deleted()
        )
      ),
      maybeBoolean = PartialOptionalField.Deleted()
    )

    assertEquals(
      partial.applyPartialUpdate(complete),
      complete.copy(
        foo = complete.foo.copy(string = "stringModified", int = 12, maybeInt = None),
        maybeBoolean = None
      )
    )
  }

  test("Update of a partial optional nested field works") {
    val completeBabarWithFoo: Babar = Babar(
      maybeFoo = Some(
        Foo(
          string = "string",
          int = 1,
          maybeString = Some("maybeString"),
          maybeInt = Some(2)
        )
      )
    )

    val completeBabarWithoutFoo: Babar = Babar(
      maybeFoo = None
    )

    // 1. Update the optional nested field with Some partial from a defined value

    val partialWithSome = PartialBabar(
      maybeFoo = PartialOptionalField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified"),
          int = PartialField.Updated(12),
          maybeString = PartialOptionalField.Unchanged(),
          maybeInt = PartialOptionalField.Deleted()
        )
      )
    )

    assertEquals(
      partialWithSome.applyPartialUpdate(completeBabarWithFoo),
      completeBabarWithFoo.copy(
        maybeFoo = Some(completeBabarWithFoo.maybeFoo.get.copy(string = "stringModified", int = 12, maybeInt = None))
      )
    )

    // 2. Setting the optional nested field with Some partial from a None value
    val brandNewFoo = Foo(
      string = "stringSet",
      int = 12,
      maybeString = Some("maybeStringSet"),
      maybeInt = None
    )
    val partialBabarWithSomeFromNone = PartialBabar(maybeFoo = PartialOptionalField.Set(brandNewFoo))

    assertEquals(
      partialBabarWithSomeFromNone.applyPartialUpdate(completeBabarWithoutFoo),
      completeBabarWithoutFoo.copy(maybeFoo = Some(brandNewFoo))
    )

    // 3. Updating an optional nested field that is currently None fails
    val partialBabarWithSomeUpdatedFromNone = PartialBabar(
      maybeFoo = PartialOptionalField.Updated(
        PartialFoo(
          string = PartialField.Updated("stringModified"),
          int = PartialField.Updated(12),
          maybeString = PartialOptionalField.Updated(PartialField.Updated("maybeStringModified")),
          maybeInt = PartialOptionalField.Deleted()
        )
      )
    )

    interceptMessage[IllegalArgumentException](
      "Cannot update a value that is not set."
    )(partialBabarWithSomeUpdatedFromNone.applyPartialUpdate(completeBabarWithoutFoo))

    // 4. Update the optional nested field with None partial from a defined value
    val partialWithNone = PartialBabar(
      maybeFoo = PartialOptionalField.Deleted()
    )

    assertEquals(
      partialWithNone.applyPartialUpdate(completeBabarWithFoo),
      completeBabarWithFoo.copy(
        maybeFoo = None
      )
    )

    // 5. Update the optional nested field with None partial from a None value
    val partialBabarWithNoneFromNone = PartialBabar(
      maybeFoo = PartialOptionalField.Deleted()
    )

    interceptMessage[IllegalArgumentException](
      "Cannot delete a value that is not set."
    )(partialBabarWithNoneFromNone.applyPartialUpdate(completeBabarWithoutFoo))
  }

  test("Update of a partial list field works") {
    val id1 = java.util.UUID.randomUUID()
    val id2 = java.util.UUID.randomUUID()
    val id3 = java.util.UUID.randomUUID()

    val complete: Baz = Baz(
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

    // 1. Elems are updated
    val partial = PartialBaz(
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
      partial.applyPartialUpdate(complete),
      complete.copy(
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

  // todo: move these tests to a separate file in package be.brkaisin.partialupdate.circe
  test("Simple partial update from a JSON works") {
    val json = """{"string":"stringModified","maybeInt":null}"""

    val partial = decode[PartialFoo](json).toTry.get

    assertEquals(
      partial.applyPartialUpdate(complete),
      complete.copy(string = "stringModified", int = 1, maybeInt = None)
    )
  }

  test("Nested partial update from a JSON works") {
    // 1. Modifying/Deleting some fields in the nested object
    val json = """{"foo":{"string":"stringModified","maybeInt":null},"maybeBoolean":null}"""

    val partial = decode[PartialBar](json).toTry.get

    assertEquals(
      partial.applyPartialUpdate(Bar(Foo("string", 1, Some("maybeString"), Some(2)), Some(true))),
      Bar(Foo("stringModified", 1, Some("maybeString"), None), None)
    )

    // 2. Not modifying the nested object
    val json2 = """{"maybeBoolean":null}"""

    val partial2 = decode[PartialBar](json2).toTry.get

    assertEquals(
      partial2.applyPartialUpdate(Bar(Foo("string", 1, Some("maybeString"), Some(2)), Some(true))),
      Bar(Foo("string", 1, Some("maybeString"), Some(2)), None)
    )
  }

  test("Optional nested update from a JSON works") {
    // 1. With a defined value
    val json = """{"maybeFoo":{"string":"stringModified","maybeInt":null}}"""

    val partial = decode[PartialBabar](json).toTry.get

    assertEquals(
      partial.applyPartialUpdate(Babar(Some(Foo("string", 1, Some("maybeString"), Some(2))))),
      Babar(Some(Foo("stringModified", 1, Some("maybeString"), None)))
    )

    // 2. With a None value
    val json2 = """{}"""

    val partial2 = decode[PartialBabar](json2).toTry.get

    // 2.1. Initial value is undefined
    assertEquals(
      partial2.applyPartialUpdate(Babar(None)),
      Babar(None)
    )

    // 2.2. Initial value is defined
    assertEquals(
      partial2.applyPartialUpdate(Babar(Some(Foo("string", 1, Some("maybeString"), Some(2))))),
      Babar(Some(Foo("string", 1, Some("maybeString"), Some(2))))
    )
  }
}
