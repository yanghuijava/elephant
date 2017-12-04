package com.yanghui.elephant.common.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class IPHelper {

	private static final Logger logger = LoggerFactory.getLogger(IPHelper.class);

	private static String hostIp = "";
	
	private static final String LOCALHOST = "127.0.0.1";
	
	private IPHelper() {
		 throw new IllegalStateException("Utility class");
	 }
	
	/**
	 * <B>方法名称：获取本机Ip</B><BR>
	 * <B>概要说明：通过获取系统所有的networkInterface网络接口 然后遍历 每个网络下的InterfaceAddress组。 
	 * 获得符合<code>InetAddress instanceof Inet4Address</code> 条件的一个IpV4地址</B><BR>
	 * @return
	 */
	public static String localIp() {
		return hostIp;
	}

	public static String getRealIp() {
		String localip = null;// 本地IP，如果没有配置外网IP则返回它
		String netip = null;// 外网IP
		try {
			Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			boolean finded = false;//是否找到外网IP
			while (netInterfaces.hasMoreElements() && !finded) {
				NetworkInterface ni = netInterfaces.nextElement();
				Enumeration<InetAddress> address = ni.getInetAddresses();
				while (address.hasMoreElements()) {
					ip = address.nextElement();
					if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
							&& !ip.getHostAddress().contains(":")) {// 外网IP
						netip = ip.getHostAddress();
						finded = true;
						break;
					} else if (ip.isSiteLocalAddress()
							&& !ip.isLoopbackAddress()
							&& !ip.getHostAddress().contains(":")) {// 内网IP
						localip = ip.getHostAddress();
					}
				}
			}

			if (netip != null && !"".equals(netip)) {
				return netip;
			} else {
				return localip;
			}
		} catch (SocketException e) {
			logger.error("获取本机Ip失败:异常信息:" + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	static {
		String ip = null;
		Enumeration allNetInterfaces;
		try {
			allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces
						.nextElement();
				List<InterfaceAddress> interfaceAddress = netInterface
						.getInterfaceAddresses();
				for (InterfaceAddress add : interfaceAddress) {
					InetAddress inetAddress = add.getAddress();
					if (inetAddress != null && inetAddress instanceof Inet4Address) {
						if (LOCALHOST.equals(inetAddress.getHostAddress())) {
							continue;
						}
						ip = inetAddress.getHostAddress();
						break;
					}
				}
			}
		} catch (SocketException e) {
			logger.warn("获取本机Ip失败:异常信息:" + e.getMessage());
			throw new RuntimeException(e);
		}
		hostIp = ip;
	}

	/**
	 * <B>方法名称：获取主机第一个有效ip</B><BR>
	 * <B>概要说明：如果没有效ip，返回空串</B><BR>
	 * @return
	 */
	public static String getHostFirstIp() {
		return hostIp;
	}
}
