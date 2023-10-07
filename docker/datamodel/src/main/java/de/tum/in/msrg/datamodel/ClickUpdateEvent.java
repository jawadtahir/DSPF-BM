package de.tum.in.msrg.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

/**
 * A simple event recording a click on a {@link ClickUpdateEvent#page} at time {@link ClickUpdateEvent#timestamp}.
 *
 */
public class ClickUpdateEvent {

    private String page;
    private long clickId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date clickTimestamp;
    private long updateId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date updateTimestamp;


    public ClickUpdateEvent(ClickEvent clickEvent, UpdateEvent updateEvent){
        this.page = clickEvent.getPage();
        this.clickId = clickEvent.getId();
        this.clickTimestamp = clickEvent.getTimestamp();
        if (updateEvent == null) {
            this.updateId = 0L;
            this.updateTimestamp = Date.from(Instant.EPOCH);
        } else {
            this.updateId = updateEvent.getId();
            this.updateTimestamp = updateEvent.getTimestamp();
        }
    }

    public ClickUpdateEvent(){
        this("", 0L, new Date(Instant.EPOCH.toEpochMilli()), 0L, new Date(Instant.EPOCH.toEpochMilli()));
    }

    public ClickUpdateEvent(String page, long clickId, Date clickTimestamp, long updateId, Date updateTimestamp) {
        this.page = page;
        this.clickId = clickId;
        this.clickTimestamp = clickTimestamp;
        this.updateId = updateId;
        this.updateTimestamp = updateTimestamp;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public long getClickId() {
        return clickId;
    }

    public void setClickId(long clickId) {
        this.clickId = clickId;
    }

    public Date getClickTimestamp() {
        return clickTimestamp;
    }

    public void setClickTimestamp(Date clickTimestamp) {
        this.clickTimestamp = clickTimestamp;
    }

    public long getUpdateId() {
        return updateId;
    }

    public void setUpdateId(long updateId) {
        this.updateId = updateId;
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
        return clickId == that.clickId && updateId == that.updateId && page.equals(that.page) && clickTimestamp.equals(that.clickTimestamp) && updateTimestamp.equals(that.updateTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, clickId, clickTimestamp, updateId, updateTimestamp);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClickUpdateEvent{");
        sb.append("page='").append(page).append('\'');
        sb.append(", clickId=").append(clickId);
        sb.append(", clickTimestamp=").append(clickTimestamp);
        sb.append(", updateId=").append(updateId);
        sb.append(", updateTimestamp=").append(updateTimestamp);
        sb.append('}');
        return sb.toString();
    }
}

