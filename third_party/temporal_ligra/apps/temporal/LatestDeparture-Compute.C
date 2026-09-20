#define WEIGHTED_TEMPORAL 1
#include "ligra.h"

/**
 * Temporal shortest path: latest departure time, AKA "reverse foremost".
 *
 * See also: Ligra's Bellman-Ford shortest path implementation.
 */
struct LatestDeparture_F {
  intE* LatestDepartureTime;
  int* Visited;
  uintE QueryTargetVertexId;  // target vertex id for temporal query
  time_t QueryStartTime;      // start time of interval for temporal query
  time_t QueryEndTime;        // end time of interval for temporal query

  LatestDeparture_F(intE* _LatestDepartureTime, int* _Visited,
                    uintE _QueryTargetVertexId, time_t _QueryStartTime,
                    time_t _QueryEndTime)
      : LatestDepartureTime(_LatestDepartureTime),
        Visited(_Visited),
        QueryTargetVertexId(_QueryTargetVertexId),
        QueryStartTime(_QueryStartTime),
        QueryEndTime(_QueryEndTime) {}

  // update
  //
  // See Algo 2 from "Path Problems in Temporal Graphs".
  inline bool update(uintE s, uintE d, intE edge_weight, intE edge_start_time,
                     intE edge_end_time) {
    // Edge starts after before start of query interval.
    if (edge_start_time < QueryStartTime) {
      return 0;
    }

    // Update LatestDepartureTime path if found one with later departure that
    // meets temporal path constraints.
    bool is_unset = LatestDepartureTime[s] < 0;
    if (edge_end_time <= LatestDepartureTime[s] || is_unset) {
      // Path with later departure.
      if (edge_start_time > LatestDepartureTime[d]) {
        LatestDepartureTime[d] = edge_start_time;

        // Mark visited.
        //
        // NOTE: here we're assuming only one edge from u to v.
        if (Visited[d] == 0) {
          Visited[d] = 1;
          return 1;
        }
      }
    }
    return 0;
  }

  // atomic version of update
  //
  // See Algo 2 of "Path Problems in Temporal Graphs".
  inline bool updateAtomic(uintE s, uintE d, intE edge_weight,
                           intE edge_start_time, intE edge_end_time) {
    if (edge_start_time < QueryStartTime) {
      return 0;
    }

    bool is_unset = LatestDepartureTime[s] < 0;
    if (!is_unset && edge_end_time > LatestDepartureTime[s]) {
      return 0;
    }

    if (edge_start_time <= LatestDepartureTime[d]) {
      return 0;
    }

    return (writeMax(&LatestDepartureTime[d], edge_start_time) &&
            CAS(&Visited[d], 0, 1));
  }

  // cond function always returns true
  inline bool cond(uintE d) { return cond_true(d); }
};

// reset visited vertices
struct LatestDeparture_Vertex_F {
  int* Visited;
  LatestDeparture_Vertex_F(int* _Visited) : Visited(_Visited) {}
  inline bool operator()(uintE i) {
    Visited[i] = 0;
    return 1;
  }
};

template <class vertex>
void Compute(graph<vertex>& GA, commandLine P) {
  // Args needed for temporal path (latest departure time).
  uint64_t target_vertex_id = strtoull(P.getArgument(3), NULL, 10);
  time_t start_time = (time_t)strtoll(P.getArgument(2), NULL, 10);
  time_t end_time = (time_t)strtoll(P.getArgument(1), NULL, 10);

  // NOTE: The "latest departure time" algo actually only works correctly if
  // edges are reversed, which is a crucial detail that seems to have been left
  // out of the "Path Problems in Temporal Graphs".
  GA.transpose();

  std::cerr << std::endl;
  std::cerr << "num_vertices=" << GA.n << std::endl;
  std::cerr << "num_edges=" << GA.m << std::endl;
  std::cerr << "target_vertex=" << target_vertex_id << std::endl;
  std::cerr << "start_time=" << start_time << std::endl;
  std::cerr << "end_time=" << end_time << std::endl;
  std::cerr << std::endl;

  long n = GA.n;

  // initialize LatestDepartureTime to *negative* "infinity" for all but the
  // target vertex.
  intE* LatestDepartureTime = newA(intE, n);
  {
    parallel_for(long i = 0; i < n; i++) LatestDepartureTime[i] =
        -(INT_MAX / 2);
  }
  LatestDepartureTime[target_vertex_id] = end_time;

  int* Visited = newA(int, n);
  { parallel_for(long i = 0; i < n; i++) Visited[i] = 0; }

  vertexSubset Frontier(n, target_vertex_id);  // initial frontier

  while (!Frontier.isEmpty()) {
    vertexSubset output =
        edgeMap(GA, Frontier,
                LatestDeparture_F(LatestDepartureTime, Visited,
                                  target_vertex_id, start_time, end_time),
                GA.m / 20, dense_forward);
    vertexMap(output, LatestDeparture_Vertex_F(Visited));
    Frontier.del();
    Frontier = output;
  }
  /*
    // Print latest departure times to target vertex from all other vertices.
    std::cerr << "Latest Departure times:\n";
    std::cerr << "Query: [" << start_time << ", " << end_time << "]\n";
    for (long i = 0; i < n; i++) {
      std::cerr << i << " -> " << target_vertex_id << " = "
                << LatestDepartureTime[i] << std::endl;
    }
  */
  Frontier.del();
  free(Visited);
  free(LatestDepartureTime);
}
