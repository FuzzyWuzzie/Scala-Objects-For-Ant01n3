name := "SOFA"

version := "0.1"

scalaVersion := "2.10.1"

scalacOptions += "-deprecation"

scalacOptions += "-feature"

fork := true

resolvers += "Jogamp" at "http://jogamp.org/deployment/maven"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "junit" % "junit" % "4.10"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0"

libraryDependencies += "com.typesafe" % "config" % "1.0.0"
