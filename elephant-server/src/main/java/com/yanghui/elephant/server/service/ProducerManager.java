package com.yanghui.elephant.server.service;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ProducerManager {
	
	private static final long LOCK_TIMEOUT_MILLIS = 3000;
    private final Lock groupChannelLock = new ReentrantLock();
    
    private final Map<String /* group name */, Set<Channel>> groupChannelTable = new HashMap<String, Set<Channel>>();
	
    private ScheduledExecutorService removeExpireKeyExecutor;
    
    @PostConstruct
    public void initMethod(){
    	ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("producer-manager-%d")
        .setDaemon(true)
        .build();
    	this.removeExpireKeyExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    	this.removeExpireKeyExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				log.debug("groupChannelTable：{}",groupChannelTable);
				ProducerManager.this.scanNotActiveChannel();
			}
		}, 1000 * 10, 1000 * 10, TimeUnit.MILLISECONDS);
    }
    @PreDestroy  
    public void destroyMethod() {  
        this.removeExpireKeyExecutor.shutdown();
    }
    
    public Map<String, Set<Channel>> getGroupChannelTable(){
    	Map<String, Set<Channel>> newGroupChannelTable = new HashMap<String, Set<Channel>>();
    	try {
            if (this.groupChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            	newGroupChannelTable.putAll(groupChannelTable);
            }
        } catch (InterruptedException e) {
            log.error("getGroupChannelTable", e);
        }finally {
            groupChannelLock.unlock();
        }
        return newGroupChannelTable;
    }
    
    public void scanNotActiveChannel(){
    	try {
    		 if (this.groupChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
    			 for(Entry<String,Set<Channel>> entry : groupChannelTable.entrySet()){
    				 if(CollectionUtils.isEmpty(entry.getValue())){
    					 continue;
    				 }
    				 Iterator<Channel> it = entry.getValue().iterator();
    				 while(it.hasNext()){
    					 Channel c = it.next();
    					 if(!c.isActive()){
    						 it.remove();
    					 }
    				 }
    			 }
    		 }else {
    			 log.warn("ProducerManager scanNotActiveChannel lock timeout");
    		 }
		} catch (Exception e) {
			log.error("scanNotActiveChannel",e);
		}finally{
			this.groupChannelLock.unlock();
		}
    }
    
    public void registerProducer(final String group,Channel channel){
    	try {
    		if (this.groupChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
        		Set<Channel> set = this.groupChannelTable.get(group);
        		if(set == null){
        			set = new HashSet<Channel>();
        			this.groupChannelTable.put(group, set);
        		}
        		set.add(channel);
        	}
		} catch (Exception e) {
			log.error("registerProducer：{}",e);
		}finally{
			this.groupChannelLock.unlock();
		}
    }
}
