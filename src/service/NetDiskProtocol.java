package service;

/**
 * 服务端和客户端通信的标记
 * 每个标记长度为2个字符
 * 如：LOG_IN + SUCCESS代表登录成功
 * 如果一个字符就能表示清楚通信的目的，则另一个字符为占位符
 * 如：LOG_IN + PLACEHOLDER + 用户对象，表示该用户对象请求的操作是登录
 */
public interface NetDiskProtocol {
    
    /** 协议字符的长度 */
    int PROTOCOL_LEN = 2;
    
    /** 占位符 */
    String PLACEHOLDER = "０";
    
    /** 分隔符 */
    String SPLIT_SIGN = "１";
    
    String SIGNUP = "２";
    String LOGIN = "３";
    String UPLOAD = "４";
    String DOWNLOAD = "５";
    String SUCCESS = "６";
    String UNKNOWN_ERROR = "７";
    String USER_EXIST = "８";
    String FILE_NOT_FOUND = "９";
    String USER_NOT_FOUND = "Ａ";
    String WRONG_PASSWORD = "Ｂ";
    String UPLOADED_IN_A_SECOND = "Ｃ";
}
