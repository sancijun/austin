package com.java3y.austin.handler.handler.impl;


import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.google.common.base.Throwables;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.dto.EmailContentModel;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.handler.handler.BaseHandler;
import com.java3y.austin.handler.handler.Handler;
import com.java3y.austin.handler.idempotent.Idempotent;
import com.java3y.austin.support.utils.AccountUtils;
import com.sun.mail.util.MailSSLSocketFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 邮件发送处理
 *
 * @author 3y
 */
@Component
@Slf4j
public class EmailHandler extends BaseHandler implements Handler {

    private static final String EMAIL_ACCOUNT_KEY = "account.emailAccount";
    private static final String PREFIX = "email_";

    @Autowired
    private AccountUtils accountUtils;

    public EmailHandler() {
        channelCode = ChannelType.EMAIL.getCode();
    }

    // 幂等？重试？
    @Override
    @Idempotent(prefix = "austin", target = "taskInfo", subkeys = {"businessId", "receiver", "contentModel"})
    public boolean handler(TaskInfo taskInfo) {
        EmailContentModel emailContentModel = (EmailContentModel) taskInfo.getContentModel();
        MailAccount account = getAccountConfig(taskInfo.getSendAccount());
        try {
            MailUtil.send(account, taskInfo.getReceiver(), emailContentModel.getTitle(),
                    emailContentModel.getContent(), true, null);
        } catch (Exception e) {
            log.error("EmailHandler#handler fail!{},params:{}", Throwables.getStackTraceAsString(e), taskInfo);
            return false;
        }
        return true;
    }

    /**
     * 获取账号信息合配置
     *
     * @return
     */
    private MailAccount getAccountConfig(Integer sendAccount) {
        MailAccount account = accountUtils.getAccount(sendAccount, EMAIL_ACCOUNT_KEY, PREFIX, new MailAccount());
        try {
            MailSSLSocketFactory sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
            account.setAuth(true).setStarttlsEnable(true).setSslEnable(true).setCustomProperty("mail.smtp.ssl.socketFactory", sf);
            account.setTimeout(25000).setConnectionTimeout(25000);
        } catch (Exception e) {
            log.error("EmailHandler#getAccount fail!{}", Throwables.getStackTraceAsString(e));
        }
        return account;
    }

}
