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
public class ClickEventStatistics {

	//using java.util.Date for better readability
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date windowStart;
	//using java.util.Date for better readability
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date windowEnd;
	//using java.util.Date for better readability
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date firstMsgTS;
	private Set<String> pages;
	private List<Long> ids;
	private long count;

	public ClickEventStatistics() {
		this.pages = new HashSet<>();
		this.ids = new ArrayList<>();
	}

	public ClickEventStatistics(Date windowStart, Date windowEnd, Date firstMsgTS, Set<String> pages, List<Long> ids, long count) {
		this.windowStart = windowStart;
		this.windowEnd = windowEnd;
		this.firstMsgTS = firstMsgTS;
		this.pages = pages;
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

	public Date getFirstMsgTS() {
		return firstMsgTS;
	}

	public void setFirstMsgTS(Date firstMsgTS) {
		this.firstMsgTS = firstMsgTS;
	}

	public Set<String> getPages() {
		return pages;
	}

	public void setPages(Set<String> pages) {
		this.pages = Collections.synchronizedSet(pages) ;
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
		ClickEventStatistics that = (ClickEventStatistics) o;
		return count == that.count && windowStart.equals(that.windowStart) && windowEnd.equals(that.windowEnd) && Objects.equals(firstMsgTS, that.firstMsgTS) && Objects.equals(pages, that.pages) && Objects.equals(ids, that.ids);
	}

	@Override
	public int hashCode() {
		return Objects.hash(windowStart, windowEnd, firstMsgTS, pages, ids, count);
	}

	@Override
	public String toString() {
		return "ClickEventStatistics{" +
				"windowStart=" + windowStart +
				", windowEnd=" + windowEnd +
				", firstMsgTS=" + firstMsgTS +
				", pages=" + Arrays.deepToString(pages.toArray()) +
				", ids=" + Arrays.deepToString(ids.toArray()) +
				", count=" + count +
				'}';
	}
}
