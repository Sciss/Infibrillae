import sbtcrossproject.Platform

lazy val baseNameL      = "infibrillae"
lazy val projectVersion = "0.3.0-SNAPSHOT"

lazy val deps = new {
  val main = new {
    val audioFile       = "2.3.3"
    val dom             = "1.1.0"
//    val fscape          = "3.6.0-SNAPSHOT"
    val jhlabs          = "2.0.235"
    val laminar         = "0.11.0" // "0.12.2"
    val lucre           = "4.4.5"
    val lucreSwing      = "2.6.3"
//    val plotly          = "0.8.1"
    val pi4j            = "1.4"
    val scalaCollider   = "2.6.4"
    val scalaJavaTime   = "2.3.0"
    val scallop         = "4.0.3"
    val soundProcesses  = "4.8.0"
  }
}

lazy val platforms = Seq[Platform](JVMPlatform, JSPlatform)

lazy val buildInfoSettings = Seq(
  // ---- build info ----
  buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
    BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
    BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
  ),
  buildInfoOptions += BuildInfoOption.BuildTime
)

lazy val root = crossProject(platforms: _*).in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoSettings)
  .settings(
    name          := "in|fibrillae",
    organization  := "de.sciss",
    licenses      := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
    homepage      := Some(url("https://www.sciss.de/exp/infibrillae/")),
    description   := "A sound piece for the web browser",
    version       := projectVersion,
    scalaVersion  := "2.13.6",
    scalacOptions ++= Seq("-deprecation"),
//    resolvers += Resolver.bintrayRepo("cibotech", "public"),  // needed for EvilPlot
    libraryDependencies ++= Seq(
      "de.sciss"          %%% "audiofile"             % deps.main.audioFile,
//      "de.sciss"  %%% "fscape-lucre"          % deps.main.fscape,
      "de.sciss"          %%% "lucre-core"            % deps.main.lucre,
      "de.sciss"          %%% "lucre-expr"            % deps.main.lucre,
      "de.sciss"          %%% "lucre-swing"           % deps.main.lucreSwing,
      "de.sciss"          %%% "scalacollider"         % deps.main.scalaCollider,
      "de.sciss"          %%% "soundprocesses-core"   % deps.main.soundProcesses,
      "de.sciss"          %%% "soundprocesses-views"  % deps.main.soundProcesses,
    ),
    buildInfoPackage := "de.sciss.infibrillae",
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.jhlabs" %  "filters"   % deps.main.jhlabs,     // image composites
      "org.rogach" %% "scallop"   % deps.main.scallop,    // command line option parsing
      "com.pi4j"   %  "pi4j-core" % deps.main.pi4j,       // GPIO control
    ),
  )
  .jvmSettings(assemblySettings)
  .jsSettings(
    // This is an application with a main method
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js"      %%% "scalajs-dom"           % deps.main.dom,
      "com.raquo"         %%% "laminar"               % deps.main.laminar,
      "io.github.cquiroz" %%% "scala-java-time"       % deps.main.scalaJavaTime,
    ),
    Compile / fastOptJS / artifactPath := baseDirectory.value.getParentFile / "lib" / "main.js",
    Compile / fullOptJS / artifactPath := baseDirectory.value.getParentFile / "lib" / "main.js",
  )

lazy val appMainClass = Some("de.sciss.infibrillae.Infibrillae")

lazy val assemblySettings = Seq(
  // ---- assembly ----
  assembly / test            := {},
  assembly / mainClass       := appMainClass,
  assembly / target          := baseDirectory.value,
  assembly / assemblyJarName := s"$baseNameL.jar",
  assembly / assemblyMergeStrategy := {
    case "logback.xml" => MergeStrategy.last
    case PathList("org", "xmlpull", _ @ _*)              => MergeStrategy.first
    case PathList("org", "w3c", "dom", "events", _ @ _*) => MergeStrategy.first // bloody Apache Batik
    case PathList(ps @ _*) if ps.last endsWith "module-info.class" => MergeStrategy.first // bloody Jackson
    case x =>
      val old = (assembly / assemblyMergeStrategy).value
      old(x)
  },
  assembly / fullClasspath := (Test / fullClasspath).value // https://github.com/sbt/sbt-assembly/issues/27
)
