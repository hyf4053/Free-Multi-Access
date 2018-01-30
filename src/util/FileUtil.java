package util;

import java.io.File;
import java.io.IOException;

public class FileUtil {
/**
 * ������Ŀ¼�����Ŀ¼�Ѵ��� ������
 * @param path Ŀ¼��ַ
 */
	public static void mkdir(String path) {
		File f = new File(path);
		if(!f.exists()) {
			f.mkdirs();
		}
	}
	/**
	 * �������ļ���ָ��Ŀ¼�����Ŀ¼������ ���������׳�IO����
	 * @param path �ļ����Ŀ¼
	 * @throws IOException IO����
	 */
	public static void createNewFile(String path) throws IOException {
		File f = new File(path);
		if(!f.exists()) {
			f.createNewFile();
		}
	}
}
