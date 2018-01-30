package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ���л�����
 * �����ֽ�����������������͵Ļ���ת��
 */
public class SerializationHelper {

	public static byte[] objectToBytes(Object o) {
		byte[] bytes = null;
		try (
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
		){
			oos.writeObject(o);
			bytes = baos.toByteArray();
			}catch (IOException e) {
				e.printStackTrace();
			}
		return bytes;
	}
	
	 public static Object bytesToObject(byte[] bytes) {
	        Object o = null;
	        try (
	                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
	                ObjectInputStream ois = new ObjectInputStream(bais);
	                ) {
	            o = ois.readObject();
	        } catch (IOException | ClassNotFoundException e) {
	            e.printStackTrace();
	        }
	        return o;
	    }
	 
	 /**
	     * �ѻ�����������תΪ�ֽ�����
	     * Ŀǰ��ʵ����long/int��ת��
	     * @param o ������������
	     * @return ���������͵��ֽ�����
	     */
	    public static byte[] dataTypeTobytesFactory(Object o) {
	        byte[] bytes = null;
	        String typeName = o.getClass().getSimpleName();
	        try (
	                ByteArrayOutputStream baos = new ByteArrayOutputStream();
	                DataOutputStream dos = new DataOutputStream(baos);
	                ) {
	            switch (typeName) {
	            case "Integer":
	                int i = (int) o;
	                dos.writeInt(i);
	                break;
	            case "Long":
	                long l = (long) o;
	                dos.writeLong(l);
	                break;
	            }
	            bytes = baos.toByteArray();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return bytes;
	    }
	    
	    /**
	     * ���ղ��������˳�򣬰Ѷ���ֽ�����ƴ������
	     * @param byteArrays �����ƴ�ӵ��ֽ�����
	     * @return ƴ�Ӻ���ֽ�����
	     */
	    public static byte[] joint(byte[]...byteArrays) {
	        int length = 0;
	        for (int i = 0; i < byteArrays.length; i++) {
	            length += byteArrays[i].length;
	        }
	        byte[] bytes = new byte[length];
	        int lastIndex = 0;
	        for (int i = 0; i < byteArrays.length; i++) {
	            System.arraycopy(byteArrays[i], 0, bytes, lastIndex, byteArrays[i].length);
	            lastIndex += byteArrays[i].length;
	        }
	        return bytes;
	    }
	    
	    @Deprecated
	    public static byte[] getBytesOfInt(int i) {
	        byte[] bytes = null;
	        try (
	                ByteArrayOutputStream baos = new ByteArrayOutputStream();
	                DataOutputStream dos = new DataOutputStream(baos);
	                ) {
	            dos.writeInt(i);
	            bytes = baos.toByteArray();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return bytes;
	    }
	    
	    @Deprecated
	    public static byte[] getBytesOfLong(Long l) {
	        byte[] bytes = null;
	        try (
	                ByteArrayOutputStream baos = new ByteArrayOutputStream();
	                DataOutputStream dos = new DataOutputStream(baos);
	                ) {
	            dos.writeLong(l);
	            bytes = baos.toByteArray();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return bytes;
	        /*
	         * ��һ��д��
	         * byte[] bytes = new byte[8];
	         * bytes[0] = (byte) (0xff & (l >>> 56));
	         * bytes[1] = (byte) (0xff & (l >>> 48));
	         * bytes[2] = (byte) (0xff & (l >>> 40));
	         * bytes[3] = (byte) (0xff & (l >>> 32));
	         * bytes[4] = (byte) (0xff & (l >>> 24));
	         * bytes[5] = (byte) (0xff & (l >>> 16));
	         * bytes[6] = (byte) (0xff & (l >>> 8));
	         * bytes[7] = (byte) (0xff & (l >>> 0));
	         */
	    }
	
}
