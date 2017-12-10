package com.yanghui.elephant.server.config;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.yanghui.elephant.server.job.TransactionCheckJob;

@Configuration
public class ElasticJobConfigurer {
	
	@Resource
    private ZookeeperRegistryCenter regCenter;

    @Bean(initMethod = "init", destroyMethod = "close")
    public ZookeeperRegistryCenter regCenter(@Value("${elephant.zk-server}") final String serverList,
                                             @Value("${elephant.elastic-job-zk-namespace}") final String namespace) {
        return new ZookeeperRegistryCenter(new ZookeeperConfiguration(serverList, namespace));
    }
    
    @Bean(initMethod = "init")
    public JobScheduler registryTransactionCheckJob(TransactionCheckJob transactionCheckJob) {
        LiteJobConfiguration liteJobConfiguration = LiteJobConfiguration.newBuilder(new SimpleJobConfiguration(
                JobCoreConfiguration.newBuilder(TransactionCheckJob.class.getSimpleName(), "0 0/1 * * * ?", 1).build(),
                TransactionCheckJob.class.getCanonicalName())).overwrite(true).build();
        return new SpringJobScheduler(transactionCheckJob, regCenter, liteJobConfiguration);
    }
}
