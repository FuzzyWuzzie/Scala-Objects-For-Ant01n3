name := "SOFA"

version := "0.1"

scalaVersion := "2.11.0"

scalacOptions += "-deprecation"

scalacOptions += "-feature" 

scalacOptions += "-Ydelambdafy:method"

scalacOptions += "-target:jvm-1.7"

fork := true

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

// Needed since scala 2.11

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.4"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"

// I found no way to only use the "classifier" to force ivy to download the native elements.
// However when using only the classifier "native...", the builder seems to ignore the main jar and compile
// using the only native jar. Using the classifier "" seems to solve the problem, although it is ugly.

libraryDependencies += "org.jogamp.gluegen" % "gluegen-rt" % "2.1.0" classifier "natives-macosx-universal" classifier ""

libraryDependencies += "org.jogamp.jogl" % "jogl-all" % "2.1.0" classifier "natives-macosx-universal" classifier ""
