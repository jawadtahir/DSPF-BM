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

package org.apache.flink.playgrounds.ops.clickcount.records;



import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * A simple event recording a click on a {@link ClickEvent#page} at time {@link ClickEvent#timestamp}.
 *
 */
public class ClickEvent{

	//using java.util.Date for better readability
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date timestamp;
	private String page;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private Date creationTimestamp;

	public ClickEvent() {
	}

	public ClickEvent(final Date timestamp, final String page) {
		this.timestamp = timestamp;
		this.page = page;
		this.creationTimestamp = Date.from(Instant.now());
	}

	public ClickEvent(final Date timestamp, final String page, final Date creationTimestamp) {
		this.timestamp = timestamp;
		this.page = page;
		this.creationTimestamp = creationTimestamp;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getPage() {
		return page;
	}

	public void setPage(final String page) {
		this.page = page;
	}

	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setCreationTimestamp(Date creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ClickEvent that = (ClickEvent) o;
		return Objects.equals(timestamp, that.timestamp) && Objects.equals(page, that.page) && Objects.equals(creationTimestamp, that.creationTimestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, page, creationTimestamp);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ClickEvent{");
		sb.append("timestamp=").append(timestamp);
		sb.append(", page='").append(page).append('\'');
		sb.append(", creationTime=").append(creationTimestamp);
		sb.append('}');
		return sb.toString();
	}
}
