package com.github.pshirshov.izumi.idealingua.compiler

import java.io.File
import java.nio.file.{Path, Paths}

import scopt.OptionParser

case class LanguageOpts(id: String, withRuntime: Boolean, manifest: Option[File], extensions: List[String], overrides: Map[String, String])


case class IDLCArgs(
                     source: Path,
                     target: Path,
                     languages: List[LanguageOpts],
                     init: Option[Path],
                   )

object IDLCArgs {
  def default: IDLCArgs = IDLCArgs(
    Paths.get("source")
    , Paths.get("target")
    , List.empty
    , None
  )

  val parser: OptionParser[IDLCArgs] = new scopt.OptionParser[IDLCArgs]("idlc") {
    head("idlc")
    help("help")

    opt[File]('i', "init").optional().valueName("<dir>")
      .action((a, c) => c.copy(init = Some(a.toPath)))
      .text("init directory (must be empty or non-existing)")


    opt[File]('s', "source").optional().valueName("<dir>")
      .action((a, c) => c.copy(source = a.toPath))
      .text("source directory (default: `./source`)")

    opt[File]('t', "target").optional().valueName("<dir>")
      .action((a, c) => c.copy(target = a.toPath))
      .text("target directory (default: `./target`)")

    arg[String]("**language-id**")
      .text("{scala|typescript|go|csharp} (may repeat, like `scala -mf + typescript -mf + -nrt go`")
      .action {
        (a, c) =>
          c.copy(languages = c.languages :+ LanguageOpts(a, withRuntime = true, None, List.empty, Map.empty))
      }
      .optional()
      .unbounded()
      .children(
        opt[File]("manifest").abbr("m")
          .optional()
          .text("Language-specific compiler manifest. Use `@` for builtin stub, `+` for default path (./manifests/<language>.json)")
          .action {
            (a, c) =>
              c.copy(languages = c.languages.init :+ c.languages.last.copy(manifest = Some(a)))
          },
        opt[Unit]("no-runtime").abbr("nrt")
          .optional()
          .text("Don't include buitin runtime into compiler output")
          .action {
            (_, c) =>
              c.copy(languages = c.languages.init :+ c.languages.last.copy(withRuntime = false))
          },
        opt[String]('d', "define").valueName("name=value")
          .text("Define manifest override")
          .optional()
          .unbounded()
          .action {
            (a, c) =>
              val kv = a.split("=")
              c.copy(languages = c.languages.init :+ c.languages.last.copy(overrides = c.languages.last.overrides.updated(kv.head, kv(1))))
          },
        opt[String]("extensions").abbr("e").valueName("spec")
          .optional()
          .text("extensions spec, like -AnyvalExtension;-CirceDerivationTranslatorExtension or *")
          .action {
            (a, c) =>
              c.copy(languages = c.languages.init :+ c.languages.last.copy(extensions = a.split(',').toList))
          },
      )
  }

}
