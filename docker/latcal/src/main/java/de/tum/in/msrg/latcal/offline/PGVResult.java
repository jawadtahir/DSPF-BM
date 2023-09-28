package de.tum.in.msrg.latcal.offline;

public class PGVResult {

    private Long readInputEvents;
    private Long expectedOutputs;
    private Long readOutputs;
    private Long correctOutputs;
    private Long lowerIncorrectOutputs;
    private Long higherIncorrectOutputs;
    private Long readOutputInputs;
    private Long processedEvents;
    private Long unproceesedEvents;
    private Long duplicateEvents;
    private Long lateEvents;




    public  PGVResult (){
        this(0L,0L,0L,0L,0L,0L,0L,0L,0L,0L, 0L);
    }

    public PGVResult(Long readInputEvents, Long expectedOutputs, Long readOutputs, Long readOutputInputs, Long processedEvents, Long unproceesedEvents, Long duplicateEvents, Long lateEvents, Long correctOutputs, Long lowerIncorrectOutputs, Long higherIncorrectOutputs) {
        this.readInputEvents = readInputEvents;
        this.expectedOutputs = expectedOutputs;
        this.readOutputs = readOutputs;
        this.readOutputInputs = readOutputInputs;
        this.processedEvents = processedEvents;
        this.unproceesedEvents = unproceesedEvents;
        this.duplicateEvents = duplicateEvents;
        this.lateEvents = lateEvents;
        this.correctOutputs = correctOutputs;
        this.lowerIncorrectOutputs = lowerIncorrectOutputs;
        this.higherIncorrectOutputs = higherIncorrectOutputs;
    }

    public Long getReadInputEvents() {
        return readInputEvents;
    }

    public void setReadInputEvents(Long readInputEvents) {
        this.readInputEvents = readInputEvents;
    }

    public void incReadInputEvents() {
        readInputEvents++;
    }

    public Long getReadOutputInputs() {
        return readOutputInputs;
    }

    public void setReadOutputInputs(Long readOutputInputs) {
        this.readOutputInputs = readOutputInputs;
    }

    public void incReadOutputInputs() {
        readOutputInputs++;
    }

    public Long getProcessedEvents() {
        return processedEvents;
    }

    public void setProcessedEvents(Long processedEvents) {
        this.processedEvents = processedEvents;
    }

    public void incProcessedEvents() {
        processedEvents++;
    }

    public Long getUnproceesedEvents() {
        return unproceesedEvents;
    }

    public void setUnproceesedEvents(Long unproceesedEvents) {
        this.unproceesedEvents = unproceesedEvents;
    }

    public void incUnproceesedEvents() {
        unproceesedEvents++;
    }

    public Long getDuplicateEvents() {
        return duplicateEvents;
    }

    public void setDuplicateEvents(Long duplicateEvents) {
        this.duplicateEvents = duplicateEvents;
    }

    public void incDuplicateEvents() {
        duplicateEvents++;
    }

    public Long getLateEvents() {
        return lateEvents;
    }

    public void setLateEvents(Long lateEvents) {
        this.lateEvents = lateEvents;
    }

    public void incLateEvents() {
        lateEvents++;
    }

    public Long getExpectedOutputs() {
        return expectedOutputs;
    }

    public void setExpectedOutputs(Long expectedOutputs) {
        this.expectedOutputs = expectedOutputs;
    }

    public void incExpectedOutputs() {
        expectedOutputs++;
    }

    public Long getReadOutputs() {
        return readOutputs;
    }

    public void setReadOutputs(Long readOutputs) {
        this.readOutputs = readOutputs;
    }

    public void incReadOutputs() {
        readOutputs++;
    }

    public Long getCorrectOutputs() {
        return correctOutputs;
    }

    public void setCorrectOutputs(Long correctOutputs) {
        this.correctOutputs = correctOutputs;
    }

    public void incCorrectOutputs() {
        correctOutputs++;
    }

    public Long getLowerIncorrectOutputs() {
        return lowerIncorrectOutputs;
    }

    public void setLowerIncorrectOutputs(Long lowerIncorrectOutputs) {
        this.lowerIncorrectOutputs = lowerIncorrectOutputs;
    }

    public void incLowerIncorrectOutputs() {
        lowerIncorrectOutputs++;
    }

    public Long getHigherIncorrectOutputs() {
        return higherIncorrectOutputs;
    }

    public void setHigherIncorrectOutputs(Long higherIncorrectOutputs) {
        this.higherIncorrectOutputs = higherIncorrectOutputs;
    }

    public void incHigherIncorrectOutputs() {
        higherIncorrectOutputs++;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PGVResult{");
        sb.append("readInputEvents=").append(readInputEvents);
        sb.append(", expectedOutputs=").append(expectedOutputs);
        sb.append(", readOutputs=").append(readOutputs);
        sb.append(", correctOutputs=").append(correctOutputs);
        sb.append(", lowerIncorrectOutputs=").append(lowerIncorrectOutputs);
        sb.append(", higherIncorrectOutputs=").append(higherIncorrectOutputs);
        sb.append(", readOutputInputs=").append(readOutputInputs);
        sb.append(", processedEvents=").append(processedEvents);
        sb.append(", unproceesedEvents=").append(unproceesedEvents);
        sb.append(", duplicateEvents=").append(duplicateEvents);
        sb.append(", lateEvents=").append(lateEvents);
        sb.append('}');
        return sb.toString();
    }
}
