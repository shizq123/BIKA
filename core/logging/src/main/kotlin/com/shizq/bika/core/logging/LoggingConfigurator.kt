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
    private const val STDOUT = "STDOUT"
    private const val FILE = "FILE"
    private const val LOG_FILE_NAME = "app.log"
    private const val LOG_FILE_PATTERN = "app-%d{yyyy-MM-dd}.log"

    fun configureLogging(logsFolder: File) {
        if (!logsFolder.exists()) {
            logsFolder.mkdirs()
        }

        val builder: ConfigurationBuilder<BuiltConfiguration> =
            ConfigurationBuilderFactory.newConfigurationBuilder()

        builder.setStatusLevel(Level.WARN)
        builder.setConfigurationName(APP_NAME)

        // Console appender
        val consoleLayout: LayoutComponentBuilder = builder.newLayout("PatternLayout")
            .addAttribute(
                "pattern",
                "%highlight{%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%t] %c{1}: %msg%n%throwable}{FATAL=red bold, ERROR=red, WARN=yellow, INFO=green, DEBUG=bright_blue, TRACE=bright_green}",
            )
            .addAttribute("charset", CHARSET)

        val consoleAppender: AppenderComponentBuilder =
            builder.newAppender(STDOUT, "Console")
                .add(consoleLayout)

        builder.add(consoleAppender)

        // File appender
        val fileLayout: LayoutComponentBuilder = builder.newLayout("PatternLayout")
            .addAttribute(
                "pattern",
                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%t] %c: %msg%n%throwable",
            )
            .addAttribute("charset", CHARSET)

        val fileAppender: AppenderComponentBuilder =
            builder.newAppender(FILE, "RollingFile")
                .addAttribute("fileName", "${logsFolder.absolutePath}/$LOG_FILE_NAME")
                .addAttribute("filePattern", "${logsFolder.absolutePath}/$LOG_FILE_PATTERN")
                .add(fileLayout)
                .addComponent(
                    builder.newComponent("Policies")
                        .addComponent(
                            builder.newComponent("TimeBasedTriggeringPolicy")
                                .addAttribute("interval", 1)
                                .addAttribute("modulate", true),
                        ),
                )
                .addComponent(
                    builder.newComponent("DefaultRolloverStrategy")
                        .addAttribute("max", 7),
                )

        builder.add(fileAppender)

        // Root logger
        builder.add(
            builder.newRootLogger(Level.ALL)
                .add(builder.newAppenderRef(STDOUT))
                .add(builder.newAppenderRef(FILE)),
        )

        // Special logger for Ktor client
        builder.add(
            builder.newLogger("io.ktor.client.plugins", Level.DEBUG)
                .addAttribute("additivity", false)
                .add(builder.newAppenderRef(STDOUT))
                .add(builder.newAppenderRef(FILE)),
        )

        val ctx = LogManager.getContext(false) as LoggerContext
        ctx.start(builder.build())
        ctx.updateLoggers()
    }
}