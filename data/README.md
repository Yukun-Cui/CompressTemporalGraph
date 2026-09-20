# 数据目录

压缩数据与原始数据分开存放：

```text
data/
├── compressed/
│   ├── Comm/
│   ├── MyD/
│   ├── Flickr/
│   ├── Wiki/
│   ├── Yahoo/
│   └── Yahoo-sub/
└── raw/
    ├── Comm.raw.txt.gz
    ├── MyD.raw.txt.gz
    ├── Flickr.raw.txt.gz
    ├── Wiki.raw.txt.gz
    ├── Yahoo.raw.txt.gz
    └── Yahoo-sub.raw.txt.gz
```

`compressed/<Dataset>/` 包含 `<Dataset>.graph`、`<Dataset>.offsets`、`<Dataset>.timestamps`、`<Dataset>.efindex`、`<Dataset>.properties`，以及 `graph_lower_bits.bin`、`graph_upper_bits.bin`、`timestamp_lower_bits.bin`、`timestamp_upper_bits.bin` 四个 GPU 索引文件。个别数据集还包含辅助文件，例如 `Yahoo.outdegrees.bin`。

`raw/` 直接平铺六个原始 `.gz`。

`Yahoo-sub` 是 Yahoo 的逻辑 workload 视图，不保存另一份物理图：其原始包和九个压缩/索引文件均通过硬链接复用 Yahoo 数据，但文件 basename 保持为 `Yahoo-sub`。
