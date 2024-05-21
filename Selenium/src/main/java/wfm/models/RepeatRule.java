package wfm.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import utils.serialization.LocalDateSerializer;
import wfm.components.schedule.Periodicity;

import java.time.LocalDate;

public class RepeatRule {
    private boolean custom;
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;
    private String name;
    private String periodicity;

    public RepeatRule() {
    }

    public RepeatRule(Periodicity p) {
        this.periodicity = p.getRepeatTypeInApi();
        this.name = p.getRepeatType();
    }

    public boolean isCustom() {
        return custom;
    }

    public String getName() {
        return name;
    }

    public String getPeriodicity() {
        return periodicity;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public RepeatRule setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public RepeatRule setCustom(boolean custom) {
        this.custom = custom;
        return this;
    }

}
