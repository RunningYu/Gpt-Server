package chatgptserver.controller;

import chatgptserver.bean.ao.JsonResult;
import chatgptserver.bean.ao.QuestionRequestAO;
import chatgptserver.bean.ao.ppt.PptCreateRequestAO;
import chatgptserver.service.PptService;
import chatgptserver.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author : 其然乐衣Letitbe
 * @date : 2024/4/22
 */
@Slf4j
@RestController
public class PptController {

    @Autowired
    private PptService pptService;

    @Autowired
    private UserService userService;

    @ApiOperation("PPT大纲生成")
    @PostMapping("/ppt/outline/create")
    public JsonResult pptOutlineCreate(HttpServletRequest httpServletRequest,
                                       @RequestBody QuestionRequestAO request) {
        String token = httpServletRequest.getHeader("token");
        log.info("PptController pptOutlineCreate request:[{}], token:[{}]", request, token);
        String userCode = userService.getUserCodeByToken(token);
        JsonResult response = pptService.pptOutlineCreate(userCode, request.getContent(), request.getIsRebuild(), request.getCid());

        return response;
    }

    @ApiOperation("根据PPT大纲生成PPT")
    @PostMapping("/ppt/create/by/outline")
    public JsonResult pptCreateByOutline(HttpServletRequest httpServletRequest,
                                         @RequestBody PptCreateRequestAO request) {
        String token = httpServletRequest.getHeader("token");
        log.info("PptController pptCreateByOutline request:[{}] token:[{}]", request, token);
        request.setUserCode(userService.getUserCodeByToken(token));
        JsonResult response = pptService.pptCreateByOutline(request);

        return response;
    }

    @ApiOperation("获取主题颜色列表")
    @GetMapping("/ppt/color/list")
    public JsonResult pptColorList(HttpServletRequest httpServletRequest) {
        log.info("PptController pptColorList");
        JsonResult response = pptService.pptColorList();

        return response;
    }

    @ApiOperation("PPT上传")
    @GetMapping("/ppt/upload")
    public JsonResult pptUpload() {

        return null;
    }

    @ApiOperation("PPT评分")
    @GetMapping("/ppt/scoring")
    public JsonResult pptScoring() {

        return null;
    }

    @ApiOperation("PPT分类查询")
    @GetMapping("/ppt/list")
    public JsonResult pptList() {

        return null;
    }

    @ApiOperation("PPT收藏")
    @GetMapping("/ppt/collect")
    public JsonResult pptCollect() {

        return null;
    }

    @ApiOperation("PPT收藏列表")
    @GetMapping("/ppt/collect/list")
    public JsonResult pptCollectList() {

        return null;
    }

}