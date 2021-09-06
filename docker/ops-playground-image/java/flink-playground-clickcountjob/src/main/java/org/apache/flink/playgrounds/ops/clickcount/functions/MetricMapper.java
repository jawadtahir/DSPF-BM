package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;

public class MetricMapper extends RichMapFunction<ClickEvent, ClickEvent> {

    private transient Meter throughputMeter;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.throughputMeter = getRuntimeContext().getMetricGroup().meter("customNumRecordsIn", new MeterView(1));
    }

    @Override
    public ClickEvent map(ClickEvent clickEvent) throws Exception {
        this.throughputMeter.markEvent();
        return clickEvent;
    }
}
