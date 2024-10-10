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

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

/**
 * A small wrapper class for windowed page counts.
 *
 */
public class PageStatistics implements Serializable {

	private String page;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date windowStart;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date windowEnd;

	private List<Long> clickIds;
	private List<Long> updateIds;

	public PageStatistics() {
		this(
				"",
				new Date(Instant.EPOCH.toEpochMilli()),
				new Date(Instant.EPOCH.toEpochMilli()),
				Collections.synchronizedList(new ArrayList<Long>()),
				Collections.synchronizedList(new ArrayList<Long>()));
	}

	public PageStatistics(String page, Date windowStart, Date windowEnd, List<Long> clickIds, List<Long> updateIds) {
		this.page = page;
		this.windowStart = windowStart;
		this.windowEnd = windowEnd;
		this.clickIds = clickIds;
		this.updateIds = updateIds;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public Date getWindowStart() {
		return windowStart;
	}

	public void setWindowStart(Date windowStart) {
		this.windowStart = windowStart;
	}

	public Date getWindowEnd() {
		return windowEnd;
	}

	public void setWindowEnd(Date windowEnd) {
		this.windowEnd = windowEnd;
	}

	public List<Long> getClickIds() {
		return clickIds;
	}

	public void setClickIds(List<Long> clickIds) {
		this.clickIds = clickIds;
	}

	public List<Long> getUpdateIds() {
		return updateIds;
	}

	public void setUpdateIds(List<Long> updateIds) {
		this.updateIds = updateIds;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PageStatistics that = (PageStatistics) o;
		return page.equals(that.page) && windowStart.equals(that.windowStart) && windowEnd.equals(that.windowEnd) && clickIds.equals(that.clickIds) && updateIds.equals(that.updateIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(page, windowStart, windowEnd, clickIds, updateIds);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("PageStatistics{");
		sb.append("page='").append(page).append('\'');
		sb.append(", windowStart=").append(windowStart);
		sb.append(", windowEnd=").append(windowEnd);
		sb.append(", clickIds=").append(Arrays.deepToString(clickIds.toArray()));
		sb.append(", updateIds=").append(Arrays.deepToString(updateIds.toArray()));
		sb.append('}');
		return sb.toString();
	}

}
