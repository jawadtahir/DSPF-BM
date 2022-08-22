package de.tum.in.msrg.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * A simple event recording a click on a {@link ClickUpdateEvent#page} at time {@link ClickUpdateEvent#timestamp}.
 *
 */
public class ClickUpdateEvent {

    private long id;
    //using java.util.Date for better readability
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date timestamp;
    private String page;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date creationTimestamp;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date updateTimestamp;

    public ClickUpdateEvent() {
    }

    public ClickUpdateEvent(ClickEvent clickEvent, UpdateEvent updateEvent){
        this.id = clickEvent.getId();
        this.timestamp = clickEvent.getTimestamp();
        this.page = clickEvent.getPage();
        this.creationTimestamp = clickEvent.getCreationTimestamp();
        if (updateEvent == null) {
            this.updateTimestamp = Date.from(Instant.EPOCH);
        } else {
        this.updateTimestamp = updateEvent.getTimestamp();}
    }


    public ClickUpdateEvent(long id, Date timestamp, String page) {
        this.id = id;
        this.timestamp = timestamp;
        this.page = page;
        this.creationTimestamp = Date.from(Instant.now());
    }


    public ClickUpdateEvent(long id, Date timestamp, String page, Date creationTimestamp) {
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

    public Date getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(Date updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClickUpdateEvent that = (ClickUpdateEvent) o;
        return id == that.id && timestamp.equals(that.timestamp) && page.equals(that.page) && Objects.equals(creationTimestamp, that.creationTimestamp) && updateTimestamp.equals(that.updateTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, page, creationTimestamp, updateTimestamp);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClickUpdateEvent{");
        sb.append("id=").append(id);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", page='").append(page).append('\'');
        sb.append(", creationTimestamp=").append(creationTimestamp);
        sb.append(", updateTimestamp=").append(updateTimestamp);
        sb.append('}');
        return sb.toString();
    }
}

