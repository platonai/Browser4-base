# Coworker 定时任务配置指南

## 概述

本指南介绍如何为 `coworker.sh` 脚本设置定时任务，使其每 30 秒执行一次，并在已经运行时自动跳过。

---

## 方案一：使用守护进程（推荐）

使用 `coworker-daemon.sh` 脚本管理后台守护进程。

### 安装和启动

```bash
# 使脚本可执行
chmod +x /path/to/bin/copilot/coworker-daemon.sh

# 启动守护进程
/path/to/bin/copilot/coworker-daemon.sh start

# 查看状态
/path/to/bin/copilot/coworker-daemon.sh status

# 停止守护进程
/path/to/bin/copilot/coworker-daemon.sh stop

# 重启守护进程
/path/to/bin/copilot/coworker-daemon.sh restart
```

### 关键特性

- ✅ 每 30 秒执行一次
- ✅ 自动检测正在运行的实例，避免并发
- ✅ 支持优雅关闭和强制杀死
- ✅ 详细的日志记录
- ✅ 锁文件机制防止重复运行

### 日志位置

```
$AppHome/logs/coworker/coworker-daemon.log
```

### 锁文件位置

```
$TMPDIR/coworker-locks/coworker.lock
```

---

## 方案二：使用 Systemd 服务（Linux）

创建 systemd 服务文件以在系统启动时自动启动。

### 1. 创建服务文件

创建 `/etc/systemd/system/coworker.service`：

```ini
[Unit]
Description=Coworker Daemon Service
After=network.target

[Service]
Type=simple
User=your_username
WorkingDirectory=/path/to/Browser4-4.6
ExecStart=/path/to/Browser4-4.6/bin/copilot/coworker-daemon.sh start
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 2. 启用和启动服务

```bash
# 重新加载 systemd 配置
sudo systemctl daemon-reload

# 启用开机自启
sudo systemctl enable coworker.service

# 启动服务
sudo systemctl start coworker.service

# 查看状态
sudo systemctl status coworker.service

# 查看日志
sudo journalctl -u coworker.service -f
```

---

## 方案三：使用 Cron（通用）

使用 cron 定时任务每分钟检查一次是否需要运行。

### 1. 使脚本可执行

```bash
chmod +x /path/to/bin/copilot/coworker-cron.sh
```

### 2. 编辑 Crontab

```bash
crontab -e
```

添加以下行（每分钟执行一次检查）：

```cron
* * * * * /path/to/Browser4-4.6/bin/copilot/coworker-cron.sh
```

### 3. 查看已安装的 Cron 任务

```bash
crontab -l
```

### 日志位置

```
$AppHome/logs/coworker/coworker-cron.log
```

---

## 实现细节

### 锁文件机制

脚本使用文件锁来防止并发执行：

```bash
LOCK_FILE=$TMPDIR/coworker-locks/coworker.lock
```

- 执行前检查锁文件是否存在
- 如果存在且进程仍在运行，则跳过本次执行
- 执行后删除锁文件
- 如果锁文件超过 35 秒（间隔 + 缓冲），认为是陈旧的锁，自动删除

### 执行流程

```
检查锁文件 → 获取锁 → 执行 coworker.sh → 释放锁 → 等待 30s → 重复
     ↓ (已运行)
     └─→ 跳过本次执行 → 等待 30s → 重复
```

---

## 常见问题

### Q1: 脚本没有执行？

**A:** 检查以下几点：

1. 脚本是否可执行：`ls -la /path/to/coworker-daemon.sh`
2. 检查日志文件：`tail -f $AppHome/logs/coworker/coworker-daemon.log`
3. 检查 coworker.sh 是否可执行：`ls -la /path/to/coworker.sh`
4. 检查 `copilot` 命令是否在 PATH 中：`which copilot`

### Q2: 如何查看执行历史？

**A:** 查看日志文件：

```bash
tail -100 /path/to/logs/coworker/coworker-daemon.log
```

### Q3: 如何停止定时任务？

**A:** 使用相应的方案停止：

```bash
# 守护进程方案
/path/to/bin/copilot/coworker-daemon.sh stop

# Systemd 方案
sudo systemctl stop coworker.service

# Cron 方案
crontab -e  # 删除对应的行
```

### Q4: 如何修改执行间隔（不是 30 秒）？

**A:**

守护进程方案，编辑 `coworker-daemon.sh`，修改：
```bash
INTERVAL=30  # 改为你需要的秒数
```

Cron 方案，修改 crontab 表达式：
```cron
*/1 * * * *   # 每分钟
*/5 * * * *   # 每 5 分钟
*/30 * * * *  # 每 30 分钟
0 * * * *     # 每小时
```

---

## 故障排查

### 检查锁文件状态

```bash
# 查看当前锁文件
ls -la $TMPDIR/coworker-locks/

# 手动清除锁文件（如果需要）
rm -f $TMPDIR/coworker-locks/coworker.lock
```

### 验证进程状态

```bash
# 查看运行中的 coworker 相关进程
ps aux | grep coworker

# 查看是否有僵尸进程
ps aux | grep defunct | grep coworker
```

### 检查文件权限

```bash
# 确保脚本可执行
chmod +x /path/to/bin/copilot/coworker*.sh

# 确保日志目录可写
chmod 755 /path/to/logs/coworker
```

---

## 推荐方案选择

| 方案 | 优势 | 劣势 | 适用场景 |
|------|------|------|---------|
| **守护进程** | 精确控制，低资源占用，支持所有系统 | 需手动启动 | 开发、测试环境 |
| **Systemd** | 自动启动，系统集成好，便于管理 | 仅限 Linux | 生产 Linux 环境 |
| **Cron** | 通用，系统原生支持 | 精度较低（分钟级），额外进程开销 | 简单场景、通用环保 |

**推荐使用守护进程方案**，因为它提供最好的控制力和最低的系统开销。

---

## 相关文件

- `coworker.sh` - 主任务脚本
- `coworker-daemon.sh` - 守护进程脚本
- `coworker-cron.sh` - Cron 脚本（可选）


