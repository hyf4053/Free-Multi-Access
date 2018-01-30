package service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import entity.AFile;
import entity.User;
import util.SerializationHelper;

/**
 * ������
 * ���ڷ������ͻ��˶�д��Ϣ
 */
public class Translator {
    
    /** ��������������ַ������� */
    private static Charset coder = Charset.forName("GBK");
    
    public static String readProtocol(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            // ׼����һ�ζ�ȡ
            key.interestOps(SelectionKey.OP_READ);
            return readProtocol(channel);
        } catch (IOException e) {
            /*
             * ��������쳣
             * ˵���ͻ��˳������⣨�����Ѿ��رգ�
             * ��˰Ѹ�SelectionKeyȡ��ע��
             */
            System.out.println("cancel a key when readProtocol");
            cancel(key);
        }
        return null;
    }
    
    public static String readProtocol(SocketChannel channel) throws IOException {
        // һ��Э���ַ�2���ֽڣ����Ϊbuf����4�ֽڵ�����
        ByteBuffer buf = ByteBuffer.allocate(4);
        String protocol = "";
        channel.read(buf);
        buf.flip();
        protocol += coder.decode(buf);
        return protocol;
    }
    
    public static User readUser(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            key.interestOps(SelectionKey.OP_READ);
            return readUser(channel);
        } catch (IOException e) {
            System.out.println("cancel a key when readUser");
            cancel(key);
        }
        return null;
    }
    
    public static User readUser(SocketChannel channel) throws IOException {
        /*
         * �ȶ�ȡ�������еĳ���
         * ΪByteBuffer����ó��ȵĿռ�
         * Ȼ���ٶ�ȡ��������
         */
        int length = readInt(channel);
        ByteBuffer userBuf = ByteBuffer.allocate(length);
        channel.read(userBuf);
        userBuf.flip();
        byte[] userBytes = userBuf.array();
        User user = (User) SerializationHelper.bytesToObject(userBytes);
        return user;
    }
    
    public static int readInt(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            key.interestOps(SelectionKey.OP_READ);
            return readInt(channel);
        } catch (IOException e) {
            System.out.println("cancel a key when readInt");
            cancel(key);
        }
        return -1;
    }
    
    public static int readInt(SocketChannel channel) throws IOException {
        // һ��int 4���ֽڣ����Ϊbuf����4�ֽڵĿռ�
        ByteBuffer buf = ByteBuffer.allocate(4);
        channel.read(buf);
        buf.flip();
        int result = buf.getInt();
        return result;
    }
    
    public static AFile readAFile(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            key.interestOps(SelectionKey.OP_READ);
            return readAFile(channel);
        } catch (IOException e) {
            System.out.println("cancel a key when readAFile");
            cancel(key);
        }
        return null;
    }
    
    public static AFile readAFile(SocketChannel channel) throws IOException {
        int length = readInt(channel);
        ByteBuffer fileBuf = ByteBuffer.allocate(length);
        channel.read(fileBuf);
        fileBuf.flip();
        byte[] fileBytes = fileBuf.array();
        AFile file = (AFile) SerializationHelper.bytesToObject(fileBytes);
        return file;
    }
    
    public static long readLong(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            key.interestOps(SelectionKey.OP_READ);
            return readLong(channel);
        } catch (IOException e) {
            System.out.println("cancel a key when readLong");
            cancel(key);
        }
        return -1;
    }
    
    public static long readLong(SocketChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8);
        channel.read(buf);
        buf.flip();
        long result = buf.getLong();
        return result;
    }

    /**
     * �򵥹���ģʽ����һ������������������д��ͨ����
     * @param channel д���ͨ��
     * @param o ����������������
     */
    public static void write(SocketChannel channel, Object o) {
        String typeName = o.getClass().getSimpleName();
        byte[] bytes = null;
        switch (typeName) {
        case "Long":
        case "Integer": 
            bytes = SerializationHelper.dataTypeTobytesFactory(o);
            break;
        case "User":
        case "AFile":
            byte[] objectBytes = SerializationHelper.objectToBytes(o);
            byte[] lengthBytes = SerializationHelper.dataTypeTobytesFactory(objectBytes.length);
            bytes = SerializationHelper.joint(lengthBytes, objectBytes);
            break;
        case "byte[]":
            bytes = (byte[]) o;
            break;
        case "String":
            String message = (String) o;
            bytes = message.getBytes();// ʹ��Ĭ�ϱ���
            break;
        default:
            System.err.println("δ�ҵ������������");
            return;
        }
        try {
            channel.write(ByteBuffer.wrap(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void write(SocketChannel channel, String protocol, User user) {
        byte[] protocolBytes = protocol.getBytes();
        byte[] userBytes = SerializationHelper.objectToBytes(user);
        byte[] lengthBytes = SerializationHelper.dataTypeTobytesFactory(userBytes.length);
        byte[] bytes = SerializationHelper.joint(protocolBytes, lengthBytes, userBytes);
        write(channel, bytes);
    }
    
    public static void write(SocketChannel channel, String protocol, int userId, AFile file) {
        byte[] protocolBytes = protocol.getBytes();
        byte[] userIdBytes = SerializationHelper.dataTypeTobytesFactory(userId);
        byte[] fileBytes = SerializationHelper.objectToBytes(file);
        byte[] lengthBytes = SerializationHelper.dataTypeTobytesFactory(fileBytes.length);
        byte[] bytes = SerializationHelper.joint(protocolBytes, userIdBytes, lengthBytes, fileBytes);
        write(channel, bytes);
    }
    
    /** ȡ��key��Selector��ע�Ტ�ر����Ӧͨ�� */
    private static void cancel(SelectionKey key) {
        key.cancel();
        if (key.channel() != null) {
            try {
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
//    ʹ������Ĵ����ʹ�÷������ͻ�������ͬ��
//    ��һ�����ݻ�û������϶���һ����ȡ�����ݵ����׳��쳣��
//    /** ��iд��channel */
//    public static void write(SocketChannel channel, int i) {
//        try {
//            ByteBuffer buf = ByteBuffer.allocate(4);
//            buf.putInt(i);
//            buf.flip();
//            channel.write(buf);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    
//    /** ��userд��channel */
//    public static void write(SocketChannel channel, User user) {
//        byte[] userBytes = SerializationHelper.objectToBytes(user);
//        int length = userBytes.length;
//        // ��д����ĳ��ȣ���������ʱ���֪��Ҫ�����ٳ���
//        write(channel, length);
//        write(channel, userBytes);
//    }
//    
//    /** ��fileд��channel */
//    public static void write(SocketChannel channel, AFile file) {
//        byte[] fileBytes = SerializationHelper.objectToBytes(file);
//        int length = fileBytes.length;
//        write(channel, length);
//        write(channel, fileBytes);
//    }
}