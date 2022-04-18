package com.java3y.austin.handler.receiver;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.java3y.austin.common.domain.AnchorInfo;
import com.java3y.austin.common.domain.LogParam;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.enums.AnchorState;
import com.java3y.austin.handler.pending.Task;
import com.java3y.austin.handler.pending.TaskPendingHolder;
import com.java3y.austin.handler.utils.GroupIdMappingUtils;
import com.java3y.austin.support.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author 3y
 * 消费MQ的消息
 */
@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "austinConsumer", topic = "${austin.business.topic.name}")
public class Receiver implements RocketMQListener<MessageExt> {
    private static final String LOG_BIZ_TYPE = "Receiver#consumer";
    @Autowired
    private ApplicationContext context;

    @Autowired
    private TaskPendingHolder taskPendingHolder;

    @Autowired
    private LogUtils logUtils;

    // 幂等？重试？
    @Override
    public void onMessage(MessageExt messageExt) {
        Optional<String> message = Optional.ofNullable(new String(messageExt.getBody()));
        if (message.isPresent()) {

            List<TaskInfo> taskInfoLists = JSON.parseArray(message.get(), TaskInfo.class);
            String messageGroupId = GroupIdMappingUtils.getGroupIdByTaskInfo(CollUtil.getFirst(taskInfoLists.iterator()));

            for (TaskInfo taskInfo : taskInfoLists) {
                Task task = context.getBean(Task.class).setTaskInfo(taskInfo);
                // 每种消息对应一个动态线程池，将 task 放入线程池，调度执行
                taskPendingHolder.route(messageGroupId).execute(task);
            }

        }
    }
}
