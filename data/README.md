# data

数据集目录。**实际数据文件不入 git**：Yahoo 一个数据集就有约 1.03 亿个顶点，压缩产物体积远超仓库合适的大小，且绝大部分可以由 `cpu_java` 的 runner 从原始数据重新生成。仓库里只保留目录结构和本说明，`data/raw/` 与 `data/compressed/` 下的内容由 `.gitignore` 排除。

## 布局

```
data/
├── raw/                       # 六个原始 .gz 压缩包，平铺存放
│   └── <Dataset>.raw.txt.gz
└── compressed/
    └── <Dataset>/
        ├── <Dataset>.graph        # BVGraph 压缩图
        ├── <Dataset>.offsets
        ├── <Dataset>.properties
        ├── <Dataset>.timestamps
        ├── <Dataset>.efindex      # Elias-Fano 索引
        ├── graph_lower_bits.bin       # 以下四个为 GPU Elias-Fano 索引
        ├── graph_upper_bits.bin
        ├── timestamp_lower_bits.bin
        └── timestamp_upper_bits.bin
```

`<Dataset>` 取值：`Comm`、`MyD`、`Flickr`、`Wiki`、`Yahoo`。

## Yahoo-sub

`Yahoo-sub` 是 Yahoo 的逻辑 workload 视图，不是独立数据集：**所有 workload 都忽略节点 ID `0–1999`**，从 2000 号节点开始。

GPU 侧通过 `DATASET_STORAGE_NAME` 直接复用 Yahoo 的数据文件（见 `gpu_cuda/Yahoo-sub/dataset_config.cuh`：`DATASET_NAME` 为 `Yahoo-sub` 而 `DATASET_STORAGE_NAME` 为 `Yahoo`），因此不需要单独的 `data/compressed/Yahoo-sub/`。

Java 侧的 `BaselineRunner` 按 `data/compressed/Yahoo-sub/Yahoo-sub` 这一 basename 读取，所以本地需要让它指向 Yahoo 的同名文件。用硬链接复用、避免占用双份空间：

```bash
mkdir -p data/compressed/Yahoo-sub
cd data/compressed/Yahoo-sub
for ext in graph offsets properties timestamps efindex; do
    ln ../Yahoo/Yahoo.$ext Yahoo-sub.$ext
done
ln ../Yahoo/Yahoo.raw.txt.gz ../../raw/Yahoo-sub.raw.txt.gz 2>/dev/null || true
```

## 准备数据

把原始 `.gz` 放进 `data/raw/` 后，Java runner 在压缩文件缺失时会自动创建 `data/compressed/<Dataset>/` 并重新生成：

```bash
cd cpu_java
mvn exec:java -Dexec.args="Comm BFS"
```

四个 GPU Elias–Fano 索引（`*_bits.bin`）不由 Java runner 生成，需要另行准备。
