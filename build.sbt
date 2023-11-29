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

val scalaReflectDep = "org.scala-lang" % "scala-reflect" % "2.13.12" % Provided

val magnoliaDeps = Seq(
  "com.softwaremill.magnolia1_2" %% "magnolia" % "1.1.6",
  scalaReflectDep
)

val testDeps = Seq(
  "org.scalacheck" %% "scalacheck" % "1.17.0",
  "org.scalameta" %% "munit" % "0.7.29"
).map(_ % Test)

lazy val core = project
  .in(file("core"))
  .settings(
    libraryDependencies ++= magnoliaDeps ++ testDeps
  )

lazy val circe = project
  .in(file("circe"))
  .settings(
    libraryDependencies ++= circeDeps ++ testDeps
  )
  .dependsOn(core, core % "test -> test")

lazy val diff = project
  .in(file("diff"))
  .settings(
    libraryDependencies ++= List(scalaReflectDep) ++ testDeps
  )
  .dependsOn(core, core % "test -> test")
