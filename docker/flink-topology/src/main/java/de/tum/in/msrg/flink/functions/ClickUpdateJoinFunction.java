package de.tum.in.msrg.flink.functions;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.flink.api.common.functions.JoinFunction;

public class ClickUpdateJoinFunction implements JoinFunction<ClickEvent, UpdateEvent, ClickUpdateEvent> {
    @Override
    public ClickUpdateEvent join(ClickEvent first, UpdateEvent second) throws Exception {
        ClickUpdateEvent clickUpdateEvent = new ClickUpdateEvent(first, second);
        return clickUpdateEvent;
    }
}
