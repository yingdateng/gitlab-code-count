package com.shtel.paas.gitlab.entity;

import lombok.Data;

@Data
public class UserEventInfo {
    private Integer project_id;
    private String action_name;
    private String author_id;
    private String author_username;
    private String created_at;
    private PushData push_data;
    @Data
    public class PushData {
        private String commit_count;
        private String action;
        private String ref_type;
        private String commit_from;
        private String commit_to;
        private String ref;
        private String commit_title;
    }
}
