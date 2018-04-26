package com.github.pshirshov.izumi.logstage.api

import com.github.pshirshov.izumi.fundamentals.platform.build.ExposedTestScope
import com.github.pshirshov.izumi.logstage.api.logger.RenderingOptions
import com.github.pshirshov.izumi.logstage.api.rendering.StringRenderingPolicy
import com.github.pshirshov.izumi.logstage.core.{ConfigurableLogRouter, LogConfigServiceStaticImpl}
import com.github.pshirshov.izumi.logstage.model.Log
import com.github.pshirshov.izumi.logstage.model.config.LoggerConfig
import com.github.pshirshov.izumi.logstage.model.logger.LogSink
import com.github.pshirshov.izumi.logstage.sink.console.ConsoleSink
import org.scalatest.WordSpec

import scala.util.Random

class ExampleService(logger: IzLogger) {
  def start(): Unit = {
    val loggerWithContext = logger("userId" -> "xxx")
    val loggerWithSubcontext = loggerWithContext("custom" -> "value")

    val arg = "this is an argument"

    loggerWithContext.trace(s"This would be automatically extended")
    logger.debug(s"Service started. argument: $arg, Random value: ${Random.self.nextInt()}")
    loggerWithSubcontext.info("Just a string")
    logger.warn("Just an integer: " + 1)
    val arg1 = 5
    logger.crit(s"This is an expression: ${2 + 2 == 4} and this is an other one: ${5 * arg1 == 25}")
    val t = new RuntimeException("Oy vey!")
    logger.crit(s"A failure happened: $t")
  }
}


@ExposedTestScope
class LoggingMacroTest extends WordSpec {

  import LoggingMacroTest._

  "Log macro" should {
    "support console sink" in {
      new ExampleService(setupConsoleLogger()).start()
    }

    "support console sink with json output policy" in {
      new ExampleService(setupJsonLogger()).start()
    }
  }
}

object LoggingMacroTest {

  val coloringPolicy = new StringRenderingPolicy(RenderingOptions())
  val simplePolicy = new StringRenderingPolicy(RenderingOptions(withExceptions = false, withColors = false))
  val jsonPolicy = new JsonRenderingPolicy()
  val consoleSinkText = new ConsoleSink(coloringPolicy)
  val consoleSinkJson = new ConsoleSink(jsonPolicy)

  def setupConsoleLogger(): IzLogger = {
    configureLogger(Seq(consoleSinkText))
  }

  def setupJsonLogger(): IzLogger = {
    configureLogger(Seq(consoleSinkJson))
  }

  def configureLogger(sinks: Seq[LogSink]): IzLogger = {
    val router: ConfigurableLogRouter = mkRouter(sinks)
    IzLogger(router)
  }

  def mkRouter(sinks: Seq[LogSink]): ConfigurableLogRouter = {
    val configService = new LogConfigServiceStaticImpl(Map.empty, LoggerConfig(Log.Level.Trace, sinks))
    val router = new ConfigurableLogRouter(configService)
    router
  }
}
