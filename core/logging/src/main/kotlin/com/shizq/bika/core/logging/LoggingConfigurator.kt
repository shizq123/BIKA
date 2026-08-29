package com.shizq.bika.core.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration
import java.io.File
object LoggingConfigurator {
    private const val CHARSET = "UTF-8"
    private const val APP_NAME = "APP"
    private const val CONSOLE_APPENDER = "STDOUT"
    private const val FILE_APPENDER = "FILE"
    private const val LOG_FILE_NAME = "app.log"
    private const val LOG_FILE_PATTERN = "app-%d{yyyy-MM-dd}.log"
    private const val KTOR_CLIENT_LOGGER = "io.ktor.client.plugins"
    private const val CONSOLE_PATTERN =
        "%highlight{%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%t] %c{1}: %msg%n%throwable}{FATAL=red bold, ERROR=red, WARN=yellow, INFO=green, DEBUG=bright_blue, TRACE=bright_green}"
    private const val FILE_PATTERN =
        "%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%t] %c: %msg%n%throwable"
    fun configureLogging(logsFolder: File) {
        ensureDirectory(logsFolder)
        val builder = ConfigurationBuilderFactory.newConfigurationBuilder().apply {
            setStatusLevel(Level.WARN)
            setConfigurationName(APP_NAME)
            add(consoleAppender())
            add(fileAppender(logsFolder))
            add(rootLogger())
            add(ktorClientLogger())
        }
        val context = LogManager.getContext(false) as LoggerContext
        context.stop()
        context.start(builder.build())
        context.updateLoggers()
    }
    private fun ensureDirectory(folder: File) {
        if (!folder.exists() && !folder.mkdirs()) {
            throw IllegalStateException("Failed to create log directory: ${folder.absolutePath}")
        }
    }
    private fun ConfigurationBuilder<BuiltConfiguration>.consoleAppender(): AppenderComponentBuilder =
        newAppender(CONSOLE_APPENDER, "Console")
            .add(patternLayout(CONSOLE_PATTERN))
    private fun ConfigurationBuilder<BuiltConfiguration>.fileAppender(logsFolder: File): AppenderComponentBuilder =
        newAppender(FILE_APPENDER, "RollingFile")
            .addAttribute("fileName", File(logsFolder, LOG_FILE_NAME).absolutePath)
            .addAttribute("filePattern", File(logsFolder, LOG_FILE_PATTERN).absolutePath)
            .add(patternLayout(FILE_PATTERN))
            .addComponent(
                newComponent("Policies")
                    .addComponent(
                        newComponent("TimeBasedTriggeringPolicy")
                            .addAttribute("interval", 1)
                            .addAttribute("modulate", true),
                    ),
            )
            .addComponent(
                newComponent("DefaultRolloverStrategy")
                    .addAttribute("max", 7),
            )
    private fun ConfigurationBuilder<BuiltConfiguration>.rootLogger() =
        newRootLogger(Level.ALL)
            .add(newAppenderRef(CONSOLE_APPENDER))
            .add(newAppenderRef(FILE_APPENDER))
    private fun ConfigurationBuilder<BuiltConfiguration>.ktorClientLogger() =
        newLogger(KTOR_CLIENT_LOGGER, Level.DEBUG)
            .addAttribute("additivity", false)
            .add(newAppenderRef(CONSOLE_APPENDER))
            .add(newAppenderRef(FILE_APPENDER))
    private fun ConfigurationBuilder<BuiltConfiguration>.patternLayout(
        pattern: String,
    ): LayoutComponentBuilder =
        newLayout("PatternLayout")
            .addAttribute("pattern", pattern)
            .addAttribute("charset", CHARSET)
}