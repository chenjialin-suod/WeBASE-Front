FROM java:8

MAINTAINER qkd90

RUN mkdir -p /webase-front/server
RUN mkdir -p /webase-front/server/logs
RUN mkdir -p /webase-front/server/temp


WORKDIR /webase-front/server

ENV SERVER_PORT=5002
# 声明服务运行在9999端口
EXPOSE ${SERVER_PORT}

ADD ./target/webase-front.jar ./app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dserver.port=${SERVER_PORT}", \
            # 应用名称 如果想区分集群节点监控 改成不同的名称即可
#            "-Dskywalking.agent.service_name=ruoyi-server", \
#            "-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar", \
            "-jar", "app.jar"]

