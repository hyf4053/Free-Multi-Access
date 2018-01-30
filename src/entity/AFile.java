package entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;

public class AFile implements Serializable {
	
	//�ļ���id����洢�����ݿ���
	private int id;
	private String name;
	private String format;
	
	//�ļ��ڷ���˴�����·��
	private String path;
	
	/** ���ͻ������£���Ҫ���ڻ�ȡ�ļ������ļ����ͣ���׺�� */
	public AFile(String path) throws FileNotFoundException {
		File file = new File(path);
		if(!file.exists())
			throw new FileNotFoundException();
		String[] temp = split(file.getName());
		name = temp[0];
		format = temp[1];
		
		//��Ч��
		id = -1;
		path = null;
	}
	
	/**
	 * �������ʹ�ã������ʹ�����Constructor������һ��ӵ�п���Ϣ���ļ�����
	 * @param id �ļ�id
	 * @param name �ļ���
	 * @param format �ļ���ʽ
	 * @param path �ļ��ڷ������Ĵ浵·��
	 */
	public AFile(int id, String name, String format, String path) {
		this.id = id;
		this.name = name;
		this.format = format;
		this.path = path;
	}
	
	/**
	 * �ļ����û������еı�����ʽ
	 * @param name ����
	 * @param format �ļ�����
	 */
	public AFile(String name, String format) {
		this.name = name;
		this.format = format;
	}
	
	/**
	 * ��ȡ�ļ������׺
	 * @param fullName �ļ�ȫ����������׺������ picture.jpeg
	 * @return ����Ϊ2��String���飬0Ϊ�ļ�����1Ϊ��׺
	 */
	
	public static String[] split(String fullName) {
		String[] temp = fullName.split("\\.");
		String[] result = new String[2];
		result[0] = "";
		result[1] = "";
		if(temp.length == 2) {
			result[0] = temp[0];
            result[1] = temp[1];
		}else if (temp.length > 2) {
			 int length = temp.length;
			 result[0] = temp[0];
			 for (int i = 1; i < length - 1; i++) {
	                result[0] = result[0] + "." +temp[i];
	            }
			 result[1] = temp[length - 1];
		} else {
			result[0] = temp[0];
		}
		
		return result;
	}
	
	public int getId() {
        return id;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public String toString() {
        if (format.equals("")) return name;
        return name + "." + format;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        AFile other = (AFile) obj;
        if (this.name.equals(other.name) && this.format.equals(other.format)) {
            return true;
        } else {
            return false;
        }
    }
}
