package com.yanghui.elephant.store.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

@Data
@EqualsAndHashCode(callSuper=false)
@ToString(callSuper=true)
@TableName("t_message")
public class MessageEntity extends Model<MessageEntity>{

	private static final long serialVersionUID = 1L;
	
	@TableId(value="id", type= IdType.AUTO)
	private Long id;
	
	@TableField("message_id")
	private String messageId;
	
	private String group;
	
	private String destination;
	
	private byte[] body;
	
	private Integer status;
	
	private String properties;
	
	@TableField("create_time")
	private Date createTime;
	
	@TableField("update_time")
	private Date updateTime;

	@Override
	protected Serializable pkVal() {
		return this.id;
	}

}
