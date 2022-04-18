package com.java3y.austin.service.api.impl.action;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Throwables;
import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.service.api.impl.domain.SendTaskModel;
import com.java3y.austin.support.pipeline.BusinessProcess;
import com.java3y.austin.support.pipeline.ProcessContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author sancijun
 * 将消息发送到MQ
 */
@Slf4j
public class SendMqAction implements BusinessProcess {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${austin.business.topic.name}")
    private String topicName;

    @Override
    public void process(ProcessContext context) {
        SendTaskModel sendTaskModel = (SendTaskModel) context.getProcessModel();
        String message = JSON.toJSONString(sendTaskModel.getTaskInfo(), new SerializerFeature[]{SerializerFeature.WriteClassName});

        try {
            this.send(message);
        } catch (Exception e) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(RespStatusEnum.SERVICE_ERROR));
            log.error("send rocketmq fail! e:{},params:{}", Throwables.getStackTraceAsString(e)
                    , JSON.toJSONString(CollUtil.getFirst(sendTaskModel.getTaskInfo().listIterator())));
        }
    }

    /**
     * 发送 RocketMQ 消息
     * @param jsonMessage
     */
    public void send(String jsonMessage) {
        rocketMQTemplate.asyncSend(topicName, jsonMessage, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("send message to topic=[{}] sendResult=[{}]", topicName, sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("send message to topic=[{}]", topicName, throwable);
            }
        });
    }
}
