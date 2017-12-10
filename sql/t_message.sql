/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50626
Source Host           : localhost:3306
Source Database       : elephant

Target Server Type    : MYSQL
Target Server Version : 50626
File Encoding         : 65001

Date: 2017-12-10 22:56:36
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `message_id` varchar(100) DEFAULT NULL COMMENT '消息ID',
  `group` varchar(255) DEFAULT NULL COMMENT '发送者分组名称',
  `destination` varchar(500) DEFAULT NULL COMMENT 'mq地址',
  `body` blob COMMENT '消息内容',
  `status` tinyint(4) DEFAULT NULL COMMENT '消息状态\r\n100：确认中\r\n101：已回滚\r\n102：已确认\r\n103：已失效',
  `send_status` tinyint(4) DEFAULT NULL COMMENT '消息发送到mq的状态\r\n1：待发送\r\n2：已发送',
  `transaction` int(11) DEFAULT NULL COMMENT '是否是事务消息\r\n0：不是\r\n1：是',
  `properties` varchar(1024) DEFAULT NULL COMMENT '消息熟悉',
  `remark` varchar(2048) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8;
