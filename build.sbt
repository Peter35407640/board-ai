name := "boardai"
organization := "io.github.pbanasiak"

version := "0.1.0"

scalaVersion := "3.4.2"

//val dl4jVersion =  "1.0.0-M2.1"
val dl4jVersion = "1.0.0-M1.1"
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  // Core logging
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "org.fusesource.jansi" % "jansi" % "2.4.0", // Add this for console colors


  // DL4J dependencies for neural networks
  "org.deeplearning4j" % "deeplearning4j-core" % dl4jVersion,
  "org.deeplearning4j" % "deeplearning4j-nn" % dl4jVersion,

  // GPU Support - nd4j cuda  12.0 not supported
//    "org.nd4j" % "nd4j-native-platform" % dl4jVersion,
  "org.nd4j" % "nd4j-cuda-11.2-platform" % dl4jVersion,


  // Testing - updated for Scala 3
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)
