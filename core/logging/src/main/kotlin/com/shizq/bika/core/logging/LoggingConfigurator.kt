package com.shizq.bika.core.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration
import java.io.File

/**
 * 日志配置工具类
 *
 * 功能：
 * - 控制台输出：DEBUG 及以上级别
 * - 文件输出：INFO 及以上级别，按天滚动，保留 7 天
 * - 支持配置文件覆盖
 * - 自动创建日志目录
 */
object LoggingConfigurator {

    // 常量定义
    private const val CONSOLE_APPENDER = "STDOUT"
    private const val FILE_APPENDER = "FILE"
    private const val CHARSET = "UTF-8"
    private const val MAX_ROLLOVER_DAYS = "7"
    private const val MAX_FILE_SIZE = "10MB"

    // 日志格式
    private const val CONSOLE_PATTERN =
        "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %c{1.} - %m%n%throwable"
    private const val FILE_PATTERN =
        "%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%thread] %c{2.} - %m%n%throwable"

    // 日志级别配置（可通过系统属性覆盖）
    private val ROOT_LEVEL = Level.toLevel(
        System.getProperty("logging.root.level", "INFO")
    )
    private val CONSOLE_LEVEL = Level.toLevel(
        System.getProperty("logging.console.level", "DEBUG")
    )
    private val FILE_LEVEL = Level.toLevel(
        System.getProperty("logging.file.level", "INFO")
    )

    // 需要特殊处理的包
    private val PACKAGE_LEVELS = mapOf(
        "io.ktor.client.plugins" to Level.WARN,
        "io.netty" to Level.WARN,
        "org.apache.http" to Level.WARN
    )

    /**
     * 配置日志系统
     *
     * @param logsFolder 日志文件夹路径
     * @throws IllegalStateException 如果无法创建日志目录
     */
    fun configureLogging(logsFolder: File) {
        // 1. 确保日志目录存在
        createLogsDirectory(logsFolder)

        // 2. 构建配置
        val builder: ConfigurationBuilder<BuiltConfiguration> =
            ConfigurationBuilderFactory.newConfigurationBuilder()

        // 3. 添加 Appenders
        addConsoleAppender(builder)
        addFileAppender(builder, logsFolder)

        // 4. 配置 Loggers
        configureLoggers(builder)

        // 5. 初始化配置
        Configurator.initialize(builder.build())
    }

    /**
     * 创建日志目录
     */
    private fun createLogsDirectory(logsFolder: File) {
        if (!logsFolder.exists() && !logsFolder.mkdirs()) {
            throw IllegalStateException("无法创建日志目录: ${logsFolder.absolutePath}")
        }

        if (!logsFolder.isDirectory) {
            throw IllegalStateException("日志路径不是目录: ${logsFolder.absolutePath}")
        }

        if (!logsFolder.canWrite()) {
            throw IllegalStateException("日志目录不可写: ${logsFolder.absolutePath}")
        }
    }

    /**
     * 添加控制台 Appender
     */
    private fun addConsoleAppender(
        builder: ConfigurationBuilder<BuiltConfiguration>
    ) {
        val consoleLayout = builder.newLayout("PatternLayout")
            .addAttribute("pattern", CONSOLE_PATTERN)
            .addAttribute("charset", CHARSET)

        val consoleAppender = builder.newAppender(CONSOLE_APPENDER, "Console")
            .add(consoleLayout)
            .addComponent(
                builder.newFilter(
                    "ThresholdFilter",
                    Filter.Result.ACCEPT,
                    Filter.Result.DENY
                ).addAttribute("level", CONSOLE_LEVEL)
            )

        builder.add(consoleAppender)
    }

    /**
     * 添加文件 Appender
     */
    private fun addFileAppender(
        builder: ConfigurationBuilder<BuiltConfiguration>,
        logsFolder: File
    ) {
        val fileLayout = builder.newLayout("PatternLayout")
            .addAttribute("pattern", FILE_PATTERN)
            .addAttribute("charset", CHARSET)

        val fileAppender = builder.newAppender(FILE_APPENDER, "RollingFile")
            .addAttribute("fileName", "${logsFolder.absolutePath}/app.log")
            .addAttribute("filePattern",
                "${logsFolder.absolutePath}/app-%d{yyyy-MM-dd}-%i.log.gz")
            .add(fileLayout)
            .addComponent(
                builder.newFilter(
                    "ThresholdFilter",
                    Filter.Result.ACCEPT,
                    Filter.Result.DENY
                ).addAttribute("level", FILE_LEVEL)
            )

        // 滚动策略：按时间和文件大小
        fileAppender.addComponent(
            builder.newComponent("Policies")
                // 按时间滚动
                .addComponent(
                    builder.newComponent("TimeBasedTriggeringPolicy")
                        .addAttribute("interval", "1")
                        .addAttribute("modulate", true)
                )
                // 按文件大小滚动
                .addComponent(
                    builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", MAX_FILE_SIZE)
                )
        )

        // 滚动策略：保留最近 7 天的日志
        fileAppender.addComponent(
            builder.newComponent("DefaultRolloverStrategy")
                .addAttribute("max", MAX_ROLLOVER_DAYS)
                .addAttribute("compressionLevel", "9")
        )

        builder.add(fileAppender)
    }

    /**
     * 配置 Loggers
     */
    private fun configureLoggers(
        builder: ConfigurationBuilder<BuiltConfiguration>
    ) {
        // 根 Logger
        val rootLogger = builder.newRootLogger(ROOT_LEVEL)
            .add(builder.newAppenderRef(CONSOLE_APPENDER))
            .add(builder.newAppenderRef(FILE_APPENDER))
        builder.add(rootLogger)

        // 特殊包的 Logger 配置
        PACKAGE_LEVELS.forEach { (packageName, level) ->
            val packageLogger = builder.newLogger(packageName, level)
                .add(builder.newAppenderRef(CONSOLE_APPENDER))
                .add(builder.newAppenderRef(FILE_APPENDER))
                .addAttribute("additivity", false)
            builder.add(packageLogger)
        }
    }
}

/**
 * 便捷的扩展函数，用于在应用启动时快速配置日志
 */
fun File.configureLogging() {
    LoggingConfigurator.configureLogging(this)
}