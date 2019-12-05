package com.shtel.paas.gitlab.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties("service.gitlab")
public class GitlabConfig {

    /**
     * gitlab地址。
     */
    String address;

    Path path = new Path();

    @Getter
    public class Path {
        /**
         * 项目列表接口
         */
        String projectList = "{address}/api/v4/projects";

        /**
         * 成员列表接口
         */
        String userList = "{address}/api/v4/users";

        /**
         * 项目的提交记录列表详情接口
         */
        String projectCommitsInfo = "{address}/api/v4/projects/{projectId}/repository/commits";

        /**
         * 项目的单个提交记录详情接口
         */
        String commitInfoByProjectIdAndCommitId =
                "{address}/api/v4/projects/{projectId}/repository/commits/{commitId}";

        /**
         * 获取项目用户
         */
        String usersByProject = "{address}/api/v4/projects/{projectId}/users";

        /**
         * 列出存储库分支
         */
        String branchesInfoListByProjectId = "{address}/api/v4/projects/{projectId}/repository/branches";

        /**
         * 查询用户的 Event
         */
        String userEvent = "{address}/api/v4/users/{username}/events";

        /**
         * 项目信息
         */
        String projectInfo = "{address}/api/v4/projects/{projectId}";

    }

}
