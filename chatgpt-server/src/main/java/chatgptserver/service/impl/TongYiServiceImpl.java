package chatgptserver.service.impl;

import chatgptserver.bean.dto.XunFeiXingHuo.imageCreate.Text;
import chatgptserver.bean.dto.tongYiQianWen.*;
import chatgptserver.bean.po.MessagesPO;
import chatgptserver.dao.MessageMapper;
import chatgptserver.enums.GPTConstants;
import chatgptserver.service.MessageService;
import chatgptserver.service.OkHttpService;
import chatgptserver.service.TongYiService;
import chatgptserver.utils.MinioUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : 其然乐衣Letitbe
 * @date : 2024/3/27
 */
@Slf4j
@Service
public class TongYiServiceImpl implements TongYiService {

    @Autowired
    private OkHttpService okHttpService;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public String tyImageUnderstand(MultipartFile image, String content, String userCode, String chatCode) {
        log.info("TongYiServiceImpl tyImageUnderstand image:[{}] content:[{}], userCode:[{}], chatCode:[{}]", image, content, userCode, chatCode);
        String imageUrl = "";
        if (image != null) {
            imageUrl = minioUtil.upLoadFileToURL(image);
        }
        TongYiImageUnderStandRequestDTO request = buildTongYiImageUnderstandRequestDTO(chatCode, imageUrl, content);
        log.info("WenXinServiceImpl tyImageUnderstand request:[{}]", request);
        String responseStr = "";
        try {
            responseStr = okHttpService.makePostRequest(
                    GPTConstants.TONG_YI_QIAN_WEN_IMAGE_UNDERSTAND_URL,
                    JSON.toJSONString(request),
                    GPTConstants.TONG_YI_QIAN_WEN_API_KEY);
            log.info("WenXinServiceImpl tyImageUnderstand responseStr:[{}]", responseStr);
        } catch (IOException e) {
            throw new RuntimeException("请求通义千问接口异常");
        }
        TongYiImageUnderstandResponseDTO responseDTO = JSON.parseObject(responseStr, TongYiImageUnderstandResponseDTO.class);
        String response = responseDTO.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text");
        log.info("TongYiServiceImpl tyImageUnderstand response:[{}]", response);
        messageService.recordHistoryWithImage(userCode, chatCode, imageUrl.equals("") ? "0" : imageUrl, content, response);

        return response;
    }

    @Override
    public String tyQuestion(String userCode, String chatCode, String content) {
        log.info("TongYiServiceImpl getMessageFromWenXin userCode:[{}] chatCode:[{}], content:[{}]", userCode, chatCode, content);
        Text text = new Text("user", content);
        List<Text> textList = new ArrayList<>();
        textList.add(text);

        // 获取历史聊天记录
        List<MessagesPO> historyLis = messageMapper.getWenXinHistory(chatCode);
        for (MessagesPO history : historyLis) {
            Text replication = new Text("assistant", history.getReplication());
            textList.add(0, replication);
            Text question = new Text("user", history.getQuestion());
            textList.add(0, question);
        }

        QuestionInput input = new QuestionInput(textList);
        QuestionRequestDTO request = new QuestionRequestDTO("qwen-14b-chat", input);
        log.info("TongYiServiceImpl getMessageFromWenXin input:[{}]", input);
        String responseStr = "";
        try {
            responseStr = okHttpService.makePostRequest(GPTConstants.TONG_YI_QIAN_WEN_QUESTION_URL, JSON.toJSONString(request), GPTConstants.TONG_YI_QIAN_WEN_API_KEY);
            log.info("TongYiServiceImpl getMessageFromWenXin responseStr:[{}]", responseStr);
        } catch (IOException e) {
            throw new RuntimeException("通义千问文本问答接口调用异常");
        }
        QuestionResponseDTO responseDTO = JSON.parseObject(responseStr, QuestionResponseDTO.class);
        String response = responseDTO.getOutput().getText();
        messageService.recordHistory(userCode, chatCode, content, response);

        return response;
    }

    @Override
    public String tyImageCreate(String userCode, String chatCode, String content) {
        log.info("TongYiServiceImpl tyImageCreate userCode:[{}] chatCode:[{}], content:[{}]", userCode, chatCode, chatCode);
        TongYiImageCreateRequestDTO request = TongYiImageCreateRequestDTO.buildTongYiImageCreateRequestDTO(content);
        log.info("TongYiServiceImpl tyImageCreate request:[{}]", JSON.toJSONString(request));

        String responseStr = "";
        try {
            responseStr = okHttpService.makePostRequest(GPTConstants.TONG_YI_QIAN_WEN_IMAGE_CREATE_POST_URL, JSON.toJSONString(request), GPTConstants.TONG_YI_QIAN_WEN_API_KEY);
            log.info("TongYiServiceImpl tyImageCreate responseStr:[{}]", responseStr);
        } catch (IOException e) {
            throw new RuntimeException("调用大模型接口异常");
        }
        ImageCreateResultOutput resultOutput = JSON.parseObject(responseStr, ImageCreateResultOutput.class);
        String task_id = resultOutput.getTask_id();
        String response = "";
        while (true) {
            String url = String.format(GPTConstants.TONG_YI_QIAN_WEN_IMAGE_CREATE_GET_URL, task_id);
            String res = "";
            try {
                res = okHttpService.makeGetRequest(url);
            } catch (IOException e) {
                throw new RuntimeException("获取作业结果接口异常！");
            }
            TongYiImageCreateResponseDTO responseDTO = JSON.parseObject(res, TongYiImageCreateResponseDTO.class);
            if (responseDTO.getOutput().getTask_status().equals("SUCCEEDED")) {
                log.info("TongYiServiceImpl tyImageCreate res:[{}]", res);
                response = responseDTO.getOutput().getResults().get(0).get("url");
                break;
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
            }
        }
        log.info("TongYiServiceImpl tyImageCreate response:[{}]", response);
        messageService.recordHistory(userCode, chatCode, content, response);

        return response;
    }


    public TongYiImageUnderStandRequestDTO buildTongYiImageUnderstandRequestDTO(String chatCode, String imageUrl, String content) {
        List<TongYiMessages> list = new ArrayList<>();
        // 如果没有图片链接，则表示是开始了多轮对话
        if (imageUrl == null || imageUrl.equals("")) {
            // 获取 通义千问：文本问答 的第一轮对话
            MessagesPO messagesFistChat = messageMapper.getTongYiQuestionFistChat(chatCode);
            log.info("TongYiServiceImpl TongYiImageUnderStandRequestDTO messagesFistChat:[{}]", messagesFistChat);
            TongYiMessages tongYiMessagesFirstMap = TongYiMessages.buildTongYiMessages("user", messagesFistChat.getImage(), messagesFistChat.getQuestion());
            TongYiMessages tongYiMessagesFirstRplMap = TongYiMessages.buildTongYiMessages("assistant", messagesFistChat.getReplication());
            list.add(tongYiMessagesFirstMap);
            list.add(tongYiMessagesFirstRplMap);
            System.out.println(tongYiMessagesFirstMap);
            System.out.println(tongYiMessagesFirstRplMap);

            // 获取历史聊天记录
            List<MessagesPO> historyLis = messageMapper.getTongYiMultipleQuestionHistory(chatCode, messagesFistChat.getId());
            for (MessagesPO history : historyLis) {

                TongYiMessages messagesQuestion = TongYiMessages.buildTongYiMessages("user", history.getQuestion());
                TongYiMessages messagesReplication = TongYiMessages.buildTongYiMessages("assistant", history.getReplication());
                list.add(messagesQuestion);
                list.add(messagesReplication);
                System.out.println(messagesQuestion);
                System.out.println(messagesReplication);
            }
            TongYiMessages messagesNew = TongYiMessages.buildTongYiMessages("user", content);
            list.add(messagesNew);
        }

        // 如果有图片链接，则表示新的一轮 图片理解 对话
        else {
            Map<String, String> imageMap = new HashMap<>();
            imageMap.put("image", imageUrl);
            Map<String, String> textMap = new HashMap<>();
            textMap.put("text", content);
            List<Map<String, String>> contentList = new ArrayList<>();
            contentList.add(imageMap);
            contentList.add(textMap);
            TongYiMessages messages = new TongYiMessages("user", contentList);
            log.info("WenXinServiceImpl tyImageUnderstand messages:[{}]", messages);
            list.add(messages);
        }

        Input input = new Input(list);
        TongYiImageUnderStandRequestDTO request = new TongYiImageUnderStandRequestDTO("qwen-vl-plus");
        request.setInput(input);

        return request;
    }
}
