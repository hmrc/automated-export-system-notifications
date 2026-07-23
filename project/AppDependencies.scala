import sbt.*

object AppDependencies {
  private val bootstrapVersion = "10.8.0"
  private val playVersion      = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-backend-$playVersion" % bootstrapVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion" % bootstrapVersion % Test,
    "org.scalatest"          %% "scalatest"                    % "3.2.20"         % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"           % "7.0.2"          % Test,
    "org.scalacheck"         %% "scalacheck"                   % "1.19.0"         % Test,
    "org.scalatestplus"      %% "scalacheck-1-19"              % "3.2.20.0"       % Test
  )
}
