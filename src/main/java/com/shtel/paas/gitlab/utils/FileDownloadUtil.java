package com.shtel.paas.gitlab.utils;

import com.shtel.paas.gitlab.entity.HSSFWorkbookDTO;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileDownloadUtil {

    private static final String SAVE_PATH = "c:\\gitlab_code_count\\";

    /**
     * 文件下载
     *
     * @param response
     * @param filename
     * @param path
     */
    public static void download(HttpServletResponse response, String filename, String path) {
        if (filename != null) {
            FileInputStream is = null;
            BufferedInputStream bs = null;
            OutputStream os = null;
            try {
                File file = new File(path);
                if (file.exists()) {
                    //设置Headers
                    response.setHeader("Content-Type", "application/octet-stream");
                    //设置下载的文件的名称-该方式已解决中文乱码问题
                    response.setHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes("gb2312"), "ISO8859-1"));
                    is = new FileInputStream(file);
                    bs = new BufferedInputStream(is);
                    os = response.getOutputStream();
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while ((len = bs.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                } else {
                    throw new IOException("下载的文件资源不存在");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (bs != null) {
                        bs.close();
                    }
                    if (os != null) {
                        os.flush();
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 将excel保存到本地
     *
     * @param wb
     */
    public static void saveExcelToLocal(HSSFWorkbookDTO wb, long date) {
        File file = new File(SAVE_PATH + "\\" + date);
        if (!file.exists()) {
            file.mkdirs();
        }
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(new File(file, wb.getExcelName() + "项目代码统计表-" + wb.getDefaultBranch() + "-" + dateFormat()
                    + ".xls"));
            wb.write(stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String dateFormat() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyy-MM-dd HHmmss");
        String nowTime = now.format(dtf);
        return nowTime;
    }
}
