# third_party

外部参考实现和对比基线。主项目（`cpu_java/`、`gpu_cuda/`）的构建不依赖本目录，删除或更新这里的内容不会影响主项目编译。

本目录以 vendor 方式随主仓库一起提交：两个子项目原先各自带 `.git`，已在并入主仓库时移除，因此这里只有源码快照，没有各自的提交历史。要查看历史或更新，请回到下表的上游仓库。

| 目录 | 来源 | 并入时的 commit | 用途 |
| --- | --- | --- | --- |
| `ChronoGraph/` | [panagiotisl/evolving-graph-compression](https://github.com/panagiotisl/evolving-graph-compression) (`master`) | `d52234e` | Java 时序图压缩的上游实现，`cpu_java` 的参考来源 |
| `temporal_ligra/` | [Yukun-Cui/temporal_ligra](https://github.com/Yukun-Cui/temporal_ligra) (`local-changes`) | `660fa11` | Ligra 时序图扩展，CPU 对比基线 |

## ChronoGraph

Maven 项目，需要 JDK 11：

```bash
cd ChronoGraph
mvn test
```

与主项目的 `cpu_java` 共用 `gr.uoa.di.networkanalysis` 包名，但 artifactId 不同（`evolving-graph-compression` 对 `temporal-graph-cpu-baseline`），两者独立构建，互不影响。

`src/test/resources/cbtComm-sorted.txt.gz` 是上游自带的 53 MB 测试数据，也是本仓库最大的文件。

## temporal_ligra

本目录是在 [jshun/ligra 的 `temporal` 分支](https://github.com/jshun/ligra/tree/temporal) 基础上自行修改的版本。上游 `temporal` 分支为 `75eb1a2`，自己的改动（`ligra/` 解码相关文件、`apps/temporal/` 下的 `*-Compute.C` workload 与脚本、各级 `Makefile`）已提交为 `660fa11`，保存在个人 fork 的 `local-changes` 分支上。

在 `apps/` 下编译，编译器与并行后端要求（Cilk Plus / OpenMP / icpc）见 `temporal_ligra/README.md`：

```bash
cd temporal_ligra/apps/temporal
make
```

后续修改可以直接在主仓库里提交；若要回传到 ligra 上游或个人 fork，仍需在 fork 的克隆中操作。
