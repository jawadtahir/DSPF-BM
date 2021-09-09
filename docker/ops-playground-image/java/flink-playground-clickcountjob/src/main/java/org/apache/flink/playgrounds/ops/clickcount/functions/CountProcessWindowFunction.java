package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.Date;

public class CountProcessWindowFunction extends ProcessWindowFunction<ClickEvent, ClickEventStatistics, String, TimeWindow> {

    @Override
    public void process(String s, ProcessWindowFunction<ClickEvent, ClickEventStatistics, String, TimeWindow>.Context context, Iterable<ClickEvent> elements, Collector<ClickEventStatistics> out) throws Exception {
        ClickEvent firstEvent = elements.iterator().next();
        Date firstMsgTS = firstEvent.getCreationTimestamp();
        Long windowCounter = 0L;
        for (ClickEvent event : elements){
            windowCounter = windowCounter + 1;
        }

        ClickEventStatistics stats = new ClickEventStatistics(new Date(context.window().getStart()), new Date(context.window().getEnd()), firstMsgTS, s, windowCounter);

        out.collect(stats);



    }
}
