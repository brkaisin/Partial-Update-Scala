name := "Partial-Update"

version := "0.1"

scalaVersion := "2.13.12"

Compile / scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-Wvalue-discard",
  "-Ywarn-dead-code"
)

val circeVersion    = "0.14.3"
val circeDependency = "io.circe" %% (_: String) % circeVersion
def circeDependencies =
  List(
    circeDependency("circe-core"),
    circeDependency("circe-generic"),
    circeDependency("circe-parser"),
    circeDependency("circe-shapes"),
    circeDependency("circe-generic-extras")
  )

val magnoliaDependencies = Seq(
  "com.softwaremill.magnolia1_2" %% "magnolia" % "1.1.6",
  "org.scala-lang" % "scala-reflect" % "2.13.12" % Provided
)

val testDependencies = Seq(
  "org.scalacheck" %% "scalacheck" % "1.17.0",
  "org.scalameta" %% "munit" % "0.7.29"
).map(_ % Test)

libraryDependencies ++= circeDependencies ++ magnoliaDependencies ++ testDependencies
