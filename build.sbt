name := "SOFA"

version := "0.1"

scalaVersion := "2.9.2"

fork := true

resolvers += "Jogamp" at "http://jogamp.org/deployment/maven"

libraryDependencies += "junit" % "junit" % "4.10"

//libraryDependencies += "org.jogamp.gluegen" % "gluegen-rt" % "2.0-rc9"

//libraryDependencies += "org.jogamp.jogl" % "jogl-all" % "2.0-rc9"

scalacOptions += "–optimise"