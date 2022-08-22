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
import org.apache.flink.api.common.functions.RichAggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;

import java.time.Instant;
import java.util.Date;

/**
 * An {@link AggregateFunction} which simply counts {@link ClickEvent}s.
 *
 */
public class CountingAggregator implements AggregateFunction<ClickEvent, Accum, ClickEventStatistics> {
	@Override
	public Accum createAccumulator() {
		return new Accum(0, Date.from(Instant.now()), Date.from(Instant.EPOCH)) ;
	}

	@Override
	public Accum add(ClickEvent clickEvent, Accum accum) {
		accum.setCount(accum.getCount()+1);
		accum.setFirstDate(clickEvent.getTimestamp().before(accum.getFirstDate())? clickEvent.getTimestamp(): accum.getFirstDate());
		accum.setLastDate(clickEvent.getTimestamp().after(accum.getLastDate())? clickEvent.getTimestamp(): accum.getLastDate());
		return accum;
	}

	@Override
	public ClickEventStatistics getResult(Accum accum) {
		ClickEventStatistics stats = new ClickEventStatistics(accum.getFirstDate(), accum.getLastDate(),accum.getFirstDate(), "test", accum.getCount());
		return stats;
	}

	@Override
	public Accum merge(Accum accum, Accum acc1) {
		Accum merged = new Accum(
				accum.getCount()+acc1.getCount(),
				accum.getFirstDate().before(acc1.getFirstDate()) ? accum.getFirstDate() : acc1.getFirstDate(),
				accum.getLastDate().after(acc1.getLastDate()) ? accum.getLastDate() : acc1.getLastDate());
		return merged;
	}

}

class Accum{
	private long count;
	private Date firstDate;
	private Date lastDate;

	public Accum(long count, Date firstDate, Date lastDate) {
		this.count = count;
		this.firstDate = firstDate;
		this.lastDate = lastDate;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public Date getFirstDate() {
		return firstDate;
	}

	public void setFirstDate(Date firstDate) {
		this.firstDate = firstDate;
	}

	public Date getLastDate() {
		return lastDate;
	}

	public void setLastDate(Date lastDate) {
		this.lastDate = lastDate;
	}

	@Override
	public String toString() {
		return "Accum{" +
				"count=" + count +
				", firstDate=" + firstDate +
				", lastDate=" + lastDate +
				'}';
	}
}
