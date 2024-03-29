# Partial update in Scala

**todo: adapt this README since Magnolia has been replaced by custom macros**

This repository is a tiny library for representing, deriving and performing partial updates of case classes in Scala,
as well as serializing and deserializing these operations.

The core of the library has zero dependencies. The library only uses [Circe](https://circe.github.io/circe/) for
serialization and deserialization, but this lies in a separate module so that the user can choose its own serialization
library.

## Motivation

Imagine you have case classes like these:

```scala
case class Address(street: String, city: String, country: String, zip: String)

case class Person(
                   id: Person.Id,
                   name: String,
                   nickname: Option[String],
                   address: Address,
                   bestFriends: List[Person],
                   otherFriends: Option[List[Person]]
                 ) extends Identifiable[Person, Person.Id] // Identifiable is a type class that provides an id field

object Person {
  type Id = Int
}

```

And you want to update them partially, like this:

```scala
// initial values
val jack = Person(1, "Jack", None, Address("Park Avenue", "New York", "USA", "12345"), List(), None)
val ray = Person(2, "Ray", None, Address("Long Street", "San Francisco", "USA", "23456"), List(), None)
val john =
  Person(3, "John", Some("Johnny"), Address("Main Street", "London", "UK", "34567"), List(jack), Some(List(ray)))

// partially updated values
val updatedRay = ray.copy(nickname = Some("Raymond"))

val updatedJohn = john.copy(
  nickname = None, // John no longer has a nickname
  address = john.address.copy(street = "Partial Street", zip = "54321"), // John moved to Partial Street
  bestFriends = john.bestFriends.filter(_.id != 1), // Jack is no longer a best friend of John
  otherFriends = john.otherFriends.map(
    _.updated(0, updatedRay) :+ jack // Ray is updated and Jack is added to the list of other friends
  )
)
```

As you can see, updating a person needs to create a new instance of the person, copying all the fields that are not
updated and updating the fields that are updated. When the case class is nested, the `copy` method needs to be called on
the nested case class, and this can become cumbersome, especially when the case class is nested several times.
Concerning this last point, there exist libraries like [Monocle](https://www.optics.dev/Monocle/) that provide lenses (
inspired from Haskell), which are a way to focus on a field of a case class and update it, simplifying the update of
nested case classes.

Another problem with this approach is when you need to partially update a list. Imagine that you have a list of `N`
elements and you want to update the `i`th element. You need to copy the list, and update the `i`th item with the dance
of `updated` and `copy` methods. Additionally, considering every list operation (add, update, delete, reorder), this
kind of gymnastic gives no guarantee that the list will be correctly updated after the operation. For example, you could
try to update or delete an element that does not exist in the list, or add an element that already exists. This could
lead to a list that is not consistent with the update operation that was performed on it. With the library, all these
mistakes will lead to errors at runtime.

Finally, when the partial update request comes from outside your application, you need to be able to represent the
update in a way that can be serialized and deserialized. If you have a deeply nested case class with a lot of fields and
the client wants to update only one field, it is more efficient to send only the field that is updated, instead of
sending the whole case class, and decode it with the case class decoder you have in your application. Moreover,
especially if your application relies on _event sourcing_, you may want to save the partial update in a database, in
order to be able to replay the events and rebuild the state of your application. On the one hand, it is more efficient
to save only the partial update, instead of the whole case class. On the other hand, by saving the whole case class, you
lose "direct" precision concerning the update, because you don't know which fields have been updated and which have not.
Thus, retaining only the part of the information that has changed will naturally lead to a more precise audit trail.

This library provides a convenient way to encode partial updates of case classes, and to apply them to it.
The defined ADT can be easily serialized and deserialized in order to be sent over the network or saved in a database.

Moreover, if you are looking for performance, it is also possible to update the current instance of a case class _in
place_, without creating a new instance, even if this is not idiomatic Scala.

## Usage

With the library, the previous example can be rewritten like this:

```scala
val partialUpdateRay: PartialPerson = PartialPerson(nickname = PartialOptionalField.Set("Raymond"))

val partialUpdateJohn: PartialPerson = PartialPerson(
  nickname = PartialOptionalField.Deleted(),
  address = PartialNestedField.Updated(
    PartialAddress(street = PartialField.Updated("Partial Street"), zip = PartialField.Updated("54321"))
  ),
  bestFriends = PartialIdentifiableListField.ElemsUpdated(operations = List(ElemDeleted(id = 1))),
  otherFriends = PartialOptionalField.Updated(
    PartialIdentifiableListField.ElemsUpdated(operations =
      List(ElemUpdated(id = 2, value = partialUpdateRay), ElemAdded(id = 1, value = jack))
    )
  )
)
```

Let's now focus on the class `PartialPerson`. It is a case class that has the same fields as `Person`, but the fields
are wrapped in other classes that represent the different types of partial updates that can be performed on a field.
The same applies to the class `PartialAddress`. The case classes are defined as follows:

```scala
case class PartialAddress(
                           street: PartialField[String] = PartialField.Unchanged(),
                           city: PartialField[String] = PartialField.Unchanged(),
                           country: PartialField[String] = PartialField.Unchanged(),
                           zip: PartialField[String] = PartialField.Unchanged()
                         ) extends Partial[Address] {
  // ...
}

case class PartialPerson(
                          name: PartialField[String] = PartialField.Unchanged(),
                          nickname: SimplePartialOptionalField[String] = PartialOptionalField.Unchanged(),
                          address: PartialNestedField[Address, PartialAddress] = PartialNestedField.Unchanged(),
                          bestFriends: PartialIdentifiableListField[Person.Id, Person, PartialPerson] =
                          PartialIdentifiableListField.Unchanged(),
                          otherFriends: PartialOptionalListField[Person.Id, Person, PartialPerson] = PartialOptionalField.Unchanged()
                        ) extends Partial[Person] {
  // ...
}

```

where `Partial` is a type class that represents a partial update,
and `PartialField`, `PartialNestedField`, `PartialOptionalField`, `PartialIdentifiableListField`, ... are type classes
that
represent the different types of partial updates that can be performed on a field. All these classes also
extend `Partial`, which allows to nest partial updates.

### Applying a partial update

The trait `Partial` is simply defined as follows:

```scala
trait Partial[T] {
  def applyPartialUpdate(currentValue: T): T
}

```

Probably the most important method of the library is `applyPartialUpdate`, which allows to apply a partial update to a
case class. This is where the magic happens. You can easily guess that such a method must be "recursive" in the sense
that it must be able to apply a partial update to a nested case class, and so on. Let's see how it is implemented for
`PartialAddress` and `PartialPerson`:

```scala
case class PartialAddress(
                           // ...
                         ) extends Partial[Address] {
  def applyPartialUpdate(currentValue: Address): Address =
    PartialUpdator[PartialAddress].updated(this, currentValue.copy())
}

case class PartialPerson(
                          // ...
                        ) extends Partial[Person] {
  def applyPartialUpdate(currentValue: Person): Person =
    PartialUpdator[PartialPerson].updated(this, currentValue.copy())
}

```

Two things to note here:

1. The magic relies on the `PartialUpdator` type class, which is derived
   by [Magnolia](https://github.com/softwaremill/magnolia). It will automatically apply the partial update recursively
   to the nested case classes until it reaches the leaves of the tree.
2. The presence of `.copy()` of the current value. This is because the library is designed to be able to update the
   current instance of a case class _in place_, without creating a new instance. This is of course not idiomatic Scala,
   but it can be useful in some cases, especially if you are not interested in the previous value of the case class
   after the update. **Be careful** though, as this can lead to horrible bugs if misused. With the copy, the library
   will create a new instance of the case class, and you will be safe.

### Write custom partial field types

**todo: explain how to write custom partial field types**

## Serialization

As mentioned before, it is often useful to be able to serialize and deserialize partial updates. The library provides
this feature out of the box, using [Circe](https://circe.github.io/circe/). This is an opionated choice and this is why
the implementation lies in
[a separate module](circe), allowing the user to choose its own serialization library without inheriting the Circe
dependency. Every partial field type has an encoder and a decoder which are combined by Circe to derive the encoder and
decoder of the case class that represents the partial update. Let's focus on each partial field type separately.

### Simple partial field type

For simple partial field types, represented with class `PartialField`, the codec will only encode and decode the value
if it is updated, and the decoder will only decode values that are present in the JSON. An unchanged value is therefore
not encoded. Since such a field do not represent an optional field, the decoder will fail if the value is equal
to `null`
in the JSON.

### Immutable partial field type

For immutable partial field types, represented with class `PartialImmutableField`, there is only one way to update the
value: by not changing it. The codec will therefore encode nothing, and the decoder will decode nothing.

### Partial nested field

For partial nested field types, represented with class `PartialNestedField`, the behavior is the same, except that the
encoded value is the partial nested field itself, and not the leaf value. The decoder will fail if the value is equal
to `null` in the JSON.

### Partial optional field

Compared to the two previous partial field types, an optional value can be set (first value) and deleted. When the value
is deleted, it is set to `null` in the JSON. Concerning the set and update operations, there is a need to differentiate
between the initial value and the updated value, since a partial value can either be decoded as a complete value or as
a partial value (because of the optional nature of the field). For this reason, when a value is first set, it must be
discriminated by a "initialValue" field in the JSON. When a value is updated, the encoded partial is directly encoded
as a JSON object. See more about this in the example below.

### Partial list field

A list partial update can consist in adding, updating, deleting or reordering elements. The library is designed such as
it is not possible to reorder elements at the same time as the three other operations. We thus have two types of partial
list field types: `PartialIdentifiableListField.ElemsUpdated` and `PartialIdentifiableListField.ElemsReordered`. The
first is only represented
with a list of operations, while the second is represented with a list of indexes. The codec will encode the list of
operations or indexes. In the second case, in order to recognize the type of operations in the JSON, the codec will add
a field "operation" in the JSON, which can take the values "add", "update" or "delete".

### Example

Let's go back to the previous example with Jack, Ray and John. As a reminder, we had:

```scala
val partialUpdateRay: PartialPerson = PartialPerson(nickname = PartialOptionalField.Set("Raymond"))

val partialUpdateJohn: PartialPerson = PartialPerson(
  nickname = PartialOptionalField.Deleted(),
  address = PartialNestedField.Updated(
    PartialAddress(street = PartialField.Updated("Partial Street"), zip = PartialField.Updated("54321"))
  ),
  bestFriends = PartialIdentifiableListField.ElemsUpdated(operations = List(ElemDeleted(id = 1))),
  otherFriends = PartialOptionalField.Updated(
    PartialIdentifiableListField.ElemsUpdated(operations =
      List(ElemUpdated(id = 2, value = partialUpdateRay), ElemAdded(id = 1, value = jack))
    )
  )
)
```

By defining the following implicit encoders and decoders:

```scala
import io.circe.generic.semiauto.deriveCodec
import be.brkaisin.partialupdate.circe.CirceCodecs._

implicit val addressCodec: Codec[Address] = deriveCodec
implicit val personCodec: Codec[Person] = deriveCodec

implicit val partialAddressCodec: Codec[PartialAddress] = partialCodec(deriveCodec)
implicit val partialPersonCodec: Codec[PartialPerson] = partialCodec(deriveCodec)
```

The partial updates are serialized as follows:

**Ray:**

```json
{
  "nickname": {
    "initialValue": "Raymond"
  }
}

```

**John:**

```json
{
  "nickname": null,
  "address": {
    "street": "Partial Street",
    "zip": "54321"
  },
  "bestFriends": [
    {
      "id": 1,
      "operation": "delete"
    }
  ],
  "otherFriends": [
    {
      "id": 2,
      "value": {
        "nickname": {
          "initialValue": "Raymond"
        }
      },
      "operation": "update"
    },
    {
      "id": 1,
      "value": {
        "id": 1,
        "name": "Jack",
        "nickname": null,
        "address": {
          "street": "Park Avenue",
          "city": "New York",
          "country": "USA",
          "zip": "12345"
        },
        "bestFriends": [
        ],
        "otherFriends": null
      },
      "operation": "add"
    }
  ]
}
```

## Partial update computation (diff)

Another [useful feature of the library](diff) is the possibility to compute the partial update that happened between two
instances of a case class. For every partial field type, we define a way to compute the partial update between two
instances of the same type. This is represented by the
trait [PartialDiffComputor](diff/src/main/scala-2.13/be/brkaisin/partialupdate/diff/PartialDiffComputor.scala).
This [file](diff/src/main/scala-2.13/be/brkaisin/partialupdate/diff/Implicits.scala) contains implicits for every
partial
field type of the library. Once you have these implicits in scope, you can derive the diff computor for a case class by
calling `PartialDiffComputorMacro.derive`:

```scala
final case class Foo(string: String, int: Int)

final case class PartialFoo(string: PartialField[String], int: PartialField[Int]) extends Partial[Foo] {
...
}

val fooPartialDiffComputor: PartialDiffComputor[Foo, PartialFoo] =
  PartialDiffComputorMacro.derivePartialDiffComputor // make it implicit if needed
```

As you can guess, Scala macros are used to derive the diff computor. When adding Scala 3 support, this will be replaced
by new meta-programming system. Once you have a diff computor, you can compute the partial update between two instances
of a case class by calling `computePartialDiff`:

```scala
val foo1 = Foo("foo", 1)
val foo2 = Foo("bar", 1)
val diff = fooPartialDiffComputor.computePartialDiff(foo1, foo2) // PartialFoo(string = Updated(bar), int = Unchanged())
```

### Note concerning partial list field types

**todo: explain that the computation of the diff between two lists is opinionated since there exists several ways to
compute the diff between two lists**

## Future work

- [ ] Scala 3 support (currently, the library is written for Scala 2.13 only).
- [x] Make the tests more unitary, by avoiding mixing different partial field types in the same basic tests, and
  splitting the tests in different files.
- [ ] Automatic code generation of the case classes that represent the partial updates.
- [ ] Add compilation type safety guarantying that the classes for partials updates are consistent with the case
  classes they represent (fields names, types, ...). Note that this would be solved with automatic code generation.
  [Shapeless](https://github.com/milessabin/shapeless) would be a good candidate for this, but it would also increase
  the compilation time, which is not desirable.
- [x] [Potentially] Remove Magnolia and replace it with custom meta-programming or Scala 3 built-in type class
  derivation.
- [x] Add a "module" for serialization and deserialization, allowing the user to choose its own serialization library.
- [x] Add the possibility to derive the partial update that happened between two instances of a case class. Now, this
  partial update is provided by the user. This new feature thus consists in computing a diff.
- [ ] Implement more partial fields types, like `PartialMapField`, `PartialSetField`, `PartialEnum2Field`, ...
- [ ] Implement an alternative for partial update that does not throw errors in the JVM but rather returns an `Either`
  or a `Try`. To be defined: do we accumulate errors (with an applicative functor) or do we stop at the first error (
  monadic approach)?