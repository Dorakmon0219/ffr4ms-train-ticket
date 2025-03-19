#!/bin/bash

# 遍历所有包含 Dockerfile 的文件夹
for dir in $(find . -type f -name 'Dockerfile' -exec dirname {} \;); do
    # 进入到该文件夹
    cd $dir

    # 检查 target 目录是否存在
    if [ -d "target" ]; then
        # 查找 target 目录中的 jar 包
        jar_file=$(find target -name "*.jar" | head -n 1)

        if [ -n "$jar_file" ]; then
            echo "构建镜像: $dir"
            # 使用 Dockerfile 构建镜像
            docker build -t $(basename $dir) .
        else
            echo "没有找到 jar 文件在 $dir 的 target 目录"
        fi
    else
        echo "没有找到 target 目录在 $dir"
    fi

    # 返回到上一级目录
    cd -
done
