/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;


import java.util.Date;

/**
 * A simple {@link ProcessWindowFunction}, which wraps a count of {@link ClickEvent}s into an
 * instance of {@link ClickEventStatistics}.
 *
 **/
public class ClickEventStatisticsCollector
		extends ProcessWindowFunction<Tuple2<Long, Date>, ClickEventStatistics, String, TimeWindow> {

	@Override
	public void process(
			final String page,
			final Context context,
			final Iterable<Tuple2<Long, Date>> elements,
			final Collector<ClickEventStatistics> out) throws Exception {

		Tuple2<Long, Date> tuple = elements.iterator().next();
		Long count = tuple.f0;
		Date earliestDate = tuple.f1;

		out.collect(new ClickEventStatistics(new Date(context.window().getStart()), new Date(context.window().getEnd()), earliestDate, page, count));
	}
}
