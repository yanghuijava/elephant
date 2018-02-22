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

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.google.common.collect.Lists;
import com.yanghui.elephant.common.constant.RequestCode;
import com.yanghui.elephant.common.protocol.header.CheckTransactionStateRequestHeader;
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

	@Override
	public void execute(ShardingContext shardingContext) {
		List<MessageEntity> findList = this.messageEntityMapper.queryTransactionNotComplete();
		log.info("查询没有完成的事务消息（查询1分钟之前的）记录数：{}",findList);
		if(findList.isEmpty()){
			return;
		}
		try {
			for(MessageEntity entity : findList){
				
				CheckTransactionStateRequestHeader requestHeader = new CheckTransactionStateRequestHeader();
				
				requestHeader.setMessageId(entity.getMessageId());
				requestHeader.setDestination(entity.getDestination());
				requestHeader.setProducerGroup(entity.getGroup());
				if(!StringUtil.isNullOrEmpty(entity.getProperties())){
					requestHeader.setProperties(entity.getProperties());
				}
				
				RemotingCommand request = RemotingCommand.buildRequestCmd(requestHeader,RequestCode.CHECK_TRANSACTION);
				request.setBody(entity.getBody());
				
				sentToClient(request);
			}
		} catch (Exception e) {
			log.error("回查发送异常：{}",e);
		}
	}
	
	private void sentToClient(RemotingCommand request){
		Map<String,Set<Channel>> groupChannelTable = this.producerManager.getGroupChannelTable();
		
		CheckTransactionStateRequestHeader requestHeader = (CheckTransactionStateRequestHeader)request.getCommandCustomHeader();
		if(groupChannelTable.get(requestHeader.getProducerGroup()) == null || 
				CollectionUtils.isEmpty(groupChannelTable.get(requestHeader.getProducerGroup()))){
			return;
		}
		List<Channel> channels = Lists.newArrayList(groupChannelTable.get(requestHeader.getProducerGroup()));
		Channel c = channels.get(new Random().nextInt(channels.size()));
		if(c.isActive()){
			c.writeAndFlush(request);
		}
	}
}
