import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / scalaVersion := "3.8.3"

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "tandu",
    idePackagePrefix := Some("tandu"),
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("tandu")))
    },
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar"  % "17.2.0",
      "com.raquo" %%% "waypoint" % "9.0.0",
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    )
  )
