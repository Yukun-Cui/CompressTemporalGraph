# CUDA GPU 实现

主线目录结构为 `gpu_cuda/<Dataset>/*.cu`。数据集名称固定为 `Comm`、`MyD`、`Flickr`、`Wiki`、`Yahoo`、`Yahoo-sub`。

## 目录约定

- `shared/*.cu`：七种算法的共享实现。
- `shared/dataset_config_base.cuh`：资源路径、压缩图路径和算法默认参数。
- `shared/path_config.cuh`：统一解析 `TGA_COMPRESS_ROOT`，避免机器相关绝对路径。
- `decode_tables/`：`gammaInA.txt`、`zetaInA.txt` 和 `gamma8192.txt` 解码查找表。
- `<Dataset>/dataset_config.cuh`：只保存该数据集不同于默认值的配置。
- `<Dataset>/*.cu`：通常是包含配置与共享实现的短入口文件。
- `<Dataset>/<算法>_<备注>.cu`：尚未合入主线的候选或实验实现；不带备注的标准算法文件才是主线入口。

目前仅保留三份确有代码差异的专用实现：

- `Flickr/neighbor_access.cu`：使用 `__ldg` 优化 Gamma/Zeta 与压缩数据读取，将引用、非引用节点分阶段访问，用显式状态栈解码引用链，并收缩非引用节点遍历中的冗余局部变量。
- `Wiki/bfs.cu`：保留 Wiki 历史专用的引用块遍历和节点编号处理。
- `Wiki/neighbor_access.cu`：保留 Wiki 专用的重边初值与节点边界处理。

其余算法统一复用 `shared/`，避免同一份 CUDA 主体在各数据集目录重复维护。

## 候选实现

以下文件与主线放在同一数据集目录，通过下划线备注区分，均未经过本轮编译或性能验证：

- `Flickr/neighbor_access_shared_lookup.cu`：共享查找表尝试，存在提前返回和同步路径问题。
- `Flickr/neighbor_access_next_value_rewrite.cu`：重写三路邻居合并状态，状态推进尚未验证，查表读取也退回了普通全局内存访问。
- `Wiki/neighbor_access_dynamic_reference_split.cu`：动态任务分配并拆分引用节点与普通节点。
- `Yahoo/neighbor_access_dynamic_scheduler.cu`：使用全局任务索引动态分配节点。
- `Yahoo/neighbor_access_supernode_warp.cu`：两个 warp 组成的超级节点时间戳/邻居流水线原型。
- `Yahoo/neighbor_access_supernode_block.cu`：使用一个 block 和共享环形队列的超级节点原型。

两个 Yahoo 超级节点原型仍硬编码节点 `234`，引用节点分支没有完成，跨 warp/block 的共享状态同步也未验证。需要使用主线时请选择不带下划线备注的 `neighbor_access.cu`。

## 数据集差异

- `Wiki`：有效图节点从 1 开始，重边解码初值为 0，PageRank 和 HITS 各迭代 100 次。
- `Yahoo-sub`：读取 `data/compressed/Yahoo/`，但通过 `ACTIVE_VERTEX_BEGIN=2000` 忽略节点 ID `0–1999`；BFS 从 2001 开始。
- 其他数据集使用公共默认参数，只在配置中定义名称、最大节点和最小时间戳。

资源路径都以仓库根目录为基准。未设置环境变量时，默认假定程序从 `gpu_cuda/<Dataset>/` 启动；从其他工作目录使用时，应通过 `TGA_COMPRESS_ROOT` 指定仓库根目录。
