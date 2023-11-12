package be.brkaisin.partialupdate.models

import be.brkaisin.partialupdate.util.Identifiable

final case class IdentifiableFoo(id: java.util.UUID, string: String, int: Int)
    extends Identifiable[IdentifiableFoo, java.util.UUID]
