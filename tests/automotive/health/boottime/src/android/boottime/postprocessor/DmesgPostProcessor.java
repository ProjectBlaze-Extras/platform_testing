/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.boottime.postprocessor;

import com.android.loganalysis.item.DmesgActionInfoItem;
import com.android.loganalysis.item.DmesgItem;
import com.android.loganalysis.item.DmesgServiceInfoItem;
import com.android.loganalysis.item.DmesgStageInfoItem;
import com.android.loganalysis.parser.DmesgParser;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.metrics.proto.MetricMeasurement;
import com.android.tradefed.metrics.proto.MetricMeasurement.Measurements;
import com.android.tradefed.metrics.proto.MetricMeasurement.Metric;
import com.android.tradefed.postprocessor.BasePostProcessor;
import com.android.tradefed.result.LogFile;
import com.android.tradefed.result.TestDescription;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** A Post Processor that processes text file containing dmesg logs into key-value pairs */
@OptionClass(alias = "dmesg-post-processor")
public class DmesgPostProcessor extends BasePostProcessor {
    private static final String INIT = "init_";
    private static final String START_TIME = "_START_TIME";
    private static final String DURATION = "_DURATION";
    private static final String END_TIME = "_END_TIME";
    private static final String ACTION = "action_";
    private static final String INIT_STAGE = "init_stage_";
    private static final String BOOT_COMPLETE_ACTION = "sys.boot_completed=1";
    private static final String DMESG_BOOT_COMPLETE_TIME =
            "dmesg_action_sys.boot_completed_first_timestamp";

    @Option(name = "file-regex", description = "Regex for identifying a dmesg file name.")
    private Set<String> mDmesgFileRegex = new HashSet<>();

    /** {@inheritDoc} */
    @Override
    public Map<String, Metric.Builder> processTestMetricsAndLogs(
            TestDescription testDescription,
            HashMap<String, Metric> testMetrics,
            Map<String, LogFile> testLogs) {
        LogUtil.CLog.v("Processing test logs for %s", testDescription.getTestName());
        return processDmesgLogs(filterFiles(testLogs));
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Metric.Builder> processRunMetricsAndLogs(
            HashMap<String, Metric> rawMetrics, Map<String, LogFile> runLogs) {
        return processDmesgLogs(filterFiles(runLogs));
    }

    /** {@inheritDoc} */
    /**
     * Returns {@link MetricMeasurement.DataType.RAW} for metrics reported by the post processor.
     * RAW is required in order for {@link
     * com.android.tradefed.postprocessor.MetricFilePostProcessor} to aggregate the values
     */
    @Override
    protected MetricMeasurement.DataType getMetricType() {
        // Return raw metrics in order for MetricFilePostProcessor to aggregate
        return MetricMeasurement.DataType.RAW;
    }

    /**
     * Process Dmesg testLog files reported by the test
     *
     * @param dmesgFiles List of dmesg files
     * @return Map with metric keys and stringified double values joined by comma
     */
    private Map<String, Metric.Builder> processDmesgLogs(List<File> dmesgFiles) {
        ArrayListMultimap<String, Double> metrics = ArrayListMultimap.create();
        for (File dmesgFile : dmesgFiles) {
            CLog.d("Parsing dmesg file %s", dmesgFile.getPath());
            DmesgParser dmesgLogParser = new DmesgParser();
            try (InputStreamReader ir = new InputStreamReader(new FileInputStream(dmesgFile));
                    BufferedReader input = new BufferedReader(ir)) {
                DmesgItem dmesgItem = dmesgLogParser.parseInfo(input);
                if (!dmesgItem.getServiceInfoItems().isEmpty()) {
                    metrics.putAll(
                            Multimaps.forMap(
                                    analyzeDmesgServiceInfo(
                                            dmesgItem.getServiceInfoItems().values())));
                }
                if (!dmesgItem.getStageInfoItems().isEmpty()) {
                    metrics.putAll(
                            Multimaps.forMap(analyzeDmesgStageInfo(dmesgItem.getStageInfoItems())));
                }
                if (!dmesgItem.getActionInfoItems().isEmpty()) {
                    metrics.putAll(
                            Multimaps.forMap(
                                    analyzeDmesgActionInfo(dmesgItem.getActionInfoItems())));
                }
            } catch (IOException ioe) {
                CLog.e("Failed to analyze the dmesg logs", ioe);
            }
        }
        return buildTfMetrics(metrics.asMap());
    }

    /**
     * Build TradeFed metrics from raw Double values.
     *
     * @param metrics contains a map of {@link Collection} each single value represents a metric for
     *     a particular boot iteration
     * @return Map with metric keys and stringified double values joined by comma
     */
    private Map<String, Metric.Builder> buildTfMetrics(Map<String, Collection<Double>> metrics) {
        Map<String, Metric.Builder> tfMetrics = new HashMap<>();

        CLog.v("Collected %d metrics", metrics.size());
        for (Map.Entry<String, Collection<Double>> entry : metrics.entrySet()) {
            String stringValue =
                    entry.getValue().stream()
                            .map(value -> value.toString())
                            .collect(Collectors.joining(","));
            Measurements.Builder measurement =
                    Measurements.newBuilder().setSingleString(stringValue);
            Metric.Builder metricBuilder = Metric.newBuilder().setMeasurements(measurement);
            tfMetrics.put(entry.getKey(), metricBuilder);
        }
        return tfMetrics;
    }

    /**
     * Analyze the services info parsed from the dmesg logs and construct the metrics as a part of
     * boot time data.
     *
     * @param serviceInfoItems contains the start time, end time and the duration of each service
     *     logged in the dmesg log file.
     * @return Map with dmesg metrics from info items
     */
    private Map<String, Double> analyzeDmesgServiceInfo(
            Collection<DmesgServiceInfoItem> serviceInfoItems) {
        Map<String, Double> metrics = new HashMap<>();
        for (DmesgServiceInfoItem infoItem : serviceInfoItems) {
            String key = null;
            if (infoItem.getStartTime() != null) {
                key = String.format("%s%s%s", INIT, infoItem.getServiceName(), START_TIME);
            } else if (infoItem.getServiceDuration() != -1L) {
                key = String.format("%s%s%s", INIT, infoItem.getServiceName(), DURATION);
            } else if (infoItem.getEndTime() != null) {
                key = String.format("%s%s%s", INIT, infoItem.getServiceName(), END_TIME);
            }
            if (key != null) {
                Double value = infoItem.getStartTime().doubleValue();
                metrics.put(key, value);
            }
        }
        return metrics;
    }

    /**
     * Analyze the boot stages info parsed from the dmesg logs and construct the metrics as a part
     * of boot time data.
     *
     * @param stageInfoItems contains the start time of each stage logged in the dmesg log file.
     * @return Map with dmesg metrics from info items
     */
    private Map<String, Double> analyzeDmesgStageInfo(
            Collection<DmesgStageInfoItem> stageInfoItems) {
        Map<String, Double> metrics = new HashMap<>();
        for (DmesgStageInfoItem stageInfoItem : stageInfoItems) {
            if (stageInfoItem.getStartTime() != null) {
                String key =
                        String.format(
                                "%s%s%s", INIT_STAGE, stageInfoItem.getStageName(), START_TIME);
                metrics.put(key, stageInfoItem.getStartTime().doubleValue());
            }
            if (stageInfoItem.getDuration() != null) {
                metrics.put(
                        stageInfoItem.getStageName(), stageInfoItem.getDuration().doubleValue());
            }
        }
        return metrics;
    }

    /**
     * Analyze each action info parsed from the dmesg logs and construct the metrics as a part of
     * boot time data.
     *
     * @param actionInfoItems contains the start time of processing of each action logged in the
     *     dmesg log file.
     * @return Map with dmesg metrics from info items
     */
    private Map<String, Double> analyzeDmesgActionInfo(
            Collection<DmesgActionInfoItem> actionInfoItems) {
        boolean isFirstBootCompletedAction = true;
        Map<String, Double> metrics = new HashMap<>();
        for (DmesgActionInfoItem actionInfoItem : actionInfoItems) {
            if (actionInfoItem.getStartTime() != null) {
                if (actionInfoItem.getActionName().startsWith(BOOT_COMPLETE_ACTION)
                        && isFirstBootCompletedAction) {
                    CLog.i(
                            "Using Action: %s_%s for first boot complete timestamp :%s",
                            actionInfoItem.getActionName(),
                            actionInfoItem.getSourceName(),
                            actionInfoItem.getStartTime().doubleValue());
                    // Record the first boot complete time stamp.
                    metrics.put(
                            DMESG_BOOT_COMPLETE_TIME, actionInfoItem.getStartTime().doubleValue());
                    isFirstBootCompletedAction = false;
                }
                String key =
                        String.format(
                                "%s%s_%s%s",
                                ACTION,
                                actionInfoItem.getActionName(),
                                actionInfoItem.getSourceName() != null
                                        ? actionInfoItem.getSourceName()
                                        : "",
                                START_TIME);
                metrics.put(key, actionInfoItem.getStartTime().doubleValue());
            }
        }
        return metrics;
    }

    private List<File> filterFiles(Map<String, LogFile> logs) {
        List<File> dmesgFiles = new ArrayList<>();
        for (Map.Entry<String, LogFile> entry : logs.entrySet()) {
            CLog.v("Filtering log file %s", entry.getKey());
            Optional<String> match =
                    mDmesgFileRegex.stream()
                            .filter(regex -> entry.getKey().matches(regex))
                            .findAny();
            if (match.isPresent()) {
                CLog.d(
                        "Found dmesg testLog file %s at %s",
                        entry.getKey(), entry.getValue().getPath());
                dmesgFiles.add(new File(entry.getValue().getPath()));
            }
        }
        return dmesgFiles;
    }
}
