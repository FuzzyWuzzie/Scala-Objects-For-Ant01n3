name := "SOFA"

version := "0.1"

//scalaVersion := "2.11.1"
scalaVersion := "2.10.5"

scalacOptions ++= Seq(
	"-deprecation",
	"-feature",
//	"-Ydelambdafy:method",
	"-target:jvm-1.7",
	"-language:implicitConversions"
)

fork := true

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

// Needed since scala 2.11

//libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

//libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.4.1"

libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.4.1"

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.4"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

//libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.0" % "test"

// I found no way to only use the "classifier" to force ivy to download the native elements.
// However when using only the classifier "native...", the builder seems to ignore the main jar and compile
// using the only native jar. Using the classifier "" seems to solve the problem, although it is ugly.

libraryDependencies += "org.jogamp.gluegen" % "gluegen-rt" % "2.2.4" classifier "natives-macosx-universal" classifier ""

libraryDependencies += "org.jogamp.jogl" % "jogl-all" % "2.2.4" classifier "natives-macosx-universal" classifier ""
