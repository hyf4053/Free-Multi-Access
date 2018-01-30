package service;

/**
 * ����˺Ϳͻ���ͨ�ŵı��
 * ÿ����ǳ���Ϊ2���ַ�
 * �磺LOG_IN + SUCCESS�����¼�ɹ�
 * ���һ���ַ����ܱ�ʾ���ͨ�ŵ�Ŀ�ģ�����һ���ַ�Ϊռλ��
 * �磺LOG_IN + PLACEHOLDER + �û����󣬱�ʾ���û���������Ĳ����ǵ�¼
 */
public interface NetDiskProtocol {
    
    /** Э���ַ��ĳ��� */
    int PROTOCOL_LEN = 2;
    
    /** ռλ�� */
    String PLACEHOLDER = "��";
    
    /** �ָ��� */
    String SPLIT_SIGN = "��";
    
    String SIGNUP = "��";
    String LOGIN = "��";
    String UPLOAD = "��";
    String DOWNLOAD = "��";
    String SUCCESS = "��";
    String UNKNOWN_ERROR = "��";
    String USER_EXIST = "��";
    String FILE_NOT_FOUND = "��";
    String USER_NOT_FOUND = "��";
    String WRONG_PASSWORD = "��";
    String UPLOADED_IN_A_SECOND = "��";
}
