package com.shtel.paas.gitlab.service;

import com.alibaba.fastjson.JSONObject;
import com.shtel.paas.gitlab.config.GitlabConfig;
import com.shtel.paas.gitlab.entity.*;
import com.shtel.paas.gitlab.entity.gitlab.GitlabAccessToken;
import com.shtel.paas.gitlab.utils.ExcelUtil;
import com.shtel.paas.gitlab.utils.FileDownloadUtil;
import com.shtel.paas.gitlab.utils.GitlabAPI;
import com.shtel.paas.gitlab.utils.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Service
public class GitlabService {

    private static final Logger logger = LoggerFactory.getLogger(GitlabService.class);

    private static final String PAGE = "1";

    private static final String PER_PAGE = "100";

    private static final String TOTAL_PAGES_KEY = "X-Total-Pages";

    private static final String DEFAULT_BRANCH = "dev";

    private static final String GRANT_TYPE = "password";

    private static final String ACTION_NAME = "pushed";

    private static final String EMPTY_STRING = "";

    private static final String USER_DIMENSION = "user";

    private static final String PROJECT_DIMENSION = "project";

    private static final String[] TITLE = {"姓名", "email", "新增行数", "移除行数", "影响总行数"};

    private static final String[] TITLE_ALL = {"项目名称", "姓名", "email", "新增行数", "移除行数", "影响总行数"};

    private static final String[] USER_TITLE = {"姓名", "总添加行数", "总删除行数", "总影响行数", "项目数", "项目名-[分支](增加行,移除行,总影响行)"};

    private static RestTemplate restTemplate = HttpClientUtil.restTemplate();

    private static SimpleDateFormat dateFormatOfUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static String userToken = "";

    private static String gitLabAddress = "";

    @Autowired
    GitlabConfig gitlabConfig;

    /**
     * 浏览器下载代码统计的excel
     *
     * @param response
     * @param gitlabInfo
     */
    public void codeCountExcelDownload(HttpServletResponse response, GitlabInfo gitlabInfo) {

        HSSFWorkbookDTO hssfWorkbookDTO = getGitlabHSSFWorkbook(null);
        // 响应到客户端
        try {
            setResponseHeader(response, "gitlab代码信息统计表" + FileDownloadUtil.dateFormat() + ".xls");
            OutputStream os = response.getOutputStream();
            hssfWorkbookDTO.write(os);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 代码统计
     *
     * @param gitlabInfo
     */
    public HashMap<String, Object> codeCountExcelSaveToLocal(GitlabInfo gitlabInfo) {
        logger.info("**************Start*************");
        logger.info("**************Please wait...*************");

        userToken = getGitlabAccessToken(gitlabInfo);
        gitLabAddress = gitlabInfo.getGitlabUrl();

        HashMap<String, Object> resultMap = codeCount(gitlabInfo);

        logger.info("**************End*************");
        return resultMap;
    }

    /**
     * 统计
     *
     * @param gitlabInfo
     * @return
     */
    public HashMap<String, Object> codeCount(GitlabInfo gitlabInfo) {

        HashMap<String, Object> map = new HashMap<String, Object>();
        // 时间段
        String[] periods = null;
        String period = gitlabInfo.getPeriod();
        if (period != null && period != EMPTY_STRING) {
            periods = period.split(" - ");
        }

        // 成员维度
        List<CountResultInfo> userDimension = new ArrayList<>();
        List<String> usernames = gitlabInfo.getUsernames() == null ? new ArrayList<>() : gitlabInfo.getUsernames();

        // 项目维度
        List<CommitInfo> projectDimension = new ArrayList<>();

        List<ProjectInfo> projectsOfProject = gitlabInfo.getProjects();
        if (projectsOfProject != null && projectsOfProject.size() > 0) {
            getDataByDimension(usernames, projectsOfProject, periods, userDimension, projectDimension);
        }

        // 创建excel
        if (gitlabInfo.getIfCreateExcel()) {
            saveProjectHSSFWorkbooks(projectDimension);
            saveUserHSSFWorkbooks(userDimension);
        }
        Map<String, List<CommitInfo>> projectCommitsMap = projectDimension.stream().collect(Collectors.groupingBy
                (CommitInfo::getProjectName));
        map.put("userDimension", userDimension);
        map.put("projectDimension", projectCommitsMap);
        return map;
    }

    /**
     * 判断UTC时间是否在时间段内
     * @param periods
     * @param UTCTime
     * @return
     */
    public static Boolean ifInPeriods (String[] periods, String UTCTime) {
        Boolean resultFlag = false;
        int res0 = 0;
        int res1 = 0;
        if (periods != null && periods.length > 0) {
            String lastActivity;
            try {
                Date date = dateFormatOfUTC.parse(UTCTime);
                lastActivity = dateFormat.format(date);
                res0 = lastActivity.compareTo(periods[0]);
                res1 = lastActivity.compareTo(periods[1]);
            } catch (Exception e) {
                logger.error("Error: " + e.getStackTrace());
            }
        }
        if (res0 >= 0 && res1 <= 0) {
            resultFlag = true;
        }
        return resultFlag;
    }

    /**
     * 获取不同维度的数据
     *
     * @param usernames
     * @param projectInfos
     * @param periods
     * @param userDimensionData
     * @param projectDimensionData
     */
    public void getDataByDimension(List<String> usernames, List<ProjectInfo> projectInfos, String[] periods,
                                   List<CountResultInfo> userDimensionData, List<CommitInfo> projectDimensionData) {

        projectInfos.forEach(projectInfo -> {
            List<CommitInfo> commitInfoTemp = asyncGetCommitInfo(projectInfo, periods);
            if (commitInfoTemp != null && commitInfoTemp.size() > 0) {
                projectDimensionData.addAll(commitInfoTemp);
            }
        });

        if (projectDimensionData != null && projectDimensionData.size() > 0) {
            Map<String, List<CommitInfo>> commitInfoGroupEmail = projectDimensionData.stream().collect(Collectors
                    .groupingBy(CommitInfo::getCommitter_email));
            List<CountResultInfo> countResultInfos = formatToUserDimension(commitInfoGroupEmail, usernames);
            if (countResultInfos != null) {
                userDimensionData.addAll(countResultInfos);
            }
        }
    }

    /**
     * 成员维度-数据格式化
     *
     * @param commitInfoMap
     * @return
     */
    public static List<CountResultInfo> formatToUserDimension(Map<String, List<CommitInfo>> commitInfoMap, List<String> usernames) {
        List<CountResultInfo> countResultInfos = new ArrayList<>();
        Map<String, String> map = new HashMap<String, String>(8);
        map.put("additionsAll", "0");
        map.put("deletionsAll", "0");
        map.put("totalAll", "0");
        map.put("projectCount", "0");
        map.put("isMark", null);
        map.put("details", EMPTY_STRING);
        commitInfoMap.forEach((key, value) -> {
            if (value != null && value.size() > 0) {
                value.forEach(commitInfo -> {
                    if (usernames != null && usernames.size() > 0 && usernames.contains(commitInfo.getCommitter_name())) {
                        map.put("isMark", "true");
                    }
                    map.put("username", commitInfo.getCommitter_name());
                    map.put("additionsAll", String.valueOf(Integer.parseInt(commitInfo.getStats().getAdditions()) + Integer
                            .parseInt(map.get("additionsAll"))));
                    map.put("deletionsAll", String.valueOf(Integer.parseInt(commitInfo.getStats().getDeletions()) + Integer
                            .parseInt(map.get("deletionsAll"))));
                    map.put("totalAll", String.valueOf(Integer.parseInt(commitInfo.getStats().getTotal()) + Integer
                            .parseInt(map.get("totalAll"))));
                    map.put("projectCount", String.valueOf(1 + Integer
                            .parseInt(map.get("projectCount"))));
                    map.put("details", commitInfo.getProjectName() + "-[" + commitInfo.getLast_branch() + "]("
                            + commitInfo.getStats().getAdditions() + ","
                            + commitInfo.getStats().getDeletions() + "," + commitInfo.getStats().getTotal()
                            + ")" + ";" + map.get("details"));
                });
                CountResultInfo countResultInfo = new CountResultInfo();
                countResultInfo.setUserEmail(key);
                countResultInfo.setIsMark(map.get("isMark"));
                countResultInfo.setUsername(map.get("username"));
                countResultInfo.setAdditionsAll(map.get("additionsAll"));
                countResultInfo.setDeletionsAll(map.get("deletionsAll"));
                countResultInfo.setTotalAll(map.get("totalAll"));
                countResultInfo.setProjectCount(map.get("projectCount"));
                countResultInfo.setDetails(map.get("details"));
                countResultInfos.add(countResultInfo);
                map.put("additionsAll", "0");
                map.put("deletionsAll", "0");
                map.put("totalAll", "0");
                map.put("projectCount", "0");
                map.put("isMark", null);
                map.put("details", EMPTY_STRING);
            }
        });
        return countResultInfos;
    }

    /**
     * 获取HSSFWorkbook 多个sheet页
     *
     * @param projects
     * @return
     */
    public HSSFWorkbookDTO getGitlabHSSFWorkbook(List<ProjectInfo> projects) {

        HSSFWorkbookDTO hssfWorkbookDTO = new HSSFWorkbookDTO();
        if (projects != null && projects.size() > 0) {
            projects.forEach(projectInfo -> {
                try {
                    String fileName = projectInfo.getName();
                    ExcelUtil.createExcel(projectInfo.getCommitsInfo(), TITLE, fileName, hssfWorkbookDTO);
                } catch (Exception e) {
                    logger.error("Create an excel exception: " + e.getStackTrace());
                }
            });
        }
        return hssfWorkbookDTO;
    }

    /**
     * 多线程保存excel
     *
     * @param commitsInfo
     * @param hssfWorkbookAll
     * @param sheetName
     * @param date
     */
    @Async("taskExecutor")
    public void asyncSaveExcel(List<CommitInfo> commitsInfo, HSSFWorkbookDTO hssfWorkbookAll,
                               String sheetName, long date) {
        logger.info("Saving " + commitsInfo.get(0).getProjectName() + " excel to Local ...");

        try {
            String fileName = commitsInfo.get(0).getProjectName();
            HSSFWorkbookDTO hssfWorkbookDTO = ExcelUtil.createExcel(commitsInfo, TITLE, fileName);
            hssfWorkbookDTO.setProjectName(fileName);
            hssfWorkbookDTO.setDefaultBranch(commitsInfo.get(0).getLast_branch());

            FileDownloadUtil.saveExcelToLocal(hssfWorkbookDTO, date);
            logger.info(hssfWorkbookDTO.getExcelName() + " Excel saved successfully...");

            if (hssfWorkbookAll != null) {
                // 总的excel添加数据
                ExcelUtil.setHSSFWorkbook(sheetName, ExcelUtil.initContent(commitsInfo, TITLE_ALL, fileName), hssfWorkbookAll);
            }
        } catch (Exception e) {
            logger.error("Create an excel exception: " + e.getStackTrace());
        }
    }

    /**
     * 获取单个项目提交信息
     *
     * @param projectInfo
     * @return
     */
    public List<CommitInfo> asyncGetCommitInfo(ProjectInfo projectInfo, String[] periods) {
        logger.info("Querying " + projectInfo.getName() + " item submission record ...");
        List<CommitInfo> commitsInfo;
        // 单个项目提交记录
        try {
            commitsInfo = groupCommitInfo(getProjectCommits(projectInfo.getId(), projectInfo.getLast_branch(),
                    periods).get());
            if (commitsInfo != null && commitsInfo.size() > 0) {
                commitsInfo.forEach(commitInfo -> {
                    commitInfo.setProject_id(Integer.parseInt(projectInfo.getId()));
                    commitInfo.setLast_branch(projectInfo.getLast_branch());
                    commitInfo.setProjectName_branch(projectInfo.getName() + "-[" + projectInfo
                            .getLast_branch() + "]");
                    commitInfo.setProjectName(projectInfo.getName());
                });
            }
        } catch (Exception e) {
            commitsInfo = null;
            logger.error("Querying " + projectInfo.getName() + " item submission record error: " + e.getStackTrace());
        }

        return commitsInfo;
    }

    /**
     * 根据用户过滤用户的提交记录
     *
     * @param commitsInfo
     * @param usernames
     * @return
     */
    public List<CommitInfo> filterCommitInfoByUser(List<CommitInfo> commitsInfo, List<String> usernames) {
        List<CommitInfo> commitInfoList = new ArrayList<>();
        // 单个项目提交记录
        try {
            commitInfoList = commitsInfo.stream().filter(commitInfo -> usernames.contains(
                    commitInfo.getCommitter_name())).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error: " + e.getStackTrace());
        }

        return commitInfoList;
    }

    /**
     * 保存项目的excel
     *
     * @param commitsInfo
     */
    public void saveProjectHSSFWorkbooks(List<CommitInfo> commitsInfo) {

        String sheetName = "代码统计表(全)" + FileDownloadUtil.dateFormat();
        // 总计的excel
        HSSFWorkbookDTO hssfWorkbookAll = ExcelUtil.initHSSFWorkbook(sheetName, TITLE_ALL);
        long date = System.currentTimeMillis();
        Map<String, List<CommitInfo>> map = commitsInfo.stream().collect(Collectors.groupingBy(CommitInfo::getProjectName));
        map.forEach((key, value) -> asyncSaveExcel(value, hssfWorkbookAll, sheetName, date));
        hssfWorkbookAll.setExcelName("(全)");
        FileDownloadUtil.saveExcelToLocal(hssfWorkbookAll, date);
        logger.info("All Excel saved successfully!");
    }

    /**
     * 保存成员维度代码统计excel
     *
     * @param countResultInfos
     */
    public void saveUserHSSFWorkbooks(List<CountResultInfo> countResultInfos) {
        long date = System.currentTimeMillis();
        try {
            HSSFWorkbookDTO hssfWorkbookDTO = ExcelUtil.createUserExcel(countResultInfos, USER_TITLE, "成员维度");
            FileDownloadUtil.saveExcelToLocal(hssfWorkbookDTO, date);
        } catch (Exception e) {
            logger.error("Create an excel exception: " + e.getStackTrace());
        }
    }

    /**
     * 获取用户的accessToken
     *
     * @param gitlabInfo
     * @return
     */
    public String getGitlabAccessToken(GitlabInfo gitlabInfo) {

        String accessToken;
        try {
            GitlabAccessToken gitlabAccessToken = GitlabAPI.getAccessToken(gitlabInfo.getGitlabUrl(), GRANT_TYPE, gitlabInfo.getUsername(), gitlabInfo.getPassword());
            accessToken = gitlabAccessToken.getAccessToken();
            return accessToken;
        } catch (IOException e) {
            logger.error("Failed to get user token: " + e.getStackTrace());
            return null;
        }
    }


    /**
     * 根据提交id获取提交记录详情 - 针对电信gitlab API获取不到行数
     *
     * @param projectId
     * @param commitId
     * @return
     */
    public CommitInfo getCommitsById(String projectId, String commitId) {
        logger.info("Querying commitId = " + commitId + " submission record ...");
        CommitInfo commitInfo = null;
        HttpHeaders httpHeaders = new HttpHeaders();
        String gitlabUrlOfCommitId = HttpClientUtil.parseUrl(gitlabConfig.getPath()
                .getCommitInfoByProjectIdAndCommitId(), new HashMap<String, String>(4) {{
            put("address", gitLabAddress);
            put("projectId", projectId);
            put("commitId", commitId);
        }}, new HashMap<String, String>(2) {{
            put("access_token", userToken);
        }});
        try {
            ResponseEntity<String> response = restTemplate.exchange(gitlabUrlOfCommitId, HttpMethod.GET, new HttpEntity<String>(httpHeaders), String.class);
            commitInfo = JSONObject.parseObject(response.getBody(), CommitInfo.class);
        } catch (Exception e) {
            logger.error("Queried commitId = " + commitId + " submission record filed! " + e.getStackTrace());
        }
        return commitInfo;
    }


    /**
     * 获取单个项目的提交记录
     *
     * @param projectId
     * @return
     */
    @Async("taskExecutor")
    public CompletableFuture<List<CommitInfo>> getProjectCommits(String projectId, String branchesName, String[]
            periods) {

        HashMap<String, String> params = new HashMap<String, String>(4) {{
            put("access_token", userToken);
            put("per_page", PER_PAGE);
            put("page", PAGE);
            put("with_stats", "true");
            // 分支
            put("ref_name", branchesName);
            if (periods != null && periods.length > 0) {
                // 提交时间段 YYYY-MM-DDTHH:MM:SSZ
                put("since", periods[0]);
                put("until", periods[1]);
            }
        }};

        // 格式化url
        String gitlabUrl = HttpClientUtil.parseUrl(gitlabConfig.getPath().getProjectCommitsInfo(), new HashMap<String, String>(2) {{
            put("address", gitLabAddress);
            put("projectId", projectId);
        }}, null);

        List<CommitInfo> commitInfoList = getAllByObject(gitlabUrl, params, CommitInfo.class);

        // 如果没有返回提交代码行数，再进行查询一次
        if (commitInfoList.get(0).getStats() == null) {
            commitInfoList.forEach(commitInfo -> {
                CommitInfo commitInfoTemp = getCommitsById(projectId, commitInfo.getId());
                commitInfo.createStats();
                commitInfo.getStats().setAdditions(commitInfoTemp.getStats().getAdditions());
                commitInfo.getStats().setDeletions(commitInfoTemp.getStats().getDeletions());
                commitInfo.getStats().setTotal(commitInfoTemp.getStats().getTotal());
            });
        }
        return CompletableFuture.completedFuture(commitInfoList);
    }

    /**
     * 获取用户列表
     *
     * @param gitlabInfo
     * @return
     */
    public List<UserInfo> getUserList(GitlabInfo gitlabInfo) {

        userToken = getGitlabAccessToken(gitlabInfo);
        gitLabAddress = gitlabInfo.getGitlabUrl();

        logger.info("Querying user list ...");
        HashMap<String, String> params = new HashMap<String, String>(4) {{
            put("access_token", userToken);
            put("per_page", PER_PAGE);
            put("page", PAGE);
        }};

        // 格式化url
        String gitlabUrl = HttpClientUtil.parseUrl(gitlabConfig.getPath().getUserList(), new HashMap<String, String>(2) {{
            put("address", gitLabAddress);
        }}, null);
        List<UserInfo> userList = new ArrayList<>();
        try {
            userList = getAllByObject(gitlabUrl, params, UserInfo.class);
            logger.info("Query user list is successful!");
        } catch (Exception e) {
            logger.error("Error: " + e.getStackTrace());
        }

        return userList;
    }

    /**
     * 获取用户操作过的项目
     *
     * @param gitlabInfo
     * @return
     */
    public List<ProjectInfo> getProjectListByUsers(GitlabInfo gitlabInfo) {

        userToken = getGitlabAccessToken(gitlabInfo);
        gitLabAddress = gitlabInfo.getGitlabUrl();

        List<String> usernames = gitlabInfo.getUsernames();
        List<UserEventInfo> userEventInfos;
        Map<String, String> map = new HashMap<>();
        List<ProjectInfo> projectInfos = new ArrayList<>();
        for (String username : usernames) {
            userEventInfos = getUserPushedEvents(username, null);
            if (userEventInfos != null && userEventInfos.size() > 0) {
                userEventInfos.forEach(userEventInfo -> map.put(userEventInfo.getProject_id().toString(), EMPTY_STRING));
            }
        }

        if (map != null && map.size() > 0) {
            map.forEach((key, value) -> {
                ProjectInfo projectInfo = getProjectById(key);
                // 设置项目的分支信息
                setProjectBranchesInfo(projectInfo, null);
                if (projectInfo != null) {
                    projectInfos.add(projectInfo);
                }
            });
        }
        return projectInfos;
    }

    /**
     * 设置项目/项目列表的分支信息
     *
     * @param projectInfos
     */
    public void setProjectBranchesInfo(ProjectInfo project, List<ProjectInfo> projectInfos) {
        if (project != null) {
            List<BranchesInfo> branchesInfos = getBranchesInfoListByProjectId(project.getId());
            String lastBranch = compareToTime(branchesInfos);
            project.setLast_branch(lastBranch);
            project.setBranchesInfos(branchesInfos);
        }
        if (projectInfos != null && projectInfos.size() > 0) {
            projectInfos.forEach(projectInfo -> {
                List<BranchesInfo> branchesInfos = getBranchesInfoListByProjectId(projectInfo.getId());
                String lastBranch = compareToTime(branchesInfos);
                projectInfo.setLast_branch(lastBranch);
                projectInfo.setBranchesInfos(branchesInfos);
            });
        }
    }

    /**
     * 获取用户的提交代码的events
     *
     * @param username
     * @param periods
     * @return
     */

    public List<UserEventInfo> getUserPushedEvents(String username, String[] periods) {
        logger.info("Querying username = " + username + " pushed events ...");
        List<UserEventInfo> returnUserEventInfos = null;
        HashMap<String, String> params = new HashMap<String, String>(4) {{
            put("access_token", userToken);
            put("per_page", PER_PAGE);
            put("page", PAGE);
            put("action", ACTION_NAME);
        }};

        // 格式化url
        String gitlabUrl = HttpClientUtil.parseUrl(gitlabConfig.getPath().getUserEvent(), new HashMap<String, String>(2) {{
            put("address", gitLabAddress);
            put("username", username);
        }}, null);

        try {
            List<UserEventInfo> userEventInfos;
            userEventInfos = getAllByObject(gitlabUrl, params, UserEventInfo.class);
            if (periods != null && periods.length > 0) {
                for (UserEventInfo userEventInfo : userEventInfos) {
                    if (ifInPeriods(periods, userEventInfo.getCreated_at())) {
                        returnUserEventInfos.add(userEventInfo);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error: " + e.getStackTrace());
        }

        return returnUserEventInfos;
    }

    /**
     * 项目分支列表以及分支最新提交信息
     *
     * @param projectId
     * @return
     */
    public List<BranchesInfo> getBranchesInfoListByProjectId(String projectId) {

        HashMap<String, String> params = new HashMap<String, String>(2) {{
            put("access_token", userToken);
        }};
        String url = HttpClientUtil.parseUrl(gitlabConfig.getPath().getBranchesInfoListByProjectId(), new HashMap<String, String>(2) {{
            put("address", gitLabAddress);
            put("projectId", projectId);
        }}, null);

        List<BranchesInfo> branchesInfos = getAllByObject(url, params, BranchesInfo.class);
        return branchesInfos;
    }

    /**
     * 获取所有数据（针对分页的接口）
     *
     * @param url
     * @param params
     * @param objClass
     * @param <T>
     * @return
     */
    public static <T> List<T> getAllByObject(String url, HashMap<String, String> params, Class<T> objClass) {

        List<T> returnList = new ArrayList<T>();

        String gitlabUrl = HttpClientUtil.parseUrl(url, new HashMap<String, String>(2){}, params);
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            ResponseEntity<String> responseOfOne = restTemplate.exchange(gitlabUrl, HttpMethod.GET, new HttpEntity<String>(httpHeaders), String.class);
            List<T> oneList = JSONObject.parseArray(responseOfOne.getBody(), objClass);
            if (oneList != null && oneList.size() > 0) {
                returnList.addAll(oneList);
            }

            HttpHeaders headers = responseOfOne.getHeaders();
            String totalPageStr = headers.get(TOTAL_PAGES_KEY).toString();
            int totalPages = Integer.valueOf(totalPageStr.substring(1, totalPageStr.length() - 1));
            if (totalPages > 1) {
                for (int i = 2; i <= totalPages; i++) {
                    params.put("page", String.valueOf(i));
                    gitlabUrl = HttpClientUtil.parseUrl(url, new HashMap<String, String>(2){}, params);
                    ResponseEntity<String> responseTemp = restTemplate.exchange(gitlabUrl, HttpMethod.GET, new HttpEntity<String>(httpHeaders), String.class);
                    List<T> projectsTemp = JSONObject.parseArray(responseTemp.getBody(), objClass);
                    if (projectsTemp != null && projectsTemp.size() > 0) {
                        returnList.addAll(projectsTemp);
                    }
                }
            }

            return returnList;
        } catch (Exception e) {
            logger.error("Failed to get list request: " + url + " massage: " + e.getStackTrace());
            return returnList;
        }
    }

    /**
     * 获取BranchesInfos里面的最新时间提交的分支
     *
     * @param branchesInfos
     * @return
     */
    public static String compareToTime(List<BranchesInfo> branchesInfos) {
        String branchesName = DEFAULT_BRANCH;
        if (branchesInfos.size() == 1) {
            branchesName = branchesInfos.get(0).getName();
        }
        if (branchesInfos.size() > 1) {
            try {
                BranchesInfo branchesInfoMax = branchesInfos.get(0);
                long maxTime = dateFormatOfUTC.parse(branchesInfoMax.getCommit().getCommitted_date()).getTime();
                for (int i = 1; i <= branchesInfos.size(); i++) {
                    if (i >= branchesInfos.size()) {
                        break;
                    }
                    long tempTime = dateFormatOfUTC.parse(branchesInfos.get(i).getCommit().getCommitted_date()).getTime();
                    if (tempTime > maxTime) {
                        maxTime = tempTime;
                        branchesInfoMax = branchesInfos.get(i);
                        branchesName = branchesInfos.get(i).getName();
                    } else {
                        branchesName = branchesInfoMax.getName();
                    }
                }
            } catch (Exception e) {
                logger.error("Error: " + e.getStackTrace());
            }

        }
        return branchesName;
    }

    /**
     * 聚合单个项目提交信息
     *
     * @param commitsInfo
     * @return
     */
    public static List<CommitInfo> groupCommitInfo(List<CommitInfo> commitsInfo) {
        logger.info("Aggregating submission records ...");
        Map<String, CommitInfo> map = new HashMap<>();
        if (commitsInfo != null && commitsInfo.size() > 0) {
            commitsInfo.forEach(commitInfo -> {
                if (map.containsKey(commitInfo.getCommitter_name())) {
                    CommitInfo temp = map.get(commitInfo.getCommitter_name());
                    try {
                        String additions = String.valueOf(Integer.valueOf(commitInfo.getStats().getAdditions()) + Integer.valueOf(temp.getStats().getAdditions()));
                        String deletions = String.valueOf(Integer.valueOf(commitInfo.getStats().getDeletions()) + Integer.valueOf(temp.getStats().getDeletions()));
                        String total = String.valueOf(Integer.valueOf(commitInfo.getStats().getTotal()) + Integer.valueOf(temp.getStats().getTotal()));
                        temp.getStats().setAdditions(additions);
                        temp.getStats().setDeletions(deletions);
                        temp.getStats().setTotal(total);
                    } catch (Exception e) {
                        logger.error("Error: " + e.getStackTrace());
                    }
                } else {
                    // map中不存在，新建key，用来存放数据
                    map.put(commitInfo.getCommitter_name(), commitInfo);
                }
            });
        }
        logger.info("Aggregate commit record completed ...");
        return new ArrayList(map.values());
    }

    /**
     * 根据项目id获取项目详情
     *
     * @param projectId
     * @return
     */
    public ProjectInfo getProjectById(String projectId) {
        logger.info("Querying projectId = " + projectId + " info ...");
        ProjectInfo projectInfo = null;
        HashMap<String, String> params = new HashMap<String, String>(2) {{
            put("access_token", userToken);
        }};

        // 格式化url
        String gitlabUrl = HttpClientUtil.parseUrl(gitlabConfig.getPath().getProjectInfo(), new HashMap<String, String>(2) {{
            put("address", gitLabAddress);
            put("projectId", projectId);
        }}, params);
        HttpHeaders httpHeaders = new HttpHeaders();
        try {
            ResponseEntity<String> response = restTemplate.exchange(gitlabUrl, HttpMethod.GET, new HttpEntity<String>(httpHeaders), String.class);
            projectInfo = JSONObject.parseObject(response.getBody(), ProjectInfo.class);
        } catch (Exception e) {
            logger.error("Queried projectId = " + projectId + " info filed! " + e.getStackTrace());
        }

        return projectInfo;
    }

    /**
     * 发送响应流方法
     *
     * @param response
     * @param fileName
     */
    public static void setResponseHeader(HttpServletResponse response, String fileName) {

        try {
            try {
                fileName = new String(fileName.getBytes(), "ISO8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            response.setContentType("application/octet-stream;charset=ISO8859-1");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * 获取项目的成员
     *
     * @param projectId
     * @return
     */
    public List<UserInfo> getUsersByProject(String projectId) {

        HashMap<String, String> params = new HashMap<String, String>(4) {{
            put("access_token", userToken);
            put("per_page", PER_PAGE);
            put("page", PAGE);
        }};

        // 格式化url
        String gitlabUrl = HttpClientUtil.parseUrl(gitlabConfig.getPath().getUsersByProject(), new HashMap<String, String>(2) {{
            put("address", gitLabAddress);
            put("projectId", projectId);
        }}, null);
        List<UserInfo> userInfoList = getAllByObject(gitlabUrl, params, UserInfo.class);
        return userInfoList;
    }

    /**
     * 获取所有项目列表
     *
     * @param gitlabInfo
     * @return
     */
    public List<ProjectInfo> getProjectList(GitlabInfo gitlabInfo, Boolean getLastBranche) {

        if (userToken == null || EMPTY_STRING.equals(userToken)) {
            userToken = getGitlabAccessToken(gitlabInfo);
        }
        if (gitLabAddress == null || EMPTY_STRING.equals(gitLabAddress)) {
            gitLabAddress = gitlabInfo.getGitlabUrl();
        }

        logger.info("Querying project list ...");
        List<ProjectInfo> projectInfos = new ArrayList<>();
        HashMap<String, String> params = new HashMap<String, String>(4) {{
            put("access_token", userToken);
            put("per_page", PER_PAGE);
            put("page", PAGE);
        }};

        // 格式化url
        String gitlabUrl = HttpClientUtil.parseUrl(gitlabConfig.getPath().getProjectList(), new HashMap<String, String>
                (2) {{
            put("address", gitLabAddress);
        }}, null);

        try {
            projectInfos = getAllByObject(gitlabUrl, params, ProjectInfo.class);
        } catch (Exception e) {
            logger.error("Error: " + e.getStackTrace());
        }

        if (getLastBranche) {
            setProjectBranchesInfo(null, projectInfos);
        }
        logger.info("Query project list is successful!");
        return projectInfos;
    }
}
