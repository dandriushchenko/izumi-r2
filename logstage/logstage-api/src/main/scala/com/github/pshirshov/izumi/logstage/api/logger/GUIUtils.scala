package com.github.pshirshov.izumi.logstage.api.logger

import com.github.pshirshov.izumi.logstage.model.Log

object GUIUtils {
  def logLevelColor(lvl: Log.Level): String = lvl match {
    case Log.Level.Trace => Console.MAGENTA
    case Log.Level.Debug => Console.BLUE
    case Log.Level.Info => Console.GREEN
    case Log.Level.Warn => Console.CYAN
    case Log.Level.Error => Console.YELLOW
    case Log.Level.Crit => Console.RED
  }
}