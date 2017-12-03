package com.yanghui.elephant.remoting;

import java.util.List;

public interface RemotingClient extends RemotingService{
	
	public void updateRegisterCenterAddressList(final List<String> addrs);

    public List<String> getRegisterCenterAddressList();
    

}
