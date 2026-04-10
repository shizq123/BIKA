package com.shizq.bika

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric

/**
 * Custom Metrics to measure baseline profile effectiveness.
 */
class BaselineProfileMetrics {
    companion object {
        /**
         * A [TraceSectionMetric] that tracks the time spent in JIT compilation.
         *
         * This number should go down when a baseline profile is applied properly.
         */
        @OptIn(ExperimentalMetricApi::class)
        val jitCompilationMetric = TraceSectionMetric("JIT Compiling %", label = "JIT compilation")

        /**
         * A [TraceSectionMetric] that tracks the time spent in class initialization.
         *
         * This number should go down when a baseline profile is applied properly.
         */
        @OptIn(ExperimentalMetricApi::class)
        val classInitMetric = TraceSectionMetric("L%/%;", label = "ClassInit")

        /**
         * Metrics relevant to startup and baseline profile effectiveness measurement.
         */
        @OptIn(ExperimentalMetricApi::class)
        val allMetrics = listOf(StartupTimingMetric(), jitCompilationMetric, classInitMetric)
    }
}