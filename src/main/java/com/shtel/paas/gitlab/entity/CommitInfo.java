package com.shtel.paas.gitlab.entity;

import lombok.Data;

@Data
public class CommitInfo {

    private String id;
    private String created_at;
    private String title;
    private String message;
    private String author_name;
    private String author_email;
    private String authored_date;
    private String committer_name;
    private String committer_email;
    private String committed_date;
    private Stats stats;
    private Integer project_id;

    @Data
    public class Stats {
        private String additions;
        private String deletions;
        private String total;
    }

    public void createStats() {
        stats = new Stats();
    }

    private String projectName;
    private String last_branch;
    private String projectName_branch;
    private String additions;
    private String deletions;
    private String total;
}
