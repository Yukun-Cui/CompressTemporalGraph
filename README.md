# CompressTemporalGraph：时序图压缩对比实验

本仓库是 CompressTemporalGraph 主项目，包含 GPU 实现、Java CPU baseline、规范化数据集和历史实验。当前对比实验统一使用以下数据集名称：`Comm`、`MyD`、`Flickr`、`Wiki`、`Yahoo`、`Yahoo-sub`。

`Yahoo-sub` 是 Yahoo 的逻辑 workload 视图：通过硬链接复用 Yahoo 的数据文件，但所有 workload 都忽略节点 ID `0–1999`。

## 目录

| 路径 | 用途 |
| --- | --- |
| `gpu_cuda/` | GPU 主实现及候选变体；每个数据集位于 `<Dataset>/`，共享 CUDA 代码位于 `shared/` |
| `cpu_java/` | 单一 CPU baseline，支持全部六个数据集名称 |
| `data/compressed/` | 按数据集归档的压缩图和 GPU 索引 |
| `data/raw/` | 平铺存放六个原始 `.gz` 压缩包 |
| `gpu_cuda/decode_tables/` | CUDA 使用的解码查找表 |
| `third_party/` | 外部参考实现，不属于主项目构建，详见 `third_party/README.md` |

## 第三方项目

`third_party/` 下的两个项目是独立的上游仓库，各自保留自己的 `.git`、remote 和构建方式，主项目的构建不依赖它们：

| 路径 | 说明 |
| --- | --- |
| `third_party/ChronoGraph/` | 上游 Java 时序图压缩实现（`panagiotisl/evolving-graph-compression`），`cpu_java` 的参考来源 |
| `third_party/temporal_ligra/` | 基于 [jshun/ligra 的 temporal 分支](https://github.com/jshun/ligra/tree/temporal) 自行修改的版本 |

## 路径约定

运行 CUDA 或 Java 时可设置：

```bash
export TGA_COMPRESS_ROOT=/path/to/repo-root
```

未设置时，CUDA 从算法目录使用相对路径，Java 会从当前目录向上自动查找同时含 `data/` 和 `gpu_cuda/` 的目录。

## GPU 示例

```bash
cd gpu_cuda/Comm
nvcc -std=c++17 -O3 -arch=sm_75 bfs.cu -o bfs
./bfs
```

算法文件统一为：`bfs.cu`、`connected_components.cu`、`degree_centrality.cu`、`graph_coloring.cu`、`hits.cu`、`neighbor_access.cu`、`pagerank.cu`。

## CPU baseline 示例

```bash
cd cpu_java
mvn test
mvn exec:java -Dexec.args="Comm BFS"
mvn exec:java -Dexec.args="Yahoo-sub PageRank"
```

workload 名称为 `RA`、`BFS`、`CC`、`PR`、`HITS`、`DC`、`GC`。缺少压缩文件时 runner 会自动创建 `data/compressed/<Dataset>/`，并读取 `data/raw/<Dataset>.raw.txt.gz` 重新准备数据。

数据文件布局及 Yahoo/Yahoo-sub 的硬链接关系见 `data/README.md`。CUDA 候选实现与主线放在同一数据集目录，通过 `<算法>_<备注>.cu` 命名区分，详情见 `gpu_cuda/README.md`。第三方项目的构建方式见各自目录下的 README。
