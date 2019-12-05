package com.shtel.paas.gitlab.controller;

import com.shtel.paas.gitlab.entity.GitlabInfo;
import com.shtel.paas.gitlab.entity.ProjectInfo;
import com.shtel.paas.gitlab.entity.UserInfo;
import com.shtel.paas.gitlab.service.GitlabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/gitlab")
public class GitlabController {

    @Autowired
    private GitlabService gitlabService;

    /**
     * 浏览器下载代码统计的excel
     *
     * @param gitlabUrl
     * @param username
     * @param password
     * @param response
     */
    @GetMapping("/codeCount/download")
    public void download(@RequestParam String gitlabUrl,
                         @RequestParam String username,
                         @RequestParam String password,
                         HttpServletResponse response) {
        GitlabInfo gitlabInfo = new GitlabInfo();
        gitlabInfo.setGitlabUrl(gitlabUrl);
        gitlabInfo.setUsername(username);
        gitlabInfo.setPassword(password);
        Objects.requireNonNull(gitlabInfo.getGitlabUrl(), "gitlab地址不能为空！");
        Objects.requireNonNull(gitlabInfo.getUsername(), "用户名不能为空！");
        Objects.requireNonNull(gitlabInfo.getPassword(), "密码不能为空！");
        gitlabService.codeCountExcelDownload(response, gitlabInfo);
    }

    /**
     * 代码统计
     *
     * @param gitlabInfo
     * @return
     */
    @PostMapping("/codeCount/saveToLocal")
    @ResponseBody
    public Map<String, Object> saveToLocal(@RequestBody GitlabInfo gitlabInfo) {
        Objects.requireNonNull(gitlabInfo.getGitlabUrl(), "gitlab地址不能为空！");
        Objects.requireNonNull(gitlabInfo.getUsername(), "用户名不能为空！");
        Objects.requireNonNull(gitlabInfo.getPassword(), "密码不能为空！");
        Objects.requireNonNull(gitlabInfo.getProjects(), "项目不能为空！");
        HashMap<String, Object> resultMap = gitlabService.codeCountExcelSaveToLocal(gitlabInfo);
        return resultMap;
    }

    /**
     * 获取所有项目列表和成员列表
     * @param gitlabInfo
     * @return
     */
    @PostMapping("/codeCount/getProjectAndUserList")
    @ResponseBody
    public Map<String, Object> getProjectList(@RequestBody GitlabInfo gitlabInfo) {
        Objects.requireNonNull(gitlabInfo.getGitlabUrl(), "gitlab地址不能为空！");
        Objects.requireNonNull(gitlabInfo.getUsername(), "用户名不能为空！");
        Objects.requireNonNull(gitlabInfo.getPassword(), "密码不能为空！");
        List<ProjectInfo> projects = gitlabService.getProjectList(gitlabInfo, true);
        List<UserInfo> usersInfo = gitlabService.getUserList(gitlabInfo);
        Map<String, Object> map = new HashMap<String, Object>(2);
        map.put("projects", projects);
        map.put("usersInfo", usersInfo);
        return map;
    }
}
