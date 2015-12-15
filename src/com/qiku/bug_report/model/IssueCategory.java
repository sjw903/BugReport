package com.qiku.bug_report.model;

public class IssueCategory {
    private String title = "";
    private String questionsRef;

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getQuestionsRef() {
        return questionsRef;
    }
    public void setQuestionsRef(String questionsRef) {
        this.questionsRef = questionsRef;
    }
    public String toString(){
        return title == null ? "" : title;
    }
    public int hashCode(){
        return toString().toLowerCase().hashCode();
    }
    public boolean equals(Object  obj){
        if(obj == null)
            return false;
        if(obj instanceof IssueCategory)
            return hashCode() == obj.hashCode();
        else
            return false;
    }
}
