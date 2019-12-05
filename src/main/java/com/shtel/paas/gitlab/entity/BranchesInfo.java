package com.shtel.paas.gitlab.entity;

import lombok.Data;

@Data
public class BranchesInfo {
    private String name;
    private Commit commit;

    @Data
    public class Commit {
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
    }
}
