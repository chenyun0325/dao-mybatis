package cn.gov.zcy.platform.dao.mybatis3.multidatasource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenyun on 2017/4/21.
 */
public class DynamicDataSourceContextHolder {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();

    private static List<String> dsCol = new ArrayList<String>();//初始化使用

    public static void setDs(String dsName){
        contextHolder.set(dsName);
    }

    public static String getDs(){
       return contextHolder.get();
    }

    public static void clearDs(){
        contextHolder.remove();
    }

    public static boolean contain(String ds){
       return dsCol.contains(ds);
    }

    public static void addDsCol(String dsName){
        dsCol.add(dsName);
    }
}
