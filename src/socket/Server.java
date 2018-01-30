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
    
    /** �˿�1��������Ҫ��Ϣ�Ķ˿� */
    private static final int PORT1 = 6666;
    
    /** �˿�2�������ļ��Ķ˿� */
    private static final int PORT2 = 6667;
    
    private static final String HOST = "127.0.0.1";
    
    /** ������Ҫ��Ϣ��socket��ַ */
    private InetSocketAddress communicateAddress = new InetSocketAddress(HOST, PORT1);
    
    /** �����ļ���socket��ַ */
    private InetSocketAddress loadFileAddress = new InetSocketAddress(HOST,PORT2);
    
    /** �������洢�ļ����ļ���·�� */
    private static String repositoryPath = "F:\\server_repository";
    
    /**
     * ��һ��ѡ������һ������ͨ��
     * ������Ӧ���ò��ѽ���ͨ���󶨵�ѡ������
     */
    
    public Server() throws IOException {
        selector = Selector.open(); 
        acceptor = ServerSocketChannel.open();
        acceptor.bind(communicateAddress);
        acceptor.configureBlocking(false);
        acceptor.register(selector, SelectionKey.OP_ACCEPT);
        
        FileUtil.mkdir(repositoryPath);
        repositoryPath += "\\";
        
        
        System.out.println("������������");
    }
	
    public void work() throws Exception {
        new AcceptFileChannelThread().start();
        /*
         * ���ղ�����ע�ᡢ��¼����
         */
        while (selector.select() > 0) {
            for (SelectionKey key : selector.selectedKeys()) {
                // �����ڴ����SelectionKey�Ӵ���������ɾ��
                selector.selectedKeys().remove(key);
                if (key.isAcceptable()) accept(key);
                if (key.isReadable()) {
                    SocketChannel mainChannel = (SocketChannel) key.channel();
                    String protocol = Translator.readProtocol(key);
                    // ���key�Ѿ�ʧЧ������������ѭ��
                    if (!key.isValid()) continue;
                    // ��δ����Э���ַ���˵��������δ֪���󣨰���˵�ǲ��ᷢ���ģ�
                    if (protocol == null) {
                        Translator.write(mainChannel, UNKNOWN_ERROR + PLACEHOLDER);
                        continue;
                    }
                    // ����ͻ��˷�����ע������
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
                        // ����ͻ��˷����˵�¼����
                    } else if (protocol.equals(LOGIN + PLACEHOLDER)) {
                        /*
                         *  temp�Ǹ����û�������û�����������ɵ���ʱ����
                         *  user�����ݿ�����ʵ���ڵĶ���
                         *  ���ҽ���user��temp���û������������ʱ��¼�ɹ�
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
     * ����򿪴����ļ�ͨ�����߳�
     * ÿ�δ�ͨ���������һ��LoadThread�����ļ�
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
     * �����ϴ������ص��߳�
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
        
        /** ���տͻ��˴�����ļ� */
        private void receive() throws IOException, InterruptedException {
            int uid = Translator.readInt(loadChannel);
            // fileֻ��name��format��������Ч��
            AFile file = Translator.readAFile(loadChannel);
            /*
             * �����ݿ��в���file����Ϣ
             * ������ڣ���temp��file��������Ϣ�棨temp��id��path������Ч�ģ�
             * ����temp = null
             */
            AFile temp = getAFile(file.getName(), file.getFormat());
            
            // �������˲����ڸ��ļ�������ո��ļ�
            if (temp == null) {
                // ���߿ͻ��˷���˲�����׼���ϴ����ļ����ͻ����յ�����Ϣ�����ļ�������
                Translator.write(loadChannel, FILE_NOT_FOUND + PLACEHOLDER);
                /*
                 * ׼��д��
                 * ����һ�����ļ�����������ڣ�
                 * ����һ���ļ�ͨ���򿪸ÿ��ļ�
                 */
                String path = repositoryPath + file;
                FileUtil.createNewFile(path);
                @SuppressWarnings("resource")
                FileChannel outChannel = new FileOutputStream(path).getChannel();
                /*
                 * ��ȡͨ���ڵ��ļ��ֽڻ��岢����д�����ļ���
                 * ��ÿ���ȡ���ֽ�����д�뵽ͨ����
                 */
                ByteBuffer buf = ByteBuffer.allocate(131071);// 131071��һ���ܹ���ȡ������ֽ��������Եó���
                int hasRead;
                // ÿ�루���ƣ���ȡ���ֽ���
                long readPerSecond = 0L;
                // ��¼һ��Ŀ�ʼ
                long start = System.currentTimeMillis();
                while ((hasRead = loadChannel.read(buf)) != -1) {
                    buf.flip();// flip before write!
                    outChannel.write(buf);
                    buf.clear();// clear before read!
                    readPerSecond += hasRead;
                    // ��¼һ��Ľ���
                    long end = System.currentTimeMillis();
                    /*
                     * �������һ�εļ�¼��������һ��
                     * �Ͱ��ڱ��μ���ж�ȡ���ֽ�����д��ͨ����
                     */
                    if (end - start > 1000 ) {
                        start = end;
                        Translator.write(loadChannel, readPerSecond);
                        readPerSecond = 0L;
                    }
                }
                // д��δд����ֽ�������Ϣ��ʱ����С��һ�뵫whileѭ���Ѿ�������
                if (readPerSecond != 0L) {
                    Translator.write(loadChannel, readPerSecond);
                }
                outChannel.close();
                /*
                 * �������ݿ�
                 */
                int fid = addAFile(file.getName(), file.getFormat(), path);
                if (addRelation(uid, fid)) {
                    Translator.write(loadChannel, UPLOAD + SUCCESS);
                    /*
                     * �Ѹ�����Ϣ����û���������ͻ���
                     */
                    User user = getUser(uid);
                    Translator.write(loadChannel, user);
                } else {
                    Translator.write(loadChannel, UNKNOWN_ERROR);
                }
            } else {
                /*
                 * ��������ݿ����ҵ��˸��ļ�����Ϣ ���봫
                 */
                file = temp;
                temp = null;
                if (addRelation(uid, file.getId())) {
                    Translator.write(loadChannel, UPLOADED_IN_A_SECOND + PLACEHOLDER);
                    /*
                     * �Ѹ�����Ϣ����û���������ͻ���
                     */
                    User user = getUser(uid);
                    Translator.write(loadChannel, user);
                } else {
                    Translator.write(loadChannel, UNKNOWN_ERROR);
                }
            }
            System.out.println("��������" + loadChannel.getRemoteAddress() + "���ļ���ɣ�");
        }
        
        /** ���ļ�������ͻ��� */
        private void deliver() throws IOException {
            AFile file = Translator.readAFile(loadChannel);
            // �����ݿ��л�ȡ���ļ���������Ϣ
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
    
    /** ����һ���µ�SocketChannel���Ұ���ע�ᵽselector�� */
    private void accept(SelectionKey sk) throws Exception {
        SocketChannel sc = acceptor.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
        // ׼����һ�ν���
        sk.interestOps(SelectionKey.OP_ACCEPT);
        System.out.println("������һ���ͻ���" + sc.getRemoteAddress());
    }    
    
    /** �����ݿ��в����û��������ڷ���User���󣬷��򷵻�null */
    private User getUser(String username) {
        String sql = "SELECT * FROM user WHERE name = '" + username + "';";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet userRS = pstmt.executeQuery();
            if (userRS.next()) {
                int id = userRS.getInt("id");
                String password = userRS.getString("password");
                User user = new User(id, username, password);
                
                // ��ȡ���û����е��ļ�
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
                
                // ��ȡ���û����е��ļ�
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
    
    /** �����ݿ��м���û����������Ƿ�ƥ�䣬ƥ�䷵��true�����򷵻�false */
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
    
    /** �����ݿ���������û�����ӳɹ�����true�����򷵻�false */
    private boolean addUser(String username, String password) {
        String sql = "INSERT INTO user VALUES(null, ?, ?);";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            // setXxx�ĵ�һ������ָ���ǵ�һ���ʺ�
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
    
    /** �����ݿ���Ѱ���ļ����ҵ��򷵻ظ��µ��ļ�����ӵ��id��path��Ϣ�������򷵻�null */
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
    
    /** �����ݿ�������û�����ĳ�ļ�����Ϣ���ɹ�����true�����򷵻�false */
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
    
    /** �����ݿ�������µ��ļ���Ϣ���ɹ��������ļ���id�����򷵻�-1 */
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
                // ��ʹ֪�����Ҳ����next()������ֱ��getXxx()
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
