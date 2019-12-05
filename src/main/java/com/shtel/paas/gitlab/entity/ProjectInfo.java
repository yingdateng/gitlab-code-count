package com.shtel.paas.gitlab.entity;

import lombok.Data;

import java.util.List;

@Data
public class ProjectInfo {
    private String id;
    private String default_branch;
    /**
     * 统计的分支
     */
    private String last_branch;
    private String description;
    private String name;
    private String created_at;
    private String last_activity_at;
    private Owner owner;

    @Data
    public class Owner {
        private String id;
        private String name;
        private String username;
        private String state;
        private String web_url;
    }

    private List<BranchesInfo> branchesInfos;
    private List<CommitInfo> commitsInfo;
}
