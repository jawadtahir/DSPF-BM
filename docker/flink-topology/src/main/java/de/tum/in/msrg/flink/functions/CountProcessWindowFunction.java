package de.tum.in.msrg.flink.functions;


import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.*;

public class CountProcessWindowFunction extends ProcessWindowFunction<ClickUpdateEvent, PageStatistics, String, TimeWindow> {

    @Override
    public void process(String s, ProcessWindowFunction<ClickUpdateEvent, PageStatistics, String, TimeWindow>.Context context, Iterable<ClickUpdateEvent> elements, Collector<PageStatistics> out) throws Exception {
        Date winStart = new Date(context.window().getStart());
        Date winEnd = new Date(context.window().getEnd());
        List<Long> clickIds = new ArrayList<Long>();
        List<Long> updateIds = new ArrayList<Long>();


        Iterator<ClickUpdateEvent> clickUpdateEventIterator = elements.iterator();
//        ClickUpdateEvent firstEvent = clickUpdateEventIterator.next();

//        lastUpdated = firstEvent.getUpdateTimestamp();
//        ids.add(firstEvent.getId());

        while (clickUpdateEventIterator.hasNext()){
            ClickUpdateEvent event = clickUpdateEventIterator.next();
            if (!clickIds.contains(event.getClickId())){
                clickIds.add(event.getClickId());
            }
            if (event.getUpdateId() != 0 && !updateIds.contains(event.getUpdateId())){
                updateIds.add(event.getUpdateId());
            }

        }

        PageStatistics statistics = new PageStatistics(s, winStart, winEnd, clickIds, updateIds);

        out.collect(statistics);

    }
}
