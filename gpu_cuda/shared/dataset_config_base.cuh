#pragma once

#ifndef DATASET_NAME
#error "DATASET_NAME must be defined before including dataset_config_base.cuh"
#endif

#ifndef DATASET_STORAGE_NAME
#define DATASET_STORAGE_NAME DATASET_NAME
#endif

#ifndef ACTIVE_VERTEX_BEGIN
#define ACTIVE_VERTEX_BEGIN 0
#endif

#ifndef MIN_GRAPH_VERTEX
#define MIN_GRAPH_VERTEX 0
#endif

#ifndef BFS_START_VERTEX
#define BFS_START_VERTEX 1
#endif

#ifndef CONNECTED_COMPONENTS_VERTEX_BEGIN
#define CONNECTED_COMPONENTS_VERTEX_BEGIN 1
#endif

#ifndef MULTIPLE_INITIAL_VALUE
#define MULTIPLE_INITIAL_VALUE -1
#endif

#ifndef PAGERANK_ITERATIONS
#define PAGERANK_ITERATIONS 10
#endif

#ifndef HITS_ITERATIONS
#define HITS_ITERATIONS 10
#endif

#ifndef TGA_DEFAULT_COMPRESS_ROOT
#define TGA_DEFAULT_COMPRESS_ROOT "../.."
#endif

#include "path_config.cuh"

TGA_DEFINE_PATH(gamma_file_path, "gpu_cuda/decode_tables/gammaInA.txt");
TGA_DEFINE_PATH(zeta_file_path, "gpu_cuda/decode_tables/zetaInA.txt");
TGA_DEFINE_PATH(
        graph_file_path,
        "data/compressed/" DATASET_STORAGE_NAME "/" DATASET_STORAGE_NAME ".graph");
TGA_DEFINE_PATH(
        timestamps_file_path,
        "data/compressed/" DATASET_STORAGE_NAME "/" DATASET_STORAGE_NAME ".timestamps");
TGA_DEFINE_PATH(
        offsets_file_path,
        "data/compressed/" DATASET_STORAGE_NAME "/" DATASET_STORAGE_NAME ".offsets");
TGA_DEFINE_PATH(
        g_low_file_path,
        "data/compressed/" DATASET_STORAGE_NAME "/graph_lower_bits.bin");
TGA_DEFINE_PATH(
        g_upper_file_path,
        "data/compressed/" DATASET_STORAGE_NAME "/graph_upper_bits.bin");
TGA_DEFINE_PATH(
        t_low_file_path,
        "data/compressed/" DATASET_STORAGE_NAME "/timestamp_lower_bits.bin");
TGA_DEFINE_PATH(
        t_upper_file_path,
        "data/compressed/" DATASET_STORAGE_NAME "/timestamp_upper_bits.bin");
