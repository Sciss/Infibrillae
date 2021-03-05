import sbtcrossproject.Platform

lazy val projectVersion = "0.1.18-SNAPSHOT"

lazy val deps = new {
  val main = new {
    val audioFile       = "2.3.3"
    val dom             = "1.1.0"
//    val fscape          = "3.6.0-SNAPSHOT"
    val laminar         = "0.11.0"
    val lucre           = "4.4.3"
    val lucreSwing      = "2.6.2"
//    val plotly          = "0.8.1"
    val scalaJavaTime   = "2.2.0"
    val soundProcesses  = "4.7.0"
  }
}

lazy val platforms = Seq[Platform](JVMPlatform, JSPlatform)

lazy val root = crossProject(platforms: _*).in(file("."))
//  .enablePlugins(ScalaJSPlugin)
  .settings(
    name          := "in|fibrillae",
    organization  := "de.sciss",
    licenses      := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
    homepage      := Some(url("https://www.sciss.de/exp/infibrillae/")),
    description   := "A sound piece for the web browser",
    version       := projectVersion,
    scalaVersion  := "2.13.5",
//    resolvers += Resolver.bintrayRepo("cibotech", "public"),  // needed for EvilPlot
    libraryDependencies ++= Seq(
      "de.sciss"          %%% "audiofile"             % deps.main.audioFile,
//      "de.sciss"  %%% "fscape-lucre"          % deps.main.fscape,
      "de.sciss"          %%% "lucre-core"            % deps.main.lucre,
      "de.sciss"          %%% "lucre-expr"            % deps.main.lucre,
      "de.sciss"          %%% "lucre-swing"           % deps.main.lucreSwing,
      "de.sciss"          %%% "soundprocesses-core"   % deps.main.soundProcesses,
      "de.sciss"          %%% "soundprocesses-views"  % deps.main.soundProcesses,
    ),
  )
  .jsSettings(
    // This is an application with a main method
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js"      %%% "scalajs-dom"           % deps.main.dom,
      "com.raquo"         %%% "laminar"               % deps.main.laminar,
      "io.github.cquiroz" %%% "scala-java-time"       % deps.main.scalaJavaTime,
    ),
    artifactPath in(Compile, fastOptJS) := baseDirectory.value.getParentFile / "lib" / "main.js",
    artifactPath in(Compile, fullOptJS) := baseDirectory.value.getParentFile / "lib" / "main.js",
  )

