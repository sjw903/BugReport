package com.qiku.bug_report.conf.bean;

import java.util.ArrayList;
import java.util.List;

import com.qiku.bug_report.model.IssueCategory;

public class IssueCategoryConf extends ConfigEntry{
    private List<IssueCategory> categories;

    public IssueCategoryConf() {
        categories = new ArrayList<IssueCategory>();
    }

    public List<IssueCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<IssueCategory> categories) {
        this.categories = categories;
    }
}
