package util;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

	/**
	 * ���ݿ⹤����
	 * �ں�������̬�����ֱ����ڳ�ʼ���Լ��ر����ݿ�����
	 */
	public class DBUtil {
	    
	    private static String driver;
	    private static String url;
	    private static String user;
	    private static String password;
	    
	    public static Connection conn;
	    
	    /** �������ݿ�����������һ��ָ�����ݿ������ */
	    public static void init() throws Exception {
	        Properties prop = new Properties();
	        /*
	         * ���������ļ�����ȡ������ֵ
	         */
	        prop.load(new FileInputStream("mysql.ini"));
	        driver = prop.getProperty("driver");
	        url = prop.getProperty("url");
	        user = prop.getProperty("user");
	        password = prop.getProperty("password");
	        // ��������
	        Class.forName(driver);
	        // ��ȡָ�����ݿ������
	        conn = DriverManager.getConnection(url, user, password);
	    }
	    
	    /** �ر����ݿ����� */
	    public static void close() {
	        try {
	            conn.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
}
