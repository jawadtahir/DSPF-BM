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

package de.tum.in.msrg.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.*;

/**
 * A small wrapper class for windowed page counts.
 *
 */
public class PageStatistics {

	//using java.util.Date for better readability
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date windowStart;
	//using java.util.Date for better readability
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date windowEnd;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date lastUpdateTS;
	private long updateCount;
	private String page;
	private List<Long> ids;
	private long count;

	public PageStatistics() {
		this.ids =  Collections.synchronizedList(new ArrayList<>());
	}

	public PageStatistics(Date windowStart, Date windowEnd, String page, List<Long> ids, long count) {
		this.windowStart = windowStart;
		this.windowEnd = windowEnd;
		this.page = page;
		this.ids = ids;
		this.count = count;
	}

	public Date getWindowStart() {
		return windowStart;
	}

	public void setWindowStart(final Date windowStart) {
		this.windowStart = windowStart;
	}

	public Date getWindowEnd() {
		return windowEnd;
	}

	public void setWindowEnd(final Date windowEnd) {
		this.windowEnd = windowEnd;
	}

	public Date getLastUpdateTS() {
		return lastUpdateTS;
	}

	public void setLastUpdateTS(Date lastUpdateTS) {
		this.lastUpdateTS = lastUpdateTS;
	}

	public long getUpdateCount() {
		return updateCount;
	}

	public void setUpdateCount(long updateCount) {
		this.updateCount = updateCount;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public List<Long> getIds() {
		return ids;
	}

	public void setIds(List<Long> ids) {
		this.ids = ids;
	}

	public long getCount() {
		return count;
	}

	public void setCount(final long count) {
		this.count = count;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PageStatistics that = (PageStatistics) o;
		return updateCount == that.updateCount && count == that.count && windowStart.equals(that.windowStart) && windowEnd.equals(that.windowEnd) && lastUpdateTS.equals(that.lastUpdateTS) && Objects.equals(page, that.page) && Objects.equals(ids, that.ids);
	}

	@Override
	public int hashCode() {
		return Objects.hash(windowStart, windowEnd, lastUpdateTS, updateCount, page, ids, count);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("PageStatistics{");
		sb.append("windowStart=").append(windowStart);
		sb.append(", windowEnd=").append(windowEnd);
		sb.append(", lastUpdateTS=").append(lastUpdateTS);
		sb.append(", updateCount=").append(updateCount);
		sb.append(", pages=").append(page);
		sb.append(", ids=").append(Arrays.deepToString(ids.toArray()));
		sb.append(", count=").append(count);
		sb.append('}');
		return sb.toString();
	}
}
