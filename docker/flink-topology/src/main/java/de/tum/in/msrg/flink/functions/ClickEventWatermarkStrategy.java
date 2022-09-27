package de.tum.in.msrg.flink.functions;

import de.tum.in.msrg.datamodel.ClickEvent;
import org.apache.flink.api.common.eventtime.*;

import java.time.Duration;

public class ClickEventWatermarkStrategy implements WatermarkStrategy<ClickEvent> {
    @Override
    public WatermarkGenerator<ClickEvent> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context) {
        BoundedOutOfOrdernessWatermarks<ClickEvent> wmGen = new BoundedOutOfOrdernessWatermarks<ClickEvent>(Duration.ofMillis(0));

        return new WatermarksWithIdleness<ClickEvent>(wmGen, Duration.ofMinutes(1));
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
