package com.shtel.paas.gitlab.entity;

import lombok.Data;

@Data
public class CountResultInfo {
    private String username;
    private String isMark;
    private String userEmail;
    private String additionsAll;
    private String deletionsAll;
    private String totalAll;
    private String projectCount;
    private String details;
}
