package socket;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.FileChannel.MapMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import entity.AFile;
import entity.User;

import service.Translator;
import util.FileUtil;
import static service.NetDiskProtocol.*;
import static util.DBUtil.*;
import util.DBUtil;

public class Server {
	
	private Selector selector = null;
    private ServerSocketChannel acceptor = null;
    
    /** 端口1，传输主要信息的端口 */
    private static final int PORT1 = 6666;
    
    /** 端口2，传输文件的端口 */
    private static final int PORT2 = 6667;
    
    private static final String HOST = "127.0.0.1";
    
    /** 传输主要信息的socket地址 */
    private InetSocketAddress communicateAddress = new InetSocketAddress(HOST, PORT1);
    
    /** 传输文件的socket地址 */
    private InetSocketAddress loadFileAddress = new InetSocketAddress(HOST,PORT2);
    
    /** 服务器存储文件的文件夹路径 */
    private static String repositoryPath = "F:\\server_repository";
    
    /**
     * 打开一个选择器和一个接收通道
     * 进行相应设置并把接收通道绑定到选择器上
     */
    
    public Server() throws IOException {
        selector = Selector.open(); 
        acceptor = ServerSocketChannel.open();
        acceptor.bind(communicateAddress);
        acceptor.configureBlocking(false);
        acceptor.register(selector, SelectionKey.OP_ACCEPT);
        
        FileUtil.mkdir(repositoryPath);
        repositoryPath += "\\";
        
        
        System.out.println("服务器已启动");
    }
	
    public void work() throws Exception {
        new AcceptFileChannelThread().start();
        /*
         * 接收并处理注册、登录请求
         */
        while (selector.select() > 0) {
            for (SelectionKey key : selector.selectedKeys()) {
                // 把正在处理的SelectionKey从待处理集合中删除
                selector.selectedKeys().remove(key);
                if (key.isAcceptable()) accept(key);
                if (key.isReadable()) {
                    SocketChannel mainChannel = (SocketChannel) key.channel();
                    String protocol = Translator.readProtocol(key);
                    // 如果key已经失效，则跳过本次循环
                    if (!key.isValid()) continue;
                    // 若未读到协议字符则说明出现了未知错误（按理说是不会发生的）
                    if (protocol == null) {
                        Translator.write(mainChannel, UNKNOWN_ERROR + PLACEHOLDER);
                        continue;
                    }
                    // 如果客户端发出了注册请求
                    if (protocol.equals(SIGNUP + PLACEHOLDER)) {
                        User newUser = Translator.readUser(key);
                        String username = newUser.getUsername();
                        User temp = getUser(username);
                        if (temp == null) {
                            String password = newUser.getPassword();
                            if (addUser(username, password)) {
                                Translator.write(mainChannel, SIGNUP + SUCCESS);
                            } else {
                                Translator.write(mainChannel, UNKNOWN_ERROR + PLACEHOLDER);
                            }
                        } else {
                            Translator.write(mainChannel, USER_EXIST + PLACEHOLDER);
                        }
                        // 如果客户端发出了登录请求
                    } else if (protocol.equals(LOGIN + PLACEHOLDER)) {
                        /*
                         *  temp是根据用户输入的用户名、密码组成的临时对象
                         *  user是数据库中真实存在的对象
                         *  当且仅当user和temp的用户名、密码相等时登录成功
                         */
                        User temp = Translator.readUser(key);
                        String username = temp.getUsername();
                        User user = getUser(username);
                        if (user != null) {
                            String password = temp.getPassword();
                            if (checkPassword(username, password)) {
                                Translator.write(mainChannel, LOGIN + SUCCESS, user);
                            } else {
                                Translator.write(mainChannel, WRONG_PASSWORD + PLACEHOLDER);
                            }
                        } else {
                            Translator.write(mainChannel, USER_NOT_FOUND + PLACEHOLDER);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 负责打开传输文件通道的线程
     * 每次打开通道后会启动一个LoadThread传输文件
     */
    private class AcceptFileChannelThread extends Thread {
        
        private Selector selector = null;
        private ServerSocketChannel acceptor = null;
        
        public AcceptFileChannelThread() throws IOException {
            selector = Selector.open();
            acceptor = ServerSocketChannel.open();
            acceptor.bind(loadFileAddress);
            acceptor.configureBlocking(false);
            acceptor.register(selector, SelectionKey.OP_ACCEPT);
        }
        
        public void run() {
            try {
                while (selector.select() > 0) {
                    for (SelectionKey key : selector.selectedKeys()) {
                        selector.selectedKeys().remove(key);
                        if (key.isAcceptable()) {
                            SocketChannel newChannel = acceptor.accept();
                            newChannel.configureBlocking(true);
                            key.interestOps(SelectionKey.OP_ACCEPT);
                            new LoadThread(newChannel).start();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 负责上传、下载的线程
     */
    private class LoadThread extends Thread {
        
        private SocketChannel loadChannel = null;
        
        public LoadThread(SocketChannel loadChannel) {
            this.loadChannel = loadChannel;
        }
        
        public void run() {
            String protocol = "";
            try {
                protocol = Translator.readProtocol(loadChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
            switch (protocol) {
            case UPLOAD + PLACEHOLDER:
                try {
                    receive();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case DOWNLOAD + PLACEHOLDER:
                try {
                    deliver();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.err.println("unable to read protocol");
            }
        }
        
        /** 接收客户端传输的文件 */
        private void receive() throws IOException, InterruptedException {
            int uid = Translator.readInt(loadChannel);
            // file只有name和format的域是有效的
            AFile file = Translator.readAFile(loadChannel);
            /*
             * 在数据库中查找file的信息
             * 如果存在，则temp是file的完整信息版（temp的id和path域是有效的）
             * 否则temp = null
             */
            AFile temp = getAFile(file.getName(), file.getFormat());
            
            // 如果服务端不存在该文件，则接收该文件
            if (temp == null) {
                // 告诉客户端服务端不存在准备上传的文件，客户端收到该信息后会把文件传过来
                Translator.write(loadChannel, FILE_NOT_FOUND + PLACEHOLDER);
                /*
                 * 准备写入
                 * 创建一个空文件（如果不存在）
                 * 并用一个文件通道打开该空文件
                 */
                String path = repositoryPath + file;
                FileUtil.createNewFile(path);
                @SuppressWarnings("resource")
                FileChannel outChannel = new FileOutputStream(path).getChannel();
                /*
                 * 读取通道内的文件字节缓冲并将其写入新文件中
                 * 把每秒读取的字节数量写入到通道中
                 */
                ByteBuffer buf = ByteBuffer.allocate(131071);// 131071是一次能够读取的最大字节数（测试得出）
                int hasRead;
                // 每秒（近似）读取的字节数
                long readPerSecond = 0L;
                // 记录一秒的开始
                long start = System.currentTimeMillis();
                while ((hasRead = loadChannel.read(buf)) != -1) {
                    buf.flip();// flip before write!
                    outChannel.write(buf);
                    buf.clear();// clear before read!
                    readPerSecond += hasRead;
                    // 记录一秒的结束
                    long end = System.currentTimeMillis();
                    /*
                     * 如果离上一次的记录点间隔大于一秒
                     * 就把在本次间隔中读取的字节数量写入通道中
                     */
                    if (end - start > 1000 ) {
                        start = end;
                        Translator.write(loadChannel, readPerSecond);
                        readPerSecond = 0L;
                    }
                }
                // 写入未写入的字节数量信息（时间间隔小于一秒但while循环已经结束）
                if (readPerSecond != 0L) {
                    Translator.write(loadChannel, readPerSecond);
                }
                outChannel.close();
                /*
                 * 更新数据库
                 */
                int fid = addAFile(file.getName(), file.getFormat(), path);
                if (addRelation(uid, fid)) {
                    Translator.write(loadChannel, UPLOAD + SUCCESS);
                    /*
                     * 把更新信息后的用户对象传输给客户端
                     */
                    User user = getUser(uid);
                    Translator.write(loadChannel, user);
                } else {
                    Translator.write(loadChannel, UNKNOWN_ERROR);
                }
            } else {
                /*
                 * 如果在数据库中找到了该文件的信息 则秒传
                 */
                file = temp;
                temp = null;
                if (addRelation(uid, file.getId())) {
                    Translator.write(loadChannel, UPLOADED_IN_A_SECOND + PLACEHOLDER);
                    /*
                     * 把更新信息后的用户对象传输给客户端
                     */
                    User user = getUser(uid);
                    Translator.write(loadChannel, user);
                } else {
                    Translator.write(loadChannel, UNKNOWN_ERROR);
                }
            }
            System.out.println("接收来自" + loadChannel.getRemoteAddress() + "的文件完成！");
        }
        
        /** 把文件传输给客户端 */
        private void deliver() throws IOException {
            AFile file = Translator.readAFile(loadChannel);
            // 从数据库中获取该文件的完整信息
            file = getAFile(file.getName(), file.getFormat());
            @SuppressWarnings("resource")
            FileChannel inChannel = new FileInputStream(file.getPath()).getChannel();
            Translator.write(loadChannel, inChannel.size());
            MappedByteBuffer buf = inChannel.map(MapMode.READ_ONLY, 0, inChannel.size());
            while (buf.hasRemaining()) {
                loadChannel.write(buf);
            }
            loadChannel.shutdownOutput();
        }
    }
    
    /** 接收一个新的SocketChannel并且把它注册到selector上 */
    private void accept(SelectionKey sk) throws Exception {
        SocketChannel sc = acceptor.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
        // 准备下一次接收
        sk.interestOps(SelectionKey.OP_ACCEPT);
        System.out.println("连接了一个客户端" + sc.getRemoteAddress());
    }    
    
    /** 在数据库中查找用户名，存在返回User对象，否则返回null */
    private User getUser(String username) {
        String sql = "SELECT * FROM user WHERE name = '" + username + "';";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet userRS = pstmt.executeQuery();
            if (userRS.next()) {
                int id = userRS.getInt("id");
                String password = userRS.getString("password");
                User user = new User(id, username, password);
                
                // 获取该用户所有的文件
                String getFilesSQL = "SELECT name, format FROM file INNER JOIN relation ON fid = id AND uid = " + id;
                pstmt = conn.prepareStatement(getFilesSQL);
                ResultSet fileRS = pstmt.executeQuery();
                while (fileRS.next()) {
                    String filename = fileRS.getString("name");
                    String format = fileRS.getString("format");
                    AFile file = new AFile(filename, format);
                    user.addFile(file);
                }
                return user;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private User getUser(int id) {
        String sql = "SELECT * FROM user WHERE id = " + id;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet userRS = pstmt.executeQuery();
            if (userRS.next()) {
                String name = userRS.getString("name");
                String password = userRS.getString("password");
                User user = new User(id, name, password);
                
                // 获取该用户所有的文件
                String getFilesSQL = "SELECT name, format FROM file INNER JOIN relation ON fid = id AND uid = " + id;
                pstmt = conn.prepareStatement(getFilesSQL);
                ResultSet fileRS = pstmt.executeQuery();
                while (fileRS.next()) {
                    String filename = fileRS.getString("name");
                    String format = fileRS.getString("format");
                    AFile file = new AFile(filename, format);
                    user.addFile(file);
                }
                return user;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /** 在数据库中检查用户名与密码是否匹配，匹配返回true，否则返回false */
    private boolean checkPassword(String username, String password) {
        String sql = "SELECT * FROM user WHERE name = '" + username + "' AND password = '" + password + "';";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /** 在数据库中添加新用户，添加成功返回true，否则返回false */
    private boolean addUser(String username, String password) {
        String sql = "INSERT INTO user VALUES(null, ?, ?);";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            // setXxx的第一个参数指的是第一个问号
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            if (pstmt.executeUpdate() == 1) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /** 在数据库中寻找文件，找到则返回该新的文件对象（拥有id和path信息），否则返回null */
    private AFile getAFile(String name, String format) {
        String sql = "SELECT * FROM file WHERE name = '" + name + "' AND format = '" + format + "';";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                String path = rs.getString(4);
                return new AFile(id, name, format, path);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /** 在数据库中添加用户持有某文件的信息，成功返回true，否则返回false */
    private boolean addRelation(int uid, int fid) {
        String sql = "INSERT IGNORE INTO relation VALUES(?, ?);";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, uid);
            pstmt.setInt(2, fid);
            if (pstmt.executeUpdate() == 1) return true;
            else return false;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /** 在数据库中添加新的文件信息，成功返回新文件的id，否则返回-1 */
    private int addAFile(String name, String format, String path) {
        String sql = "INSERT INTO file VALUES(NULL, ?, ?, ?);";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, format);
            pstmt.setString(3, path);
            if (pstmt.executeUpdate() == 1) {
                sql = "SELECT LAST_INSERT_ID();";
                pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();
                // 即使知道结果也必须next()而不能直接getXxx()
                while (rs.next()) {
                    int lastInsertID = rs.getInt(1);
                    return lastInsertID;
                }
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static void main(String[] args) throws Exception {
        Server server = new Server();
        DBUtil.init();
        server.work();
        DBUtil.close();
    }
}
