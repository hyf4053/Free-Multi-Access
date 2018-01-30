package socket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Scanner;

import entity.AFile;
import entity.User;

import service.Translator;
import util.FileUtil;

import static service.NetDiskProtocol.*;


public class Client {
	
	/**将main channel和这个selector绑定，服务器传回消息时选择main channel并做出相应操作*/
	private Selector selector = null;
	
	/** main channel，用来传输和接收一些信息量较小的主要信息，如协议字符串、登录、注册信息等 */
	private SocketChannel mainChannel = null;
	
	/** port1，传输主要信息的端口 */
    private static final int PORT1 = 6666;
    
    /** port2，传输文件的端口 */
    private static final int PORT2 = 6667;
    
    /** 保留地址 */
    private static final String HOST = "127.0.0.1";
    
    /** 传输主要信息的socket地址 */
    private InetSocketAddress communicateAddress = new InetSocketAddress(HOST, PORT1);
    
    /** 传输文件的socket地址 */
    private InetSocketAddress loadFileAddress = new InetSocketAddress(HOST,PORT2);
    
    private int command;
    /** 此为控制台接受用户指令 */
    private Scanner console = new Scanner(System.in);
    
    private boolean loginSuccessfully = false;
    
    /** 当前客户端在线用户 */
    private User currentUser = null;
    
    /** 客户端存储文件的地址 */
    private static String repositoryPath = "F:\\client_repository";
    
    /**打开selector和main channel，进行设置 并将main channel绑定至selector上面*/
    public Client() throws IOException {
    	selector = Selector.open();//打开Selector
    	mainChannel = SocketChannel.open(communicateAddress);//打开main channel并选择地址
    	mainChannel.configureBlocking(false);//配置Channel的block属性
    	mainChannel.register(selector, SelectionKey.OP_READ);//将main channel注册到selector上，并赋予操作属性
    	
    	FileUtil.mkdir(repositoryPath);//创建目录
    	repositoryPath += "\\";
    }
    /**
     * 这一块为命令行操作 之后需要更新到GUI上
     */
    private void readCommand() {
        while (true) {
            try {
                command = console.nextInt();
                // 跳过换行符
                console.nextLine();
                return;
            } catch (Exception e) {
                System.out.println("请输入数字！");
                // 跳过错误输入
                console.next();
            }
        }
    }
    
    /** 第一层操作，包括注册、登录、退出 */
    //
    private void FirstLayer() throws Exception {
        new ClientThread().start();
        while (true) {
            FirstLayerMenu();
            readCommand();
//            command = 1;
            switch (command) {
            case 1:
                login();
                break;
            case 2:
                signUp();
                break;
            case 3:
                System.out.println("再见！");
                return;
            }
        }
    }
    
    private void signUp() throws InterruptedException {
        System.out.println("请输入用户名：");
        String username = console.next();// 警告：用nextline方法会使得username = 上一次输完命令遗留下来的换行符
        System.out.println("请输入密码：");
        String password = console.next();
        User newUser = new User(username, password);
        String protocol = SIGNUP + PLACEHOLDER;
        Translator.write(mainChannel, protocol, newUser);
        Thread.sleep(1000);
    }
    
    private void login() throws InterruptedException {
        System.out.println("请输入用户名：");
        String username = console.next();
//        String username = "Jack";
        System.out.println("请输入密码：");
        String password = console.next();
//        String password = "123";
        User tempUser = new User(username, password);
        String protocol = LOGIN + PLACEHOLDER;
        Translator.write(mainChannel, protocol, tempUser);
        Thread.sleep(1000);
        if (loginSuccessfully) {
            // 登录成功后进入下一层页面
            SecondLayer();
        } else {
            currentUser = null;
        }
    }
    
    /** 第二层操作，包括上传、下载、登出，还可以看到当前用户的文件 */
    private void SecondLayer() {
        while (true) {
            SecondLayerMenu();
            readCommand();
//            command = 1;
            switch (command) {
            case 1:
                try {
                    upload();
                } catch (Exception e) {
                    System.err.println("上传时抛出异常！");
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    download();
                } catch (Exception e) {
                    System.err.println("下载时抛出异常！");
                    e.printStackTrace();
                }
                break;
            case 3:
                loginSuccessfully = false;
                currentUser = null;
                return;
            }
        }
    }
    
    private void upload() throws Exception {
        /*
         * 不断输入文件的路径，直到得到可用路径为止
         */
        AFile file;
        String path;
        System.out.println("请输入要上传文件的路径");
        while (true) {
            try {
                path = console.nextLine();
                file = new AFile(path);
                break;
            } catch (FileNotFoundException e) {
                System.out.println("找不到该文件，请重新输入");
            }
        }
        /*
         * 打开一个新的上传通道并将其设为阻塞的
         */
        SocketChannel uploadChannel = SocketChannel.open(loadFileAddress);
        uploadChannel.configureBlocking(true);
        /*
         * 把上传协议，用户id，文件对象通过上传通道传输给服务端
         */
        String protocol = UPLOAD + PLACEHOLDER;
        int id = currentUser.getId();
        Translator.write(uploadChannel, protocol, id, file);
        /*
         * 读取服务端的响应
         * 若服务端不存在要上传的文件，则开始上传工作
         * 若服务端存在要上传的文件，则秒传
         * 否则出现未知错误
         */
        protocol = Translator.readProtocol(uploadChannel);
        if (protocol == null) throw new Exception("未读取到协议！");
        if (protocol.equals(FILE_NOT_FOUND + PLACEHOLDER)) {
            @SuppressWarnings("resource")
            FileChannel inChannel = new FileInputStream(path).getChannel();
            MappedByteBuffer buf = inChannel.map(MapMode.READ_ONLY, 0, inChannel.size());
            /*
             *  启动一个显示进度的线程
             */
            GetCompleteRateThread rate = new GetCompleteRateThread(uploadChannel, inChannel.size());
            rate.start();
            while (buf.hasRemaining()) {
                uploadChannel.write(buf);
            }
            // 关闭输出，表明文件输入结束
            uploadChannel.shutdownOutput();
            synchronized (uploadChannel) {
                if (!rate.complete) {
                    uploadChannel.wait();
                }
            }
            protocol = Translator.readProtocol(uploadChannel);
            // 读取服务端的响应，看是否上传成功
            if (protocol.equals(UPLOAD + SUCCESS)) {
                System.out.println("上传成功！");
            } else if (protocol.equals(UNKNOWN_ERROR)) {
                throw new Exception("未知错误！");
            }
        } else if (protocol.equals(UPLOADED_IN_A_SECOND + PLACEHOLDER)) System.out.println("秒传！");
        else throw new Exception("未知错误！");
        currentUser = Translator.readUser(uploadChannel);
        uploadChannel.close();
        Thread.sleep(3000);
    }
    
    private void download() throws IOException, InterruptedException {
        /*
         * 生成要下载文件的文件对象
         * 如果文件不存在于该用户的文件列表中则会抛出异常
         */
        AFile file = null;
        while (true) {
            try {
                do {
                    System.out.println("请输入您要下载的文件名：");
                    String temp = console.next();
                    String[] fullName = AFile.split(temp);
                    file = new AFile(fullName[0], fullName[1]);
                } while (!currentUser.hasFile(file));
                break;
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }
        }
        // 客户端保存下载文件的路径
        String path = repositoryPath + file;
        /*
         * 创建同名空文件（如果不存在的话）
         * 获取该文件的文件通道
         */
        FileUtil.createNewFile(path);
        @SuppressWarnings("resource")
        FileChannel outChannel = new FileOutputStream(path).getChannel();
        /*
         * 打开一个SocketChannel
         * 设为阻塞模式
         * 传入协议和文件对象 
         */
        SocketChannel downloadChannel = SocketChannel.open(loadFileAddress);
        downloadChannel.configureBlocking(true);
        String protocol = DOWNLOAD + PLACEHOLDER;
        Translator.write(downloadChannel, protocol);
        Translator.write(downloadChannel, file);
        // 获取下载文件的大小
        Long size = Translator.readLong(downloadChannel);
        ByteBuffer buf = ByteBuffer.allocate(131071);
        // 每次从buf中读取的字节数
        int hasRead;
        // 每秒下载的字节数
        long readPerSecond = 0L;
        // 当前已经下载的大小
        long currentSize = 0L;
        // 一秒的开始
        Long start = System.currentTimeMillis();
        while ((hasRead = downloadChannel.read(buf)) != -1) {
            buf.flip();
            outChannel.write(buf);
            buf.clear();
            readPerSecond += hasRead;
            // 如果经过了一秒，更新当前数据并把完成度打印到控制台
            Long end = System.currentTimeMillis();
            if (end - start > 1000) {
                start = end;
                currentSize += readPerSecond;
                double rate = currentSize * 1.0 / size;
                System.out.println("已完成：" + String.format("%.1f", rate * 100) + "%");
                readPerSecond = 0L;
            }
        }
        // 如果还有遗留的字节数量信息
        if (readPerSecond != 0L) {
            currentSize += readPerSecond;
            double rate = currentSize * 1.0 / size;
            System.out.println("已完成：" + String.format("%.1f", rate * 100) + "%");
        }
        System.out.println("下载完成！");
        Thread.sleep(3000);
    }
    
    
    /**
     * 上传时获取上传进度的线程
     */
    private class GetCompleteRateThread extends Thread {
        private SocketChannel loadChannel = null;
        private Long fileSize;
        private Long currentSize = 0L;
        public boolean complete = false;
        
        public GetCompleteRateThread(SocketChannel loadChannel, Long fileSize) {
            this.loadChannel = loadChannel;
            this.fileSize = fileSize;
        }
        
        public void run() {
            while (getCompleteRate(currentSize, fileSize) < 1) {
                try {
                    currentSize += Translator.readLong(loadChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            synchronized (loadChannel) {
                complete = true;
                loadChannel.notify();
            }
        }
        
        private double getCompleteRate(Long currentSize, Long fileSize) {
            double rate = currentSize * 1.0 / fileSize;
            System.out.println("已完成：" + String.format("%.1f", rate * 100) + "%");
            return rate;
        }
    }
    
    /**
     * 客户端线程类
     * 负责读取服务器的部分响应
     * @author hyf40
     *
     */
    private class ClientThread extends Thread{
    	public void run() {
    		try {
    			while (selector.select() > 0) {
    				for (SelectionKey key : selector.selectedKeys()) {
    					selector.selectedKeys().remove(key);
    					if(key.isReadable()) {
    						String content = Translator.readProtocol(key);
    						if(content.equals(UNKNOWN_ERROR+PLACEHOLDER)) {
    							System.out.println("未知错误");
    						}else if(content.equals(SIGNUP+SUCCESS)){
    							System.out.println("注册成功");
    						}else if(content.equals(USER_EXIST+PLACEHOLDER)) {
    							System.out.println("用户名已存在");
    						}else if(content.equals(LOGIN+SUCCESS)) {
    							/*
                                 * 登录成功后，服务端会从数据库中提取用户信息，
                                 * 包装成用户对象传输给客户端
                                 */
    							System.out.println("登录成功");
    							loginSuccessfully = true;
    							// 读取服务端传输的用户对象，并把当前在线的用户设为该对象
                                currentUser = Translator.readUser(key);
    						}else if(content.equals(USER_NOT_FOUND+PLACEHOLDER)) {
    							System.out.println("找不到该用户");
    							loginSuccessfully = false;
    						}else if(content.equals(WRONG_PASSWORD+PLACEHOLDER)) {
    							System.out.println("密码错误");
    							loginSuccessfully = false;
    						}
    					}
    				}
    			}
    		}catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.FirstLayer();
    }
    
    private void FirstLayerMenu() {
        System.out.println("1. 登录");
        System.out.println("2. 注册");
        System.out.println("3. 退出");
    }
    
    private void SecondLayerMenu() {
        System.out.println("欢迎您，" + currentUser.getUsername());
        System.out.println("1. 上传");
        System.out.println("2. 下载");
        System.out.println("3. 登出");
        System.out.println("----------[文件列表]----------");
        currentUser.listFiles();
    }

}
