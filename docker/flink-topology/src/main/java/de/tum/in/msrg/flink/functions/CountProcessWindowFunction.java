package de.tum.in.msrg.flink.functions;


import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class CountProcessWindowFunction extends ProcessWindowFunction<ClickUpdateEvent, PageStatistics, String, TimeWindow> {

    @Override
    public void process(String s, ProcessWindowFunction<ClickUpdateEvent, PageStatistics, String, TimeWindow>.Context context, Iterable<ClickUpdateEvent> elements, Collector<PageStatistics> out) throws Exception {
        Date winStart = new Date(context.window().getStart());
        Date winEnd = new Date(context.window().getEnd());
        List<Long> ids = new ArrayList<Long>();
        Integer updateCount = 1;
        Date lastUpdated = null;

        Iterator<ClickUpdateEvent> clickUpdateEventIterator = elements.iterator();
        ClickUpdateEvent firstEvent = clickUpdateEventIterator.next();

        lastUpdated = firstEvent.getUpdateTimestamp();
        ids.add(firstEvent.getId());

        while (clickUpdateEventIterator.hasNext()){
            ClickUpdateEvent event = clickUpdateEventIterator.next();
            ids.add(event.getId());
            if (event.getUpdateTimestamp().after(lastUpdated)){
                updateCount++;
            }
        }

        PageStatistics statistics = new PageStatistics(winStart, winEnd, s, ids, ids.size());
        statistics.setUpdateCount(updateCount);

        out.collect(statistics);

    }
}
