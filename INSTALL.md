### 1、安装 Docker 及 docker-compose
- [Windows 安装 Docker](https://www.runoob.com/docker/windows-docker-install.html)
- [MacOS 安装 Docker](https://www.runoob.com/docker/macos-docker-install.html)

### 2、安装中间件
进入项目 docker 目录，执行 docker-compose up -d，等待镜像下载，容器启动。
可在 docker dashboard 查看相关容器：
![middleware](https://gitee.com/sancijun/pictures/raw/master/pics/image-20220417223354309.png)
- mysql: 127.0.0.1:13306    username: root    password:（空） 
- redis: 127.0.0.1：:6379    username: （空）   password:（空）
- rocketmq-broker: 127.0.0.1:10909
- rocketmq-namesrv: 127.0.0.1:9876
- rocketmq-console: 127.0.0.1:8081 （访问 127.0.0.1:8081 即可查看 RocketMQ 控制台）

### 3、启动 austin-web
修改 austin-web/src/main/resources/application.properties 配置，打包启动即可

### 4、启动 austin-admin
进入 austin-admin 目录，执行一下命令
```bash
# 安装依赖
npm i
# 打开服务
npm start
```
访问 localhost:3000
![austin-admin](https://gitee.com/sancijun/pictures/raw/master/pics/image-20220417223858319.png)