package com.yanghui.elephant.register;

import com.yanghui.elephant.register.listener.IZKReconnectionListener;

public interface IRegisterCenter {
	
	void init();
	
	void destroy();
	
	void addZKReconnectionListener(IZKReconnectionListener zkReconnectionListener);
}
