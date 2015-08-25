import com.typesafe.sbt.gzip.Import.gzip
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.pipeline.Pipeline
import playscalajs.PlayScalaJS.autoImport._
import playscalajs.ScalaJSPlay.autoImport._
import playscalajs.{PlayScalaJS, ScalaJSPlay}
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._


lazy val bintrayPublishIvyStyle = settingKey[Boolean]("=== !publishMavenStyle") //workaround for sbt-bintray bug

lazy val publishSettings = Seq(
  bintrayRepository := "denigma-releases",
  bintrayOrganization := Some("denigma"),
  licenses += ("MPL-2.0", url("http://opensource.org/licenses/MPL-2.0")),
  bintrayPublishIvyStyle := true
)

/**
 * For parts of the project that we will not publish
 */
lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)


//settings for all the projects
lazy val commonSettings = Seq(
  scalaVersion := Versions.scala,
  organization := "org.denigma",
  scalacOptions ++= Seq( "-feature", "-language:_" ),
  resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases"), //for scala-js-binding
  libraryDependencies ++= Dependencies.testing.value,
  unmanagedClasspath in Compile <++= unmanagedResources in Compile,
  updateOptions := updateOptions.value.withCachedResolution(true) //to speed up dependency resolution
)

lazy val bindingMacro = crossProject
  .crossType(CrossType.Full)
  .in(file("macroses"))
  .settings(  commonSettings ++ publishSettings:_* )
  .settings(
    version := Versions.macroBinding,
    name := "binding-macro",
    scalaVersion:=Versions.scala,
    libraryDependencies ++= Dependencies.macroses.shared.value,
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies += compilerPlugin("org.scalamacros" % "paradise" % Versions.macroParadise cross CrossVersion.full)
  )
  .jvmSettings(
    libraryDependencies ++= Dependencies.macroses.jvm.value
  )
  .jsSettings(
    libraryDependencies ++= Dependencies.macroses.js.value,
    jsDependencies += RuntimeDOM % "test"
  )

val macroJS = bindingMacro.js
val macroJVM = bindingMacro.jvm


lazy val binding = crossProject
  .crossType(CrossType.Full)
  .in(file("binding"))
  .settings(  commonSettings ++ publishSettings:_* )
  .settings(
    version := Versions.binding,
    name := "binding",
    scalaVersion:=Versions.scala,
    libraryDependencies ++= Dependencies.binding.shared.value
  )
  .jsSettings(
    libraryDependencies ++= Dependencies.binding.js.value,
    jsDependencies += RuntimeDOM % "test"
  )
  .jvmSettings(  libraryDependencies ++= Dependencies.binding.jvm.value )
  .dependsOn(bindingMacro)


lazy val bindingJS = binding.js
lazy val bindingJVM = binding.jvm


lazy val controls = crossProject
  .crossType(CrossType.Full)
  .in(file("controls"))
  .settings(  commonSettings ++ publishSettings:_* )
  .settings(
    version := Versions.controls,
    name := "binding-controls",
    scalaVersion:=Versions.scala
  )
  .jsSettings(
    libraryDependencies ++= Dependencies.controls.js.value,
    jsDependencies += RuntimeDOM % "test"
  )
  .jvmSettings(  libraryDependencies ++= Dependencies.controls.jvm.value )
  .jvmConfigure(p=>p.enablePlugins(SbtTwirl))
  .dependsOn(binding)



lazy val controlsJS = controls.js
lazy val controlsJVM = controls.jvm


lazy val semantic = crossProject
  .crossType(CrossType.Full)
  .in(file("semantic"))
  .settings(  commonSettings ++ publishSettings:_* )
  .settings(
    version := Versions.binding,
    name := "semantic-controls",
    scalaVersion:=Versions.scala
  )
  .jsSettings(
    libraryDependencies ++= Dependencies.semantic.js.value,
    jsDependencies += RuntimeDOM % "test"
  )
  .jvmSettings(  libraryDependencies ++= Dependencies.semantic.jvm.value )
  .jvmConfigure(p=>p.enablePlugins(SbtTwirl))
  .dependsOn(controls)


lazy val semanticJS = semantic.js
lazy val semanticJVM   = semantic.jvm

val scalaJSDevStage  = Def.taskKey[Pipeline.Stage]("Apply fastOptJS on all Scala.js projects")

def scalaJSDevTaskStage: Def.Initialize[Task[Pipeline.Stage]] = Def.task { mappings: Seq[PathMapping] =>
  mappings ++ PlayScalaJS.devFiles(Compile).value ++ PlayScalaJS.sourcemapScalaFiles(fastOptJS).value
}

lazy val preview = crossProject
		.crossType(CrossType.Full)
		.in(file("preview"))
		.settings(commonSettings++publishSettings: _*)
		.settings(
			name := "preview"
		)
		.jsConfigure(p=>p.enablePlugins(ScalaJSPlay))
		.jsSettings(
			persistLauncher in Compile := true,
			persistLauncher in Test := false,
			libraryDependencies ++= Dependencies.previewJS.value,
			jsDependencies += RuntimeDOM % "test",
			sourceMapsDirectories :=Seq(
			  (baseDirectory in semanticJS).value ,
			  (baseDirectory in controlsJS).value ,
			  (baseDirectory in bindingJS).value ,
			  (baseDirectory in macroJS).value
			  )
		)
		.jvmSettings(Revolver.settings:_*)
		.jvmConfigure(p=>p.enablePlugins(SbtTwirl,SbtWeb).enablePlugins(PlayScalaJS)) //despite "Play" in name it is actually sbtweb-related plugin
		.jvmSettings(
			libraryDependencies ++= Dependencies.akka.value ++ Dependencies.webjars.value,
			mainClass in Compile :=Some("org.denigma.preview.Main"),
			mainClass in Revolver.reStart := Some("org.denigma.preview.Main"),
			scalaJSDevStage := scalaJSDevTaskStage.value,
			//pipelineStages := Seq(scalaJSProd,gzip),
			(emitSourceMaps in fullOptJS) := true,
			pipelineStages in Assets := Seq(scalaJSDevStage,gzip), //for run configuration
			(fullClasspath in Runtime) += (packageBin in Assets).value //to package production deps
		).dependsOn(semantic,controls)

lazy val previewJS = preview.js
lazy val previewJVM = preview.jvm settings( scalaJSProjects := Seq(previewJS) )


lazy val root = Project("root",file("."),settings = commonSettings)
  .settings(
    mainClass in Compile := (mainClass in previewJVM in Compile).value,
    (fullClasspath in Runtime) += (packageBin in previewJVM in Assets).value
  ) dependsOn previewJVM aggregate(previewJVM, previewJS)