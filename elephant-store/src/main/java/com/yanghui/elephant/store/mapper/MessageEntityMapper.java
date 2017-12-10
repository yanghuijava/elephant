package com.yanghui.elephant.store.mapper;

import java.util.List;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.yanghui.elephant.store.entity.MessageEntity;

public interface MessageEntityMapper extends BaseMapper<MessageEntity>{

	/**
	 * 查询没有完成的事务消息（查询1分钟之前的）记录数
	 * @return
	 */
	List<MessageEntity> queryTransactionNotComplete();

	int updateByMessageId(MessageEntity update);

	/**
	 * 查询消息确认但是发送mq失败的（查询1分钟之前的）记录数
	 * @return
	 */
	List<MessageEntity> querySendMQExcetion();

}
