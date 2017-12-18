package com.yanghui.elephant.server.job;

import io.netty.channel.Channel;
import io.netty.util.internal.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.google.common.collect.Lists;
import com.yanghui.elephant.common.constant.MessageCode;
import com.yanghui.elephant.common.constant.RequestCode;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.server.service.ProducerManager;
import com.yanghui.elephant.store.entity.MessageEntity;
import com.yanghui.elephant.store.mapper.MessageEntityMapper;

@Component
@Log4j2
public class TransactionCheckJob implements SimpleJob{
	
	@Autowired
	private MessageEntityMapper messageEntityMapper;
	@Autowired
	private ProducerManager producerManager;

	@SuppressWarnings("unchecked")
	@Override
	public void execute(ShardingContext shardingContext) {
		List<MessageEntity> findList = this.messageEntityMapper.queryTransactionNotComplete();
		log.info("查询没有完成的事务消息（查询1分钟之前的）记录数：{}",findList);
		if(findList.isEmpty()){
			return;
		}
		try {
			for(MessageEntity entity : findList){
				Message message = new Message();
				message.setMessageId(entity.getMessageId());
				message.setBody(entity.getBody());
				message.setDestination(entity.getDestination());
				if(!StringUtil.isNullOrEmpty(entity.getProperties())){
					message.setProperties(JSON.parseObject(entity.getProperties(), Map.class));
				}
				RemotingCommand request = RemotingCommand.buildRequestCmd(message, RequestCode.SEND_MESSAGE, MessageCode.TRANSACTION_CHECK_MESSAGE);
				request.setGroup(entity.getGroup());
				sentToClient(request);
			}
		} catch (Exception e) {
			log.error("回查发送异常：{}",e);
		}
	}
	
	private void sentToClient(RemotingCommand request){
		Map<String,Set<Channel>> groupChannelTable = this.producerManager.getGroupChannelTable();
		if(groupChannelTable.get(request.getGroup()) == null || 
				CollectionUtils.isEmpty(groupChannelTable.get(request.getGroup()))){
			return;
		}
		List<Channel> channels = Lists.newArrayList(groupChannelTable.get(request.getGroup()));
		Channel c = channels.get(new Random().nextInt(channels.size()));
		if(c.isActive()){
			c.writeAndFlush(request);
		}
	}
}
