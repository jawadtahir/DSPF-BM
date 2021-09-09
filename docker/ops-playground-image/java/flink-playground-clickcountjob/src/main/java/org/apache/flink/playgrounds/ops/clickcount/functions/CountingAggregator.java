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

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;

import java.time.Instant;
import java.util.Date;

/**
 * An {@link AggregateFunction} which simply counts {@link ClickEvent}s.
 *
 */
public class CountingAggregator implements AggregateFunction<ClickEvent, Tuple2<Long, Date>, Tuple2<Long, Date>> {
	@Override
	public Tuple2<Long, Date> createAccumulator() {
		return new Tuple2<>(0L, new Date(Long.MAX_VALUE)) ;
	}

	@Override
	public Tuple2<Long, Date> add(final ClickEvent value, final Tuple2<Long, Date> accumulator) {
		accumulator.f0 = accumulator.f0 + 1L;
		accumulator.f1 = (value.getCreationTimestamp().before(accumulator.f1) )? value.getCreationTimestamp() : accumulator.f1;
		return accumulator;
	}

	@Override
	public Tuple2<Long, Date> getResult(final Tuple2<Long, Date> accumulator) {
		return accumulator;
	}

	@Override
	public Tuple2<Long, Date> merge(final Tuple2<Long, Date> a, final Tuple2<Long, Date> b) {
		return new Tuple2<Long, Date>(a.f0+b.f0,  a.f1.before(b.f1)?a.f1:b.f1);
	}
}
