package be.brkaisin.partialupdate.circe

import io.circe.parser.decode
import be.brkaisin.partialupdate.models._
import Codecs._

class CodecsSpecs extends munit.FunSuite {
  // todo: improve and continue these tests. It is not ok to check the result of the applyPartialUpdate method,
  // because it has already been tested in the core module. We should only check the result of the partialCodec instead.
  test("Simple partial update from a JSON works") {
    val complete: Foo = Foo(
      string = "string",
      int = 1
    )

    val json = """{"string":"stringModified","maybeInt":null}"""

    val partial = decode[PartialFoo](json).toTry.get

    assertEquals(
      partial.applyPartialUpdate(complete),
      complete.copy(string = "stringModified", int = 1)
    )
  }

  test("Nested partial update from a JSON works") {
    val bar = Bar(Foo("string1", 1), Foo("string2", 2))

    // 1. Modifying/Deleting some fields in the nested object
    val json = """{"foo1":{"string":"stringModified","maybeInt":null},"maybeBoolean":null}"""

    val partial = decode[PartialBar](json).toTry.get

    assertEquals(
      partial.applyPartialUpdate(bar),
      Bar(bar.foo1.copy(string = "stringModified"), bar.foo2)
    )

    // 2. Not modifying the nested object
    val json2 = """{}"""

    val partial2 = decode[PartialBar](json2).toTry.get

    assertEquals(
      partial2.applyPartialUpdate(bar),
      bar
    )
  }

  test("Optional nested update from a JSON works") {
    // 1. With a defined value
    val json = """{"maybeFoo":{"string":"stringModified","maybeInt":null}}"""

    val partial = decode[PartialBabar](json).toTry.get

    assertEquals(
      partial.applyPartialUpdate(Babar(Some(Foo("string", 1)))),
      Babar(Some(Foo("stringModified", 1)))
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
      partial2.applyPartialUpdate(Babar(Some(Foo("string", 1)))),
      Babar(Some(Foo("string", 1)))
    )
  }
}
