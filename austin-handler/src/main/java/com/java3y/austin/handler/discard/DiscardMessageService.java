package com.java3y.austin.handler.discard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.support.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 丢弃模板消息
 * @author 3y.
 */
@Service
public class DiscardMessageService {
    private static final String DISCARD_MESSAGE_KEY = "discard";

    @Value("${discard:[\"7\",\"8\"]}")
    private String discard;

    @Autowired
    private LogUtils logUtils;
    

    /**
     * 丢弃消息，配置在apollo
     * @param taskInfo
     * @return
     */
    public boolean isDiscard(TaskInfo taskInfo) {
        // 配置示例:	["1","2"]
        JSONArray array = JSON.parseArray(discard);

        if (array.contains(String.valueOf(taskInfo.getMessageTemplateId()))) {
            return true;
        }
        return false;
    }

}
