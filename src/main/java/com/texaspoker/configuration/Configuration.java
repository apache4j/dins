package com.texaspoker.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * 读取properties文件
 * @author Qutr
 *
 */
public class Configuration
{
    private Properties propertie;
    private FileInputStream inputFile;
    
    /**
     * 初始化Configuration类
     */
    public Configuration()
    {
        propertie = new Properties();
    }
    
    /**
     * 初始化Configuration类
     * @param filePath 要读取的配置文件的路径+名称
     */
    public Configuration(String filePath)
    {
        propertie = new Properties();
        try {
            inputFile = new FileInputStream(filePath);
            propertie.load(inputFile);
            inputFile.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }//end ReadConfigInfo(...)
    
    /**
     * 重载函数，得到key的值
     * @param key 取得其值的键
     * @return key的值
     */
    public String getValue(String key)
    {
        if(propertie.containsKey(key)){
            String value = propertie.getProperty(key);//得到某一属性的值
            return value;
        }
        else 
            return "";
    }//end getValue(...)
    
    /**
     * 重载函数，得到key的值
     * @param fileName properties文件的路径+文件名
     * @param key 取得其值的键
     * @return key的值
     */
    public String getValue(String fileName, String key)
    {
        try {
            String value = "";
            inputFile = new FileInputStream(fileName);
            propertie.load(inputFile);
            inputFile.close();
            if(propertie.containsKey(key)){
                value = propertie.getProperty(key);
                return value;
            }else
                return value;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }//end getValue(...)
    
    /**
     * 清除properties文件中所有的key和其值
     */
    public void clear()
    {
        propertie.clear();
    }
}