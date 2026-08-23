# ===== 构建阶段 =====
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=build /app/target/shopping-1.0.0.jar app.jar

# 时区
ENV TZ=Asia/Shanghai

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
