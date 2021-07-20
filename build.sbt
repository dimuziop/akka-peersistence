name := "akka_persistence"

version := "0.1"

scalaVersion := "2.13.6"

lazy val akkaVersion = "2.6.10" // must be 2.5.13 so that it's compatible with the stores plugins (JDBC and Cassandra)
lazy val leveldbVersion = "0.12"
lazy val leveldbjniVersion = "1.8"
lazy val postgresVersion = "42.2.23"
lazy val cassandraVersion = "1.0.5"
lazy val json4sVersion = "3.2.11"
lazy val protobufVersion = "3.6.1"
lazy val slickVersion = "3.3.3"
lazy val scalaTestVersion = "3.2.9"

// some libs are available in Bintray's JCenter
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,

  // local levelDB stores
  "org.iq80.leveldb" % "leveldb" % leveldbVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all" % leveldbjniVersion,

  // JDBC with PostgreSQL
  "org.postgresql" % "postgresql" % postgresVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.1",

 // Akka jdbc persistence dependencies
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,

  // Cassandra
  "com.typesafe.akka" %% "akka-persistence-cassandra" % cassandraVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % cassandraVersion % Test,
  // TEST TOOLS
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,

  // Google Protocol Buffers
  "com.google.protobuf" % "protobuf-java"  % protobufVersion,
)
