package de.tum.in.msrg.flink.functions;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.flink.api.common.functions.RichJoinFunction;
import org.apache.flink.metrics.Counter;


public class ClickUpdateJoinFunction extends RichJoinFunction<ClickEvent, UpdateEvent, ClickUpdateEvent> {

    private Counter invokeCounter = null;


    @Override
    public ClickUpdateEvent join(ClickEvent first, UpdateEvent second) throws Exception {
        if (invokeCounter == null){
            invokeCounter = getRuntimeContext().getMetricGroup().counter("joinInvocation");
        }
        ClickUpdateEvent clickUpdateEvent = new ClickUpdateEvent(first, second);
        invokeCounter.inc();
        return clickUpdateEvent;
    }
}
