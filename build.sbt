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

inThisBuild(
  List(
    name := "Partial-Update",
    organization := "be.brkaisin",
    description := "A tiny library to derive, apply and serialize partial updates on case classes",
    version := "0.1",
    homepage := Some(url("https://github.com/brkaisin/Partial-Update-Scala")),
    licenses := List("MIT" -> url("http://www.opensource.org/licenses/mit-license.php")),
    developers := List(
      Developer(
        "brkaisin",
        "Brieuc Kaisin",
        "kaisin.brieuc@gmail.com",
        url("https://github.com/brkaisin")
      )
    ),
    crossScalaVersions := Seq("2.13.12"), // todo: Scala 3
    scalaVersion := crossScalaVersions.value.head
  )
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

val testDeps = Seq(
  "org.scalacheck" %% "scalacheck" % "1.17.0",
  "org.scalameta" %% "munit" % "0.7.29"
).map(_ % Test)

lazy val core = project
  .in(file("core"))
  .settings(
    libraryDependencies ++= List(scalaReflectDep) ++ testDeps
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
