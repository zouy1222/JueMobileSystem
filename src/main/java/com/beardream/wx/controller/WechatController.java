package com.beardream.wx.controller;

import com.beardream.Utils.ResultUtil;
import com.beardream.model.Result;
import com.beardream.wx.config.WechatMpConfiguration;
import com.fasterxml.classmate.ResolvedType;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxJsapiSignature;
import me.chanjar.weixin.common.bean.menu.WxMenu;
import me.chanjar.weixin.common.bean.menu.WxMenuButton;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.session.Constants;
import me.chanjar.weixin.common.session.WxSession;
import me.chanjar.weixin.mp.api.WxMpConfigStorage;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.menu.WxMpMenu;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Binary Wang
 */
@RestController
@RequestMapping("/api/mobile/wechat/portal")
public class WechatController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WxMpService mWxMpService;

    @Autowired
    private WxMpMessageRouter router;

    @GetMapping(produces = "text/plain;charset=utf-8")
    public String authGet(
            @RequestParam(name = "signature",
                    required = false) String signature,
            @RequestParam(name = "timestamp",
                    required = false) String timestamp,
            @RequestParam(name = "nonce", required = false) String nonce,
            @RequestParam(name = "echostr", required = false) String echostr) {

        this.logger.info("\n接收到来自微信服务器的认证消息：[{}, {}, {}, {}]", signature,
            timestamp, nonce, echostr);

        if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
            throw new IllegalArgumentException("请求参数非法，请核实!");
        }

        if (this.mWxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }

        return "非法请求";
    }

    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam(name = "encrypt_type",
                    required = false) String encType,
            @RequestParam(name = "msg_signature",
                    required = false) String msgSignature) {
        this.logger.info(
            "\n接收微信请求：[signature=[{}], encType=[{}], msgSignature=[{}],"
                + " timestamp=[{}], nonce=[{}], requestBody=[\n{}\n] ",
            signature, encType, msgSignature, timestamp, nonce, requestBody);

        if (!this.mWxMpService.checkSignature(timestamp, nonce, signature)) {
            throw new IllegalArgumentException("非法请求，可能属于伪造的请求！");
        }
        
        String out = null;
        if (encType == null) {
            // 明文传输的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
            WxMpXmlOutMessage outMessage = this.route(inMessage);
            if (outMessage == null) {
                return "";
            }

            out = outMessage.toXml();
        } else if ("aes".equals(encType)) {
            // aes加密的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(
                requestBody, this.mWxMpService.getWxMpConfigStorage(), timestamp,
                nonce, msgSignature);
            this.logger.debug("\n消息解密后内容为：\n{} ", inMessage.toString());
            WxMpXmlOutMessage outMessage = this.route(inMessage);
            if (outMessage == null) {
                return "";
            }

            out = outMessage
                .toEncryptedXml(this.mWxMpService.getWxMpConfigStorage());
        }

        this.logger.debug("\n组装回复信息：{}", out);

        return out;
    }

    private WxMpXmlOutMessage route(WxMpXmlMessage message) {
        try {
            return this.router.route(message);
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        }

        return null;
    }

    @GetMapping("/menucreate")
    private void menu(){
        WxMenu wxMenu = new WxMenu();
        List<WxMenuButton> buttons = new ArrayList<>();
        WxMenuButton button = new WxMenuButton();
        button.setType("view");
        // redirectURI,  scope,  state
        button.setUrl(mWxMpService.oauth2buildAuthorizationUrl("http://wx.chiprincess.cn/api/mobile/index", WxConsts.OAUTH2_SCOPE_USER_INFO, "STATE"));
        button.setName("获取用户信息");
        buttons.add(button);
        wxMenu.setButtons(buttons);
        try {
            mWxMpService.getMenuService().menuCreate(wxMenu);
        } catch (WxErrorException e) {
            e.printStackTrace();
        }
        try {
            WxMpMenu wxMenuGet = mWxMpService.getMenuService().menuGet();
            System.out.println(wxMenuGet.toString());
        } catch (WxErrorException e) {
            e.printStackTrace();
        }
    }
    
    @GetMapping("/getWxConfig")
    public Result getJsConfigInfo(@RequestParam String url){
        try {
            System.out.println(mWxMpService.getJsapiTicket(false));
            WxJsapiSignature wxJsapiSignature = mWxMpService.createJsapiSignature(url);
            return ResultUtil.success(wxJsapiSignature);
        } catch (WxErrorException e) {
            e.printStackTrace();
            return ResultUtil.error(-1,"获取js接口失败");
        }
    }
}
