name := "scala-grpc-zipkin-sample"

version := "0.1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-feature", "-deprecation")

val scalaPbVersion = "0.5.47"
val braveVersion = "4.0.7-SNAPSHOT"

PB.protocVersion := "-v320"

PB.targets in Compile := Seq(
  scalapb.gen(grpc=true) -> (sourceManaged in Compile).value
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq(
    "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % scalaPbVersion,
    "io.grpc" % "grpc-all" % "1.1.2",
    "com.google.protobuf" % "protobuf-java" % "3.2.0",
    "com.google.protobuf" % "protobuf-java-util" % "3.2.0",
    "com.trueaccord.scalapb" %% "scalapb-runtime" % scalaPbVersion % "protobuf",
    "io.zipkin.brave" % "brave-core" % braveVersion,
    "io.zipkin.brave" % "brave-grpc" % braveVersion,
    "io.zipkin.brave" % "brave-spancollector-http" % braveVersion,
    "io.zipkin.reporter" % "zipkin-sender-urlconnection" % "0.6.12"
)