# 后端构建阶段（使用国内镜像代理）
FROM docker.m.daocloud.io/library/maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源代码
COPY src ./src

# 打包（跳过测试）
RUN mvn clean package -DskipTests

# 运行阶段（使用更小的 JDK 镜像）
FROM docker.m.daocloud.io/library/amazoncorretto:21
WORKDIR /app
COPY --from=build /app/target/AIagent-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8123
# 使用腾讯云环境配置启动
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=tencentcloud"]
