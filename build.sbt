name := "Partial-Update"

version := "0.1"

scalaVersion := "2.13.12"

val circeVersion = "0.14.3"
val circeDep     = "io.circe" %% (_: String) % circeVersion
def circeDeps =
  List(
    circeDep("circe-core"),
    circeDep("circe-generic"),
    circeDep("circe-parser"),
    circeDep("circe-shapes"),
    circeDep("circe-generic-extras")
  )

libraryDependencies ++= circeDeps ++ List(
  "com.propensive" %% "magnolia" % "0.17.0",
  "org.scala-lang" % "scala-reflect" % "2.13.12" % Provided
)

val testDependencies = Seq(
  "org.scalacheck" %% "scalacheck" % "1.17.0",
  "org.scalameta" %% "munit" % "0.7.29"
).map(_ % Test)

libraryDependencies ++= testDependencies
