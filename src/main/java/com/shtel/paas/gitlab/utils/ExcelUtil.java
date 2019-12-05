package com.shtel.paas.gitlab.utils;

import com.shtel.paas.gitlab.entity.CommitInfo;
import com.shtel.paas.gitlab.entity.CountResultInfo;
import com.shtel.paas.gitlab.entity.HSSFWorkbookDTO;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import java.util.List;

public class ExcelUtil {
    /**
     * 导出Excel
     *
     * @param sheetName sheet名称
     * @param title 标题
     * @param values 内容
     * @param wb HSSFWorkbook对象
     * @return
     */

    private final static int COLUMN_WIDTH = 20;

    public static HSSFWorkbookDTO getHSSFWorkbook(String sheetName, String[] title, String[][] values, HSSFWorkbookDTO wb) {

        // 第一步，创建一个HSSFWorkbook，对应一个Excel文件
        if (wb == null) {
            wb = new HSSFWorkbookDTO();
        }

        // 第二步，在workbook中添加一个sheet,对应Excel文件中的sheet
        HSSFSheet sheet = wb.createSheet(sheetName);

        // 第三步，在sheet中添加表头第0行,注意老版本poi对Excel的行数列数有限制
        HSSFRow row = sheet.createRow(0);

        // 第四步，创建单元格，并设置值表头 设置表头居中
        HSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 创建一个居中格式

        //声明列对象
        HSSFCell cell = null;

        //创建标题
        for (int i = 0; i < title.length; i++) {
            sheet.setColumnWidth(i, COLUMN_WIDTH * 256);
            cell = row.createCell(i);
            cell.setCellValue(title[i]);
            cell.setCellStyle(style);
        }

        //创建内容
        for (int i = 0; i < values.length; i++) {
            row = sheet.createRow(i + 1);
            for (int j = 0; j < values[i].length; j++) {
                //将内容按顺序赋给对应的列对象
                row.createCell(j).setCellValue(values[i][j]);
            }
        }
        return wb;
    }

    /**
     * 填充内容
     * @param sheetName
     * @param values
     * @param wb
     */
    public static void setHSSFWorkbook(String sheetName, String[][] values, HSSFWorkbookDTO wb) {

        // 第一步，在workbook中获取sheet
        HSSFSheet sheet = wb.getSheet(sheetName);

        HSSFRow row;

        //创建内容
        for (int i = 0; i < values.length; i++) {
            row = sheet.createRow(sheet.getLastRowNum() + 1);
            for (int j = 0; j < values[i].length; j++) {
                //将内容按顺序赋给对应的列对象
                row.createCell(j).setCellValue(values[i][j]);
            }
        }
    }

    /**
     * 初始化一个带有指定sheet和列名的excel
     *
     * @param sheetName
     * @param title
     * @return
     */
    public static HSSFWorkbookDTO initHSSFWorkbook(String sheetName, String[] title) {

        // 第一步，创建一个HSSFWorkbook
        HSSFWorkbookDTO wb = new HSSFWorkbookDTO();

        // 第二步，在workbook中添加一个sheet,对应Excel文件中的sheet
        HSSFSheet sheet = wb.createSheet(sheetName);

        // 第三步，在sheet中添加表头第0行,注意老版本poi对Excel的行数列数有限制
        HSSFRow row = sheet.createRow(0);

        // 第四步，创建单元格，并设置值表头 设置表头居中
        HSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 创建一个居中格式

        //声明列对象
        HSSFCell cell = null;

        //创建标题
        for (int i = 0; i < title.length; i++) {
            sheet.setColumnWidth(i, COLUMN_WIDTH * 256);
            cell = row.createCell(i);
            cell.setCellValue(title[i]);
            cell.setCellStyle(style);
        }
        return wb;
    }

    /**
     * 创建excel文件内容
     *
     * @param list
     * @param fileName
     * @return
     * @throws Exception
     */
    public static void createExcel(List<CommitInfo> list, String[] title, String fileName, HSSFWorkbookDTO wb) throws Exception {

        String[][] content = new String[list.size()][title.length];

        //sheet名
        String sheetName = fileName;

        for (int i = 0; i < list.size(); i++) {
            content[i] = new String[title.length];
            CommitInfo obj = list.get(i);
            content[i][0] = obj.getCommitter_name();
            content[i][1] = obj.getCommitter_email();
            content[i][2] = obj.getStats().getAdditions();
            content[i][3] = obj.getStats().getDeletions();
            content[i][4] = obj.getStats().getTotal();
        }

        //创建HSSFWorkbook
        ExcelUtil.getHSSFWorkbook(sheetName, title, content, wb);
    }

    /**
     * 创建excel - 单个sheet页
     *
     * @param list
     * @param title
     * @param fileName
     * @return
     * @throws Exception
     */
    public static HSSFWorkbookDTO createExcel(List<CommitInfo> list, String[] title, String fileName) throws Exception {

        String[][] content = new String[list.size()][title.length];

        //sheet名
        String sheetName = fileName;

        for (int i = 0; i < list.size(); i++) {
            content[i] = new String[title.length];
            CommitInfo obj = list.get(i);
            content[i][0] = obj.getCommitter_name();
            content[i][1] = obj.getCommitter_email();
            content[i][2] = obj.getStats().getAdditions();
            content[i][3] = obj.getStats().getDeletions();
            content[i][4] = obj.getStats().getTotal();
        }

        //创建HSSFWorkbook
        HSSFWorkbookDTO hssfWorkbookDTO = ExcelUtil.getHSSFWorkbook(sheetName, title, content, null);
        hssfWorkbookDTO.setExcelName(sheetName);
        return hssfWorkbookDTO;
    }

    /**
     * 创建用户维度的excel-单个
     * @param list
     * @param title
     * @param fileName
     * @return
     * @throws Exception
     */
    public static HSSFWorkbookDTO createUserExcel(List<CountResultInfo> list, String[] title, String fileName) throws
            Exception {

        String[][] content = new String[list.size()][title.length];

        //sheet名
        String sheetName = fileName;

        for (int i = 0; i < list.size(); i++) {
            content[i] = new String[title.length];
            CountResultInfo obj = list.get(i);
            content[i][0] = obj.getUsername();
            content[i][1] = obj.getAdditionsAll();
            content[i][2] = obj.getDeletionsAll();
            content[i][3] = obj.getTotalAll();
            content[i][4] = obj.getProjectCount();
            content[i][5] = obj.getDetails();
        }

        //创建HSSFWorkbook
        HSSFWorkbookDTO hssfWorkbookDTO = ExcelUtil.getHSSFWorkbook(sheetName, title, content, null);
        hssfWorkbookDTO.setExcelName(sheetName);
        return hssfWorkbookDTO;
    }

    /**
     * 整理excel内容 -（带项目名称列）
     * @param list
     * @param title
     * @param fileName
     * @return
     */
    public static String[][] initContent(List<CommitInfo> list, String[] title, String fileName) {
        if (list != null && list.size() > 0) {
            list.get(0).setProjectName(fileName);
        } else {
            CommitInfo commitInfo = new CommitInfo();
            commitInfo.setProjectName(fileName);
            commitInfo.setCommitter_name("");
            commitInfo.setCommitter_email("");
            commitInfo.createStats();
            commitInfo.getStats().setAdditions("");
            commitInfo.getStats().setDeletions("");
            commitInfo.getStats().setTotal("");
            list.add(commitInfo);
        }
        String[][] content = new String[list.size()][title.length];
        for (int i = 0; i < list.size(); i++) {
            content[i] = new String[title.length];
            CommitInfo obj = list.get(i);
            content[i][0] = obj.getProjectName();
            content[i][1] = obj.getCommitter_name();
            content[i][2] = obj.getCommitter_email();
            content[i][3] = obj.getStats().getAdditions();
            content[i][4] = obj.getStats().getDeletions();
            content[i][5] = obj.getStats().getTotal();
        }
        return content;
    }
}
