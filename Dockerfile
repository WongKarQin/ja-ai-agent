# 使用预装 Maven 和 JDK21 的镜像
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

# 只复制必要的源代码和配置文件
COPY pom.xml .
COPY src ./src

# 使用 Maven 执行打包
RUN mvn clean package -DskipTests

# 运行阶段（使用更小的 JDK 镜像）
FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /app/target/AIagent-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8123

# 使用生产环境配置启动应用
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=tencentcloud"]
