# 前端构建阶段（使用国内镜像代理避免 Docker Hub 超时）
FROM docker.m.daocloud.io/library/node:20-alpine AS build
WORKDIR /app
COPY . .
RUN npm install --registry=https://registry.npmmirror.com
RUN npm run build

# 运行阶段 - 使用 nginx 托管静态文件
FROM docker.m.daocloud.io/library/nginx:alpine
# 复制构建产物到 nginx 静态文件目录
COPY --from=build /app/dist /usr/share/nginx/html
# 复制自定义 nginx 配置替换默认配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 曝露端口（阿里云 FC 3.0 默认侧听 9000 端口）
EXPOSE 9000
# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
