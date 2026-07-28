#!/bin/sh
set -e

# 从 /etc/resolv.conf 获取 DNS 服务器地址（Kubernetes/CloudBase Run 内置 DNS）
DNS_SERVER=$(grep '^nameserver' /etc/resolv.conf | head -1 | awk '{print $2}')

if [ -z "$DNS_SERVER" ]; then
    echo "WARNING: No nameserver found in /etc/resolv.conf, using 127.0.0.11 as fallback"
    DNS_SERVER="127.0.0.11"
fi

echo "Using DNS resolver: $DNS_SERVER"

# 替换 nginx 配置中的 DNS_RESOLVER 占位符
sed -i "s/DNS_RESOLVER/${DNS_SERVER}/g" /etc/nginx/conf.d/default.conf

# 启动 nginx
exec nginx -g "daemon off;"
