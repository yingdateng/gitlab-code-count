package com.shtel.paas.gitlab.entity;

import lombok.Data;

import java.util.List;

@Data
public class UserInfo {
    private Integer id;
    private String name;
    private String username;
    private String state;
    private String avatar_url;
    private String web_url;
    private List<ProjectInfo> projectInfos;
}
