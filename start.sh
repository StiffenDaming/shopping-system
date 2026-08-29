#!/bin/bash
# ============================================
# 零号藏馆系统 - 一键启动脚本
# 用法: ./start.sh [backend|frontend|all]
# ============================================

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
SETTINGS_FILE="${ROOT_DIR}/maven-settings.xml"

# 生成 Maven settings.xml（含代理和阿里云镜像）
generate_settings() {
    cat > "$SETTINGS_FILE" << 'XML'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <proxies>
    <proxy>
      <id>proxy-http</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>127.0.0.1</host>
      <port>18080</port>
    </proxy>
    <proxy>
      <id>proxy-https</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>127.0.0.1</host>
      <port>18080</port>
    </proxy>
  </proxies>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
XML
}

start_backend() {
    echo ">>> 启动后端 (SpringBoot :8080)..."
    cd "$ROOT_DIR/backend"
    if [ ! -f "$SETTINGS_FILE" ]; then
        generate_settings
    fi
    mvn spring-boot:run -s "$SETTINGS_FILE"
}

start_frontend() {
    echo ">>> 启动前端 (Vite :5173)..."
    cd "$ROOT_DIR/frontend"
    npm run dev
}

case "${1:-all}" in
    backend)
        start_backend
        ;;
    frontend)
        start_frontend
        ;;
    all)
        echo ">>> 启动零号藏馆系统..."
        generate_settings
        start_backend &
        sleep 8
        start_frontend &
        echo ""
        echo "========================================"
        echo "  系统已启动!"
        echo "  前端: http://localhost:5173"
        echo "  后端: http://localhost:8080"
        echo "  数据库: Docker MySQL :3307"
        echo "  账号: admin/123456 (管理员)"
        echo "        user/123456  (普通用户)"
        echo "========================================"
        wait
        ;;
    *)
        echo "用法: $0 [backend|frontend|all]"
        exit 1
        ;;
esac
