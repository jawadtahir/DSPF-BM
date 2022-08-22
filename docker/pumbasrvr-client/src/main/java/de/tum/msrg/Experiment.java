package de.tum.msrg;

import java.util.ArrayList;
import java.util.List;

public class Experiment {

    private List<Task> tasks;

    public Experiment(){
        this.tasks = new ArrayList<>();
    }

    public Experiment(List<Task> tasks) {
        this.tasks = tasks;
    }

    protected List<Task> addTask(Task task){
        this.tasks.add(task);
        return this.tasks;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Experiment{");
        sb.append("tasks=").append(tasks);
        sb.append('}');
        return sb.toString();
    }
}
