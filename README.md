# elephant

可靠事件系统，用于保证消息生产者生产的消息能可靠的发送出去，即本地事务执行成功，消息也一定要发送成功。

## 初衷

无论是SOA系统还是微服务系统，只要是分布式系统就会遇到分布式事务问题，目前业界解决分布式事务问题都是基于BASE理论，保证数据的最终一致性；具体的方案如补偿模式，可靠事件模式和TCC型事务等，而本系统就是基于可靠事件的一种实现。

## 原理图

![image](https://github.com/yanghuijava/elephant/blob/master/screenshots/%E4%BA%8B%E5%8A%A1%E6%B6%88%E6%81%AF.png)