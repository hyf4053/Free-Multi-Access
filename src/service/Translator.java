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
 * 翻译类
 * 用于服务端与客户端读写信息
 */
public class Translator {
    
    /** 负责编码与解码的字符集对象 */
    private static Charset coder = Charset.forName("GBK");
    
    public static String readProtocol(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            // 准备下一次读取
            key.interestOps(SelectionKey.OP_READ);
            return readProtocol(channel);
        } catch (IOException e) {
            /*
             * 如果捕获到异常
             * 说明客户端出现问题（可能已经关闭）
             * 因此把该SelectionKey取消注册
             */
            System.out.println("cancel a key when readProtocol");
            cancel(key);
        }
        return null;
    }
    
    public static String readProtocol(SocketChannel channel) throws IOException {
        // 一个协议字符2个字节，因此为buf分配4字节的容量
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
         * 先读取对象序列的长度
         * 为ByteBuffer分配该长度的空间
         * 然后再读取对象序列
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
        // 一个int 4个字节，因此为buf分配4字节的空间
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
     * 简单工厂模式，把一个对象或基本数据类型写入通道中
     * @param channel 写入的通道
     * @param o 对象或基本数据类型
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
            bytes = message.getBytes();// 使用默认编码
            break;
        default:
            System.err.println("未找到对象的类名！");
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
    
    /** 取消key对Selector的注册并关闭其对应通道 */
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
    
//    使用下面的代码会使得服务端与客户端难以同步
//    （一方数据还没传输完毕而另一方读取了数据导致抛出异常）
//    /** 把i写入channel */
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
//    /** 把user写入channel */
//    public static void write(SocketChannel channel, User user) {
//        byte[] userBytes = SerializationHelper.objectToBytes(user);
//        int length = userBytes.length;
//        // 先写对象的长度，这样读的时候才知道要读多少长度
//        write(channel, length);
//        write(channel, userBytes);
//    }
//    
//    /** 把file写入channel */
//    public static void write(SocketChannel channel, AFile file) {
//        byte[] fileBytes = SerializationHelper.objectToBytes(file);
//        int length = fileBytes.length;
//        write(channel, length);
//        write(channel, fileBytes);
//    }
}