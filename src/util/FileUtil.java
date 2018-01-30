package util;

import java.io.File;
import java.io.IOException;

public class FileUtil {
/**
 * 创建新目录，如果目录已存在 则跳过
 * @param path 目录地址
 */
	public static void mkdir(String path) {
		File f = new File(path);
		if(!f.exists()) {
			f.mkdirs();
		}
	}
	/**
	 * 创建新文件到指定目录，如果目录不存在 则跳过并抛出IO错误
	 * @param path 文件存放目录
	 * @throws IOException IO错误
	 */
	public static void createNewFile(String path) throws IOException {
		File f = new File(path);
		if(!f.exists()) {
			f.createNewFile();
		}
	}
}
