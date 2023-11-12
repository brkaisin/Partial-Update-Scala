package be.brkaisin.partialupdate.util

/**
  * A trait for types that have an id or can be uniquely identified in some way.
  * @tparam T the type of the class
  * @tparam Id the type of the id
  */
trait Identifiable[T, Id] { self: T =>
  def id: Id
}
