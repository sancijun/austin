package com.java3y.austin.web.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.java3y.austin.common.constant.AustinConstant;
import com.java3y.austin.common.enums.AuditStatus;
import com.java3y.austin.common.enums.MessageStatus;
import com.java3y.austin.common.enums.TemplateType;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.support.dao.MessageTemplateDao;
import com.java3y.austin.support.domain.MessageTemplate;
import com.java3y.austin.web.service.MessageTemplateService;
import com.java3y.austin.web.vo.MessageTemplateParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息模板管理 Service
 *
 * @author 3y
 * @date 2022/1/22
 */
@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {


    @Autowired
    private MessageTemplateDao messageTemplateDao;

    @Override
    public List<MessageTemplate> queryList(MessageTemplateParam param) {
        PageRequest pageRequest = PageRequest.of(param.getPage() - 1, param.getPerPage());
        return messageTemplateDao.findAllByIsDeletedEquals(AustinConstant.FALSE, pageRequest);
    }

    @Override
    public Long count() {
        return messageTemplateDao.countByIsDeletedEquals(AustinConstant.FALSE);
    }

    @Override
    public MessageTemplate saveOrUpdate(MessageTemplate messageTemplate) {
        if (messageTemplate.getId() == null) {
            initStatus(messageTemplate);
        } else {
            resetStatus(messageTemplate);
        }

        messageTemplate.setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        return messageTemplateDao.save(messageTemplate);
    }


    @Override
    public void deleteByIds(List<Long> ids) {
        Iterable<MessageTemplate> messageTemplates = messageTemplateDao.findAllById(ids);
        messageTemplates.forEach(messageTemplate -> messageTemplate.setIsDeleted(AustinConstant.TRUE));
        messageTemplateDao.saveAll(messageTemplates);
    }

    @Override
    public MessageTemplate queryById(Long id) {
        return messageTemplateDao.findById(id).get();
    }

    @Override
    public void copy(Long id) {
        MessageTemplate messageTemplate = messageTemplateDao.findById(id).get();
        MessageTemplate clone = ObjectUtil.clone(messageTemplate).setId(null).setCronTaskId(null);
        messageTemplateDao.save(clone);
    }

    /**
     * 初始化状态信息
     * TODO 创建者 修改者 团队
     *
     * @param messageTemplate
     */
    private void initStatus(MessageTemplate messageTemplate) {
        messageTemplate.setFlowId(StrUtil.EMPTY)
                .setMsgStatus(MessageStatus.INIT.getCode()).setAuditStatus(AuditStatus.WAIT_AUDIT.getCode())
                .setCreator("Java3y").setUpdator("Java3y").setTeam("公众号Java3y").setAuditor("3y")
                .setCreated(Math.toIntExact(DateUtil.currentSeconds()))
                .setIsDeleted(AustinConstant.FALSE);

    }

    /**
     * 1. 重置模板的状态
     * 2. 修改定时任务信息(如果存在)
     *
     * @param messageTemplate
     */
    private void resetStatus(MessageTemplate messageTemplate) {
        messageTemplate.setUpdator(messageTemplate.getUpdator())
                .setMsgStatus(MessageStatus.INIT.getCode()).setAuditStatus(AuditStatus.WAIT_AUDIT.getCode());

        if (messageTemplate.getCronTaskId() != null && TemplateType.CLOCKING.getCode().equals(messageTemplate.getTemplateType())) {
        }
    }


}
