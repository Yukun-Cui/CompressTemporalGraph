# Temporal Graph Java CPU Baseline

`cpu_java` 是主项目的 Java CPU 基线模块。项目使用一套 Maven 源码支持全部数据集，数据集之间的路径、节点范围和迭代次数统一配置在 `BaselineRunner` 中。

上游参考实现位于 `../third_party/ChronoGraph/`，是独立仓库，不参与本模块构建。

## 环境

- JDK 11 或更高版本
- Maven 3.8.6 或更高版本

## 数据集与 workload

支持的数据集：`Comm`、`MyD`、`Flickr`、`Wiki`、`Yahoo`、`Yahoo-sub`。

支持的 workload：

| 缩写 | 名称 |
| --- | --- |
| `RA` | Random Access |
| `BFS` | Breadth-First Search |
| `CC` | Connected Components |
| `PR` | PageRank |
| `HITS` | HITS |
| `DC` | Degree Centrality |
| `GC` | Graph Coloring |

workload 参数不区分大小写，并接受 `PageRank`、`RandomAccess` 等常用全名。

## 构建与使用

```bash
mvn test
mvn verify
mvn exec:java -Dexec.args="Comm BFS"
mvn exec:java -Dexec.args="Yahoo-sub PageRank"
```

仅准备压缩文件，不运行 workload：

```bash
mvn exec:java -Dexec.args="Comm BFS --prepare-only"
```

如果所需压缩文件不存在，runner 会从 `data/raw/<Dataset>.raw.txt.gz` 生成文件到 `data/compressed/<Dataset>/`。

## 项目根目录

默认情况下，程序从当前目录向上查找同时包含 `data/` 和 `gpu_cuda/` 的仓库根目录。也可以显式设置：

```bash
export TGA_COMPRESS_ROOT=/absolute/path/to/repo-root
```

或传入 JVM 属性：

```bash
mvn exec:java \
  -Dtga.compress.root=/absolute/path/to/repo-root \
  -Dexec.args="Comm BFS"
```

## 核心源码

```text
src/main/java/gr/uoa/di/networkanalysis/
├── BaselineRunner.java              # 数据集配置和 workload 入口
├── EvolvingMultiGraph.java          # 时序图存储、加载和查询
├── BVMultiGraph.java                # WebGraph 派生的多重图压缩实现
├── ArcListASCIIEvolvingGraph.java   # 原始边列表适配器
├── IntMultiplesSequenceIterator.java
├── Successor.java
├── TimestampComparerAggregator.java
├── ExportOutdegrees.java            # 导出节点出度的独立工具
└── example/Compress.java            # 独立压缩入口
```

## 生成文件

压缩图以 basename 为前缀生成 `.graph`、`.offsets`、`.properties`、`.timestamps` 和 `.efindex` 文件。
四个 GPU Elias–Fano 索引文件不由 Java runner 生成，保存在 `../data/compressed/<Dataset>/` 中。
