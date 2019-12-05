package com.shtel.paas.gitlab.entity;

import lombok.Data;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

@Data
public class HSSFWorkbookDTO extends HSSFWorkbook {
    private String excelName;
    private String projectName;
    private String defaultBranch;
}
