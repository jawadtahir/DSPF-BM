package de.tum.in.msrg.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.Objects;

/**
 * A simple event recording an update on a {@link UpdateEvent#page} at time {@link UpdateEvent#timestamp}.
 *
 */
public class UpdateEvent {
    private long id;
    //using java.util.Date for better readability
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date timestamp;
    private String page;
    private String updatedBy;

    public UpdateEvent() {
    }

    public UpdateEvent(long id, Date timestamp, String page, String updatedBy) {
        this.id = id;
        this.timestamp = timestamp;
        this.page = page;
        this.updatedBy = updatedBy;
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

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateEvent that = (UpdateEvent) o;
        return id == that.id && timestamp.equals(that.timestamp) && page.equals(that.page) && Objects.equals(updatedBy, that.updatedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, page, updatedBy);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UpdateEvent{");
        sb.append("id=").append(id);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", page='").append(page).append('\'');
        sb.append(", updatedBy='").append(updatedBy).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
