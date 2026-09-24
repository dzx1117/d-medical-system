# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 走阿里云 Maven 镜像 + 失败重试，避免直连中央仓库时下载中断
COPY settings.xml /root/.m2/settings.xml
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=3"

# 先只复制 pom，利用 Docker 层缓存避免依赖重复下载
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 以非 root 用户运行
RUN groupadd -r app && useradd -r -g app app

COPY --from=build /build/target/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

# 容器内存感知 + 时区
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
