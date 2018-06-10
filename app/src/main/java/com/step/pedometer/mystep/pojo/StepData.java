package com.step.pedometer.mystep.pojo;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;


@Table("step")   //Marked as a table name using a third-party framework
public class StepData{
    //@*** Added features in the jar package to facilitate the management of the database
    //Specify auto-increment, each object requires a primary key
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;

    @Column("today")
    private String today;
    @Column("step")
    private String step;
    @Column("previousStep")
    private String previousStep;

    public void setId(int id) {
        this.id = id;
    }

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public int getId(){
        return id;
    }

    public String getPreviousStep() {
        return previousStep;
    }

    public void setPreviousStep(String previousStep) {
        this.previousStep = previousStep;
    }
}
