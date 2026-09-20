#pragma once

// Yahoo-sub is a workload view that ignores node IDs 0-1999.
#define DATASET_NAME "Yahoo-sub"
#define DATASET_STORAGE_NAME "Yahoo"
#define ACTIVE_VERTEX_BEGIN 2000
#define BFS_START_VERTEX 2001
#define CONNECTED_COMPONENTS_VERTEX_BEGIN 2000
#define MAX_LABEL 102867214
#define MAX_Vertex 102867263
#define minTimestamp 1206214540

#include "../shared/dataset_config_base.cuh"
