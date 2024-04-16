package chatgptserver.service;


import chatgptserver.bean.ao.JsonResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author : 其然乐衣Letitbe
 * @date : 2024/3/27
 */
public interface TongYiService {

    JsonResult tyImageUnderstand(MultipartFile image, String content, String token, String chatCode);

    JsonResult tyQuestion(String token, String chatCode, String content);

    JsonResult tyImageCreate(String userCode, String chatCode, String content);
}
