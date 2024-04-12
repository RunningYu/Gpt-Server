package chatgptserver.service;

import chatgptserver.bean.ao.ChatAO;
import chatgptserver.bean.ao.MessagesAO;
import chatgptserver.bean.ao.MessagesResponseAO;

import java.util.List;

/**
 * @author : 其然乐衣Letitbe
 * @date : 2024/3/23
 */
public interface MessageService {

    void recordHistory(String userCode, String chatCode, String message, String result);

    MessagesResponseAO historyList(String chatCode, int page, int size);

    void recordHistoryWithImage(String userCode, String chatCode, String imageUrl, String content, String totalResponse);

    List<ChatAO> chatCreateList(String token, String gptCode, String functionCode);

    MessagesAO buildMessageAO(String userCode, String chatCode, String content, String totalResponse);
}
