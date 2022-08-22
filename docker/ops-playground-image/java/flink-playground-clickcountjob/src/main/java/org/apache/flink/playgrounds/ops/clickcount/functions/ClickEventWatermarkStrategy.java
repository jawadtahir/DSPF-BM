package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.api.common.eventtime.*;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;

import java.time.Duration;

public class ClickEventWatermarkStrategy implements WatermarkStrategy<ClickEvent> {
    @Override
    public WatermarkGenerator<ClickEvent> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context) {
        BoundedOutOfOrdernessWatermarks<ClickEvent> wmGen = new BoundedOutOfOrdernessWatermarks<ClickEvent>(Duration.ofMillis(200));

        return new WatermarksWithIdleness<ClickEvent>(wmGen, Duration.ofMillis(5000));
    }

    @Override
    public TimestampAssigner<ClickEvent> createTimestampAssigner(TimestampAssignerSupplier.Context context) {
        return new TimestampAssigner<ClickEvent>() {
            @Override
            public long extractTimestamp(ClickEvent clickEvent, long l) {
                return clickEvent.getTimestamp().getTime();
            }
        };
    }
}
