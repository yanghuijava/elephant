package com.yanghui.elephant.server.job;

import java.util.List;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.yanghui.elephant.store.entity.MessageEntity;
import com.yanghui.elephant.store.mapper.MessageEntityMapper;

@Component
@Log4j2
public class TransactionCheckJob implements SimpleJob{
	
	@Autowired
	private MessageEntityMapper messageEntityMapper;

	@Override
	public void execute(ShardingContext shardingContext) {
		log.info("查询没有完成的事务消息（查询1分钟之前的）");
		List<MessageEntity> findList = this.messageEntityMapper.queryTransactionNotComplete();
		if(findList.isEmpty()){
			return;
		}
	}
}
