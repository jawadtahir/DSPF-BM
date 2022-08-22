package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.kafka.streams.kstream.ValueJoiner;

public class ClickUpdateJoiner implements ValueJoiner<ClickEvent, UpdateEvent, ClickUpdateEvent> {

    @Override
    public ClickUpdateEvent apply(ClickEvent value1, UpdateEvent value2) {
        return new ClickUpdateEvent(value1, value2);
    }
}
