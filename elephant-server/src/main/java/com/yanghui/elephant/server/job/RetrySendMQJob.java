package com.yanghui.elephant.server.job;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.yanghui.elephant.common.constant.SendStatus;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.mq.producer.ProducerService;
import com.yanghui.elephant.store.entity.MessageEntity;
import com.yanghui.elephant.store.mapper.MessageEntityMapper;

@Component
@Log4j2
public class RetrySendMQJob implements SimpleJob{
	
	@Autowired
	private ProducerService producerService;
	
	@Autowired
	private MessageEntityMapper messageEntityMapper;

	@SuppressWarnings("unchecked")
	@Override
	public void execute(ShardingContext shardingContext) {
		List<MessageEntity> findList = this.messageEntityMapper.querySendMQExcetion();
		log.info("查询消息确认但是发送mq失败的（查询1分钟之前的）记录数：{}",findList);
		if(findList.isEmpty()){
			return;
		}
		for(MessageEntity find : findList){
			Message message = new Message();
			message.setMessageId(find.getMessageId());
			message.setBody(find.getBody());
			message.setDestination(find.getDestination());
			if(!StringUtils.isEmpty(find.getProperties())){
				message.setProperties(JSON.parseObject(find.getProperties(), Map.class));
			}
			this.producerService.sendMessage(message);
			MessageEntity update = new MessageEntity();
			update.setMessageId(find.getMessageId());
			update.setSendStatus(SendStatus.ALREADY_SEND.getStatus());
			update.setUpdateTime(new Date());
			this.messageEntityMapper.updateByMessageId(update);
		}
	}
}
