import play.sbt.PlayImport.PlayKeys
import uk.gov.hmrc.DefaultBuildSettings

val appName = "automated-export-system-notifications"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.8"
ThisBuild / scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s", // Silence all warnings in generated routes
  "-Xlint:all",
  "-Werror"
)
ThisBuild / scalafmtOnCompile := true

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test)
  .settings(PlayKeys.playDefaultPort := 5003)
  .settings(CodeCoverageSettings.settings)
  // needed to eliminate "-Flag set repeatedly" warnings
  .settings(scalacOptions ~= (options => options.distinct))
  .settings(
    addCommandAlias("runTestOnly", "run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes")
  )

lazy val it = project
  // gives the `it` project access to the `microservice` project's Test and Compile dependencies and source code
  .dependsOn(microservice % "test->test")
  .enablePlugins(PlayScala)
  .settings(scalacOptions ~= (options => options.distinct))
  .settings(DefaultBuildSettings.itSettings())