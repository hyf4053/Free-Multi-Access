package entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;

public class AFile implements Serializable {
	
	//文件的id，会存储于数据库中
	private int id;
	private String name;
	private String format;
	
	//文件在服务端存贮的路径
	private String path;
	
	/** 供客户端舒勇，主要用于获取文件名及文件类型（后缀） */
	public AFile(String path) throws FileNotFoundException {
		File file = new File(path);
		if(!file.exists())
			throw new FileNotFoundException();
		String[] temp = split(file.getName());
		name = temp[0];
		format = temp[1];
		
		//无效域
		id = -1;
		path = null;
	}
	
	/**
	 * 供服务端使用，服务端使用这个Constructor来创建一个拥有壳信息的文件对象
	 * @param id 文件id
	 * @param name 文件名
	 * @param format 文件格式
	 * @param path 文件在服务器的存档路径
	 */
	public AFile(int id, String name, String format, String path) {
		this.id = id;
		this.name = name;
		this.format = format;
		this.path = path;
	}
	
	/**
	 * 文件在用户对象中的保存形式
	 * @param name 名称
	 * @param format 文件类型
	 */
	public AFile(String name, String format) {
		this.name = name;
		this.format = format;
	}
	
	/**
	 * 获取文件名与后缀
	 * @param fullName 文件全名，包含后缀，比如 picture.jpeg
	 * @return 长度为2的String数组，0为文件名，1为后缀
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
