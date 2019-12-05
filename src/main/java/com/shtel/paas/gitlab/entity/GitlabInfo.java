package com.shtel.paas.gitlab.entity;

import lombok.Data;

import java.util.List;

@Data
public class GitlabInfo {
    /**
     * token
     */
    private String private_token;

    /**
     * 项目id
     */
    private String projectId;

    /**
     * 成员id
     */
    private String userId;

    /*****************************来自页面的参数******************************
     * gitlab Url
     */
    private String gitlabUrl;
    /**
     * gitlab 用户名
     */
    private String username;
    /**
     * gitlab 密码
     */
    private String password;

    /**
     * 页面选择的成员名称list
     */
    private List<String> usernames;

    /**
     * 页面选择的项目list
     */
    private List<ProjectInfo> projects;

    /**
     * 是否生成excel; 默认false
     */
    private Boolean ifCreateExcel = false;

    /**
     * 时间段
     */
    private String period;
}
