package de.tum.in.msrg.kafka.processor;

import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.kafka.streams.kstream.ValueJoiner;

import java.util.Calendar;
import java.util.Date;

public class ClickUpdateJoiner implements ValueJoiner<ClickEvent, UpdateEvent, ClickUpdateEvent> {

    @Override
    public ClickUpdateEvent apply(ClickEvent value1, UpdateEvent value2) {
        if (value1 != null && value2 != null){
            Date clickEventTime = DateUtils.truncate(value1.getTimestamp(), Calendar.MINUTE);
            Date updateEventTime = DateUtils.truncate(value2.getTimestamp(), Calendar.MINUTE);

            if (!clickEventTime.equals(updateEventTime)){
                return null;
            }
        }
        return new ClickUpdateEvent(value1, value2);

    }
}
