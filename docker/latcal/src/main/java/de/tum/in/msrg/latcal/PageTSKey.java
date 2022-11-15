package de.tum.in.msrg.latcal;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class PageTSKey {
    private String page;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    private Date TS;

    public PageTSKey(String page, Date TS) {
        this.page = page;
//        this.TS = DateUtils.ceiling(DateUtils.truncate(TS, Calendar.MINUTE), Calendar.MINUTE);
        this.TS = DateUtils.truncate(TS, Calendar.MINUTE);
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public Date getTS() {
        return TS;
    }

    public void setTS(Date TS) {
        this.TS = TS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageTSKey pageTSKey = (PageTSKey) o;
        return page.equals(pageTSKey.page) && TS.equals(pageTSKey.TS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, TS);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PageTSKey{");
        sb.append("page='").append(page).append('\'');
        sb.append(", TS=").append(TS);
        sb.append('}');
        return sb.toString();
    }
}


