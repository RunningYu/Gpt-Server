package chatgptserver.service;

import chatgptserver.bean.ao.MessagesResponseAO;
import chatgptserver.bean.po.ChatPO;

import java.util.List;

/**
 * @author : 其然乐衣Letitbe
 * @date : 2024/3/23
 */
public interface MessageService {

    void recordHistory(String userCode, String chatCode, String message, String result);

    MessagesResponseAO historyList(String chatCode, int page, int size);

    void recordHistoryWithImage(String userCode, String chatCode, String imageUrl, String content, String totalResponse);

    List<ChatPO> chatCreateList(String userCode, String gptCode);
}
