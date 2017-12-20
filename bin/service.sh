#!/usr/bin/env bash

JAR_PATH=`pwd`
JAR_NAME="elephant-server"

function service_start(){
	java -jar -Xms1024M -Xmx1024M -Xss512k -XX:PermSize=256M -XX:MaxPermSize=256M "$JAR_PATH/$JAR_NAME.jar" --logging.config=classpath:config/log4j2.xml > /dev/null 2>&1 &
}

function service_stop(){
	kill -9 `jps |grep $JAR_NAME|awk '{print $1}'` > /dev/null 2>&1 &
}

case $1 in
        "start")
                jps |grep $JAR_NAME > /dev/null
                if [ $? == 0 ];then
                    echo "$JAR_NAME is already Started"
                fi
                service_start
		sleep 1
                jps |grep $JAR_NAME > /dev/null
                if [ $? == 0 ];then
                    echo "$JAR_NAME is Started!"
                    exit 0
                else
                    echo "$JAR_NAME start Fail!"
                    exit 1
                fi
                ;;
        "stop")
                service_stop
                sleep 1
                jps |grep $JAR_NAME > /dev/null
                if [ $? == 0 ];then
                        echo "$JAR_NAME stop Fail!"
                        exit 1
                else
                        echo "$JAR_NAME is Stoped!"
                        exit 0
                fi
                ;;
        "restart")
                service_stop
                sleep 1
                jps |grep $JAR_NAME > /dev/null
                if [ $? == 0 ];then
                        echo "$JAR_NAME stop Fail!"
                        exit 1
                else
                        echo "$JAR_NAME is Stoped!"
                fi
                service_start
                jps |grep $JAR_NAME > /dev/null
                if [ $? == 0 ];then
                    echo "$JAR_NAME is Started!"
                    exit 0
                else
                    echo "$JAR_NAME start Fail!"
                    exit 1
                fi
                ;;
esac