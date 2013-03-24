name := "SOFA"

version := "0.1"

//scalaVersion := "2.9.2"

scalaVersion := "2.10.0"

fork := true

resolvers += "Jogamp" at "http://jogamp.org/deployment/maven"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "junit" % "junit" % "4.10"

//libraryDependencies += "org.jogamp.gluegen" % "gluegen-rt" % "2.0-rc9"

//libraryDependencies += "org.jogamp.jogl" % "jogl-all" % "2.0-rc9"

//scalacOptions ++= Seq("–optimise", "-deprecation")

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0"

libraryDependencies += "com.typesafe" % "config" % "1.0.0"
