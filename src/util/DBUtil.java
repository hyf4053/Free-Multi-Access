package util;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

	/**
	 * 数据库工具类
	 * 内含两个静态方法分别用于初始化以及关闭数据库连接
	 */
	public class DBUtil {
	    
	    private static String driver;
	    private static String url;
	    private static String user;
	    private static String password;
	    
	    public static Connection conn;
	    
	    /** 加载数据库驱动并创建一个指定数据库的连接 */
	    public static void init() throws Exception {
	        Properties prop = new Properties();
	        /*
	         * 加载配置文件并获取各属性值
	         */
	        prop.load(new FileInputStream("mysql.ini"));
	        driver = prop.getProperty("driver");
	        url = prop.getProperty("url");
	        user = prop.getProperty("user");
	        password = prop.getProperty("password");
	        // 加载驱动
	        Class.forName(driver);
	        // 获取指定数据库的连接
	        conn = DriverManager.getConnection(url, user, password);
	    }
	    
	    /** 关闭数据库连接 */
	    public static void close() {
	        try {
	            conn.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
}
