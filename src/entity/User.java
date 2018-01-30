package entity;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class User {
	private int id;
	private String username;
	private String password;
	private List<AFile> files;
	 
	public User(String username, String password) {
		
		this.username = username;
		this.password = password;
		
		 // 如果使用该构造器，下面两个域是无效的
        id = -1;
        files = null;
	}
	
	public User(int id, String username, String password) {
		this.id = id;
		this.username = username;
		this.password = password;
		files = new ArrayList<>();
	}
	
	public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    public void addFile(AFile file) {
    	files.add(file);
    }
    
    public void listFiles() {
    	int size = files.size();
    	for(int i = 0; i< size; i++) {
    		System.out.println(files.get(i));
    	}
    }
    
    public boolean hasFile(AFile file) throws FileNotFoundException {
    	boolean has = files.contains(file);
    	if(has == false) throw new FileNotFoundException("您没有此文件！");
    	return has;
    }
    
}
