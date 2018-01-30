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
	
	/**��main channel�����selector�󶨣�������������Ϣʱѡ��main channel��������Ӧ����*/
	private Selector selector = null;
	
	/** main channel����������ͽ���һЩ��Ϣ����С����Ҫ��Ϣ����Э���ַ�������¼��ע����Ϣ�� */
	private SocketChannel mainChannel = null;
	
	/** port1��������Ҫ��Ϣ�Ķ˿� */
    private static final int PORT1 = 6666;
    
    /** port2�������ļ��Ķ˿� */
    private static final int PORT2 = 6667;
    
    /** ������ַ */
    private static final String HOST = "127.0.0.1";
    
    /** ������Ҫ��Ϣ��socket��ַ */
    private InetSocketAddress communicateAddress = new InetSocketAddress(HOST, PORT1);
    
    /** �����ļ���socket��ַ */
    private InetSocketAddress loadFileAddress = new InetSocketAddress(HOST,PORT2);
    
    private int command;
    /** ��Ϊ����̨�����û�ָ�� */
    private Scanner console = new Scanner(System.in);
    
    private boolean loginSuccessfully = false;
    
    /** ��ǰ�ͻ��������û� */
    private User currentUser = null;
    
    /** �ͻ��˴洢�ļ��ĵ�ַ */
    private static String repositoryPath = "F:\\client_repository";
    
    /**��selector��main channel���������� ����main channel����selector����*/
    public Client() throws IOException {
    	selector = Selector.open();//��Selector
    	mainChannel = SocketChannel.open(communicateAddress);//��main channel��ѡ���ַ
    	mainChannel.configureBlocking(false);//����Channel��block����
    	mainChannel.register(selector, SelectionKey.OP_READ);//��main channelע�ᵽselector�ϣ��������������
    	
    	FileUtil.mkdir(repositoryPath);//����Ŀ¼
    	repositoryPath += "\\";
    }
    /**
     * ��һ��Ϊ�����в��� ֮����Ҫ���µ�GUI��
     */
    private void readCommand() {
        while (true) {
            try {
                command = console.nextInt();
                // �������з�
                console.nextLine();
                return;
            } catch (Exception e) {
                System.out.println("���������֣�");
                // ������������
                console.next();
            }
        }
    }
    
    /** ��һ�����������ע�ᡢ��¼���˳� */
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
                System.out.println("�ټ���");
                return;
            }
        }
    }
    
    private void signUp() throws InterruptedException {
        System.out.println("�������û�����");
        String username = console.next();// ���棺��nextline������ʹ��username = ��һ�������������������Ļ��з�
        System.out.println("���������룺");
        String password = console.next();
        User newUser = new User(username, password);
        String protocol = SIGNUP + PLACEHOLDER;
        Translator.write(mainChannel, protocol, newUser);
        Thread.sleep(1000);
    }
    
    private void login() throws InterruptedException {
        System.out.println("�������û�����");
        String username = console.next();
//        String username = "Jack";
        System.out.println("���������룺");
        String password = console.next();
//        String password = "123";
        User tempUser = new User(username, password);
        String protocol = LOGIN + PLACEHOLDER;
        Translator.write(mainChannel, protocol, tempUser);
        Thread.sleep(1000);
        if (loginSuccessfully) {
            // ��¼�ɹ��������һ��ҳ��
            SecondLayer();
        } else {
            currentUser = null;
        }
    }
    
    /** �ڶ�������������ϴ������ء��ǳ��������Կ�����ǰ�û����ļ� */
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
                    System.err.println("�ϴ�ʱ�׳��쳣��");
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    download();
                } catch (Exception e) {
                    System.err.println("����ʱ�׳��쳣��");
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
         * ���������ļ���·����ֱ���õ�����·��Ϊֹ
         */
        AFile file;
        String path;
        System.out.println("������Ҫ�ϴ��ļ���·��");
        while (true) {
            try {
                path = console.nextLine();
                file = new AFile(path);
                break;
            } catch (FileNotFoundException e) {
                System.out.println("�Ҳ������ļ�������������");
            }
        }
        /*
         * ��һ���µ��ϴ�ͨ����������Ϊ������
         */
        SocketChannel uploadChannel = SocketChannel.open(loadFileAddress);
        uploadChannel.configureBlocking(true);
        /*
         * ���ϴ�Э�飬�û�id���ļ�����ͨ���ϴ�ͨ������������
         */
        String protocol = UPLOAD + PLACEHOLDER;
        int id = currentUser.getId();
        Translator.write(uploadChannel, protocol, id, file);
        /*
         * ��ȡ����˵���Ӧ
         * ������˲�����Ҫ�ϴ����ļ�����ʼ�ϴ�����
         * ������˴���Ҫ�ϴ����ļ������봫
         * �������δ֪����
         */
        protocol = Translator.readProtocol(uploadChannel);
        if (protocol == null) throw new Exception("δ��ȡ��Э�飡");
        if (protocol.equals(FILE_NOT_FOUND + PLACEHOLDER)) {
            @SuppressWarnings("resource")
            FileChannel inChannel = new FileInputStream(path).getChannel();
            MappedByteBuffer buf = inChannel.map(MapMode.READ_ONLY, 0, inChannel.size());
            /*
             *  ����һ����ʾ���ȵ��߳�
             */
            GetCompleteRateThread rate = new GetCompleteRateThread(uploadChannel, inChannel.size());
            rate.start();
            while (buf.hasRemaining()) {
                uploadChannel.write(buf);
            }
            // �ر�����������ļ��������
            uploadChannel.shutdownOutput();
            synchronized (uploadChannel) {
                if (!rate.complete) {
                    uploadChannel.wait();
                }
            }
            protocol = Translator.readProtocol(uploadChannel);
            // ��ȡ����˵���Ӧ�����Ƿ��ϴ��ɹ�
            if (protocol.equals(UPLOAD + SUCCESS)) {
                System.out.println("�ϴ��ɹ���");
            } else if (protocol.equals(UNKNOWN_ERROR)) {
                throw new Exception("δ֪����");
            }
        } else if (protocol.equals(UPLOADED_IN_A_SECOND + PLACEHOLDER)) System.out.println("�봫��");
        else throw new Exception("δ֪����");
        currentUser = Translator.readUser(uploadChannel);
        uploadChannel.close();
        Thread.sleep(3000);
    }
    
    private void download() throws IOException, InterruptedException {
        /*
         * ����Ҫ�����ļ����ļ�����
         * ����ļ��������ڸ��û����ļ��б�������׳��쳣
         */
        AFile file = null;
        while (true) {
            try {
                do {
                    System.out.println("��������Ҫ���ص��ļ�����");
                    String temp = console.next();
                    String[] fullName = AFile.split(temp);
                    file = new AFile(fullName[0], fullName[1]);
                } while (!currentUser.hasFile(file));
                break;
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }
        }
        // �ͻ��˱��������ļ���·��
        String path = repositoryPath + file;
        /*
         * ����ͬ�����ļ�����������ڵĻ���
         * ��ȡ���ļ����ļ�ͨ��
         */
        FileUtil.createNewFile(path);
        @SuppressWarnings("resource")
        FileChannel outChannel = new FileOutputStream(path).getChannel();
        /*
         * ��һ��SocketChannel
         * ��Ϊ����ģʽ
         * ����Э����ļ����� 
         */
        SocketChannel downloadChannel = SocketChannel.open(loadFileAddress);
        downloadChannel.configureBlocking(true);
        String protocol = DOWNLOAD + PLACEHOLDER;
        Translator.write(downloadChannel, protocol);
        Translator.write(downloadChannel, file);
        // ��ȡ�����ļ��Ĵ�С
        Long size = Translator.readLong(downloadChannel);
        ByteBuffer buf = ByteBuffer.allocate(131071);
        // ÿ�δ�buf�ж�ȡ���ֽ���
        int hasRead;
        // ÿ�����ص��ֽ���
        long readPerSecond = 0L;
        // ��ǰ�Ѿ����صĴ�С
        long currentSize = 0L;
        // һ��Ŀ�ʼ
        Long start = System.currentTimeMillis();
        while ((hasRead = downloadChannel.read(buf)) != -1) {
            buf.flip();
            outChannel.write(buf);
            buf.clear();
            readPerSecond += hasRead;
            // ���������һ�룬���µ�ǰ���ݲ�����ɶȴ�ӡ������̨
            Long end = System.currentTimeMillis();
            if (end - start > 1000) {
                start = end;
                currentSize += readPerSecond;
                double rate = currentSize * 1.0 / size;
                System.out.println("����ɣ�" + String.format("%.1f", rate * 100) + "%");
                readPerSecond = 0L;
            }
        }
        // ��������������ֽ�������Ϣ
        if (readPerSecond != 0L) {
            currentSize += readPerSecond;
            double rate = currentSize * 1.0 / size;
            System.out.println("����ɣ�" + String.format("%.1f", rate * 100) + "%");
        }
        System.out.println("������ɣ�");
        Thread.sleep(3000);
    }
    
    
    /**
     * �ϴ�ʱ��ȡ�ϴ����ȵ��߳�
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
            System.out.println("����ɣ�" + String.format("%.1f", rate * 100) + "%");
            return rate;
        }
    }
    
    /**
     * �ͻ����߳���
     * �����ȡ�������Ĳ�����Ӧ
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
    							System.out.println("δ֪����");
    						}else if(content.equals(SIGNUP+SUCCESS)){
    							System.out.println("ע��ɹ�");
    						}else if(content.equals(USER_EXIST+PLACEHOLDER)) {
    							System.out.println("�û����Ѵ���");
    						}else if(content.equals(LOGIN+SUCCESS)) {
    							/*
                                 * ��¼�ɹ��󣬷���˻�����ݿ�����ȡ�û���Ϣ��
                                 * ��װ���û���������ͻ���
                                 */
    							System.out.println("��¼�ɹ�");
    							loginSuccessfully = true;
    							// ��ȡ����˴�����û����󣬲��ѵ�ǰ���ߵ��û���Ϊ�ö���
                                currentUser = Translator.readUser(key);
    						}else if(content.equals(USER_NOT_FOUND+PLACEHOLDER)) {
    							System.out.println("�Ҳ������û�");
    							loginSuccessfully = false;
    						}else if(content.equals(WRONG_PASSWORD+PLACEHOLDER)) {
    							System.out.println("�������");
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
        System.out.println("1. ��¼");
        System.out.println("2. ע��");
        System.out.println("3. �˳�");
    }
    
    private void SecondLayerMenu() {
        System.out.println("��ӭ����" + currentUser.getUsername());
        System.out.println("1. �ϴ�");
        System.out.println("2. ����");
        System.out.println("3. �ǳ�");
        System.out.println("----------[�ļ��б�]----------");
        currentUser.listFiles();
    }

}
