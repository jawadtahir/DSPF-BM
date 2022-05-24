package de.tum.in.msrg.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;


import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * A simple event recording a click on a {@link ClickEvent#page} at time {@link ClickEvent#timestamp}.
 *
 */
public class ClickEvent{

    private long id;
    //using java.util.Date for better readability
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date timestamp;
    private String page;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date creationTimestamp;

    public ClickEvent() {
    }

    public ClickEvent(long id, Date timestamp, String page) {
        this.id = id;
        this.timestamp = timestamp;
        this.page = page;
        this.creationTimestamp = Date.from(Instant.now());
    }


    public ClickEvent(long id, Date timestamp, String page, Date creationTimestamp) {
        this.id = id;
        this.timestamp = timestamp;
        this.page = page;
        this.creationTimestamp = creationTimestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClickEvent that = (ClickEvent) o;
        return id == that.id && timestamp.equals(that.timestamp) && page.equals(that.page) && creationTimestamp.equals(that.creationTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, page, creationTimestamp);
    }

    @Override
    public String toString() {
        return "ClickEvent{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", page='" + page + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                '}';
    }
}

