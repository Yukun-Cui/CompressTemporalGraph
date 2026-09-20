# rm -f ../inputs/*.compressed
# ./encoder -s ../inputs/rMatGraph_J_5_100 ../inputs/rMatGraph_J_5_100.compressed
# ./encoder -s -w ../inputs/rMatGraph_WJ_5_100 ../inputs/rMatGraph_WJ_5_100.compressed
# ./encoder -s -w -t ../inputs/simple_temporal.txt ../inputs/simple_temporal.txt.compressed

make cleansrc

# # 1) OPENMP + BYTE
# make clean && make BFS OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
# rm -f ../inputs/*.compressed
# ./encoder -s ../inputs/rMatGraph_J_5_100 ../inputs/rMatGraph_J_5_100.compressed
# ./BFS -s ../inputs/rMatGraph_J_5_100
# ./BFS -s -c ../inputs/rMatGraph_J_5_100.compressed

# # 2) OPENMP + NIBBLE
# make clean && make BFS OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
# rm -f ../inputs/*.compressed
# ./encoder -s ../inputs/rMatGraph_J_5_100 ../inputs/rMatGraph_J_5_100.compressed
# ./BFS -s ../inputs/rMatGraph_J_5_100
# ./BFS -s -c ../inputs/rMatGraph_J_5_100.compressed

# # 3) OPENMP + BYTERLE (default)
# make clean && make BFS OPENMP=1 && make encoder OPENMP=1
# rm -f ../inputs/*.compressed
# ./encoder -s ../inputs/rMatGraph_J_5_100 ../inputs/rMatGraph_J_5_100.compressed
# ./BFS -s ../inputs/rMatGraph_J_5_100
# ./BFS -s -c ../inputs/rMatGraph_J_5_100.compressed

# # 4) PD + OPENMP + BYTE
# make clean && make BFS PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
# rm -f ../inputs/*.compressed
# ./encoder -s ../inputs/rMatGraph_J_5_100 ../inputs/rMatGraph_J_5_100.compressed
# ./BFS -s ../inputs/rMatGraph_J_5_100
# ./BFS -s -c ../inputs/rMatGraph_J_5_100.compressed

# # 5) PD + OPENMP + NIBBLE
# make clean && make BFS PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
# rm -f ../inputs/*.compressed
# ./encoder -s ../inputs/rMatGraph_J_5_100 ../inputs/rMatGraph_J_5_100.compressed
# ./BFS -s ../inputs/rMatGraph_J_5_100
# ./BFS -s -c ../inputs/rMatGraph_J_5_100.compressed

# # 6) PD + OPENMP + BYTERLE
# make clean && make BFS PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
# rm -f ../inputs/*.compressed
# ./encoder -s ../inputs/rMatGraph_J_5_100 ../inputs/rMatGraph_J_5_100.compressed
# ./BFS -s ../inputs/rMatGraph_J_5_100
# ./BFS -s -c ../inputs/rMatGraph_J_5_100.compressed

# # 1) OPENMP + BYTE
# make clean && make BellmanFord OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
# ./encoder -s -w ../inputs/rMatGraph_WJ_5_100 ../inputs/rMatGraph_WJ_5_100.compressed
# ./BellmanFord -s ../inputs/rMatGraph_WJ_5_100
# ./BellmanFord -s -c ../inputs/rMatGraph_WJ_5_100.compressed

# # 2) OPENMP + NIBBLE
# make clean && make BellmanFord OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
# ./encoder -s -w ../inputs/rMatGraph_WJ_5_100 ../inputs/rMatGraph_WJ_5_100.compressed
# ./BellmanFord -s ../inputs/rMatGraph_WJ_5_100
# ./BellmanFord -s -c ../inputs/rMatGraph_WJ_5_100.compressed

# # 3) OPENMP + BYTERLE (default)
# make clean && make BellmanFord OPENMP=1 && make encoder OPENMP=1
# ./encoder -s -w ../inputs/rMatGraph_WJ_5_100 ../inputs/rMatGraph_WJ_5_100.compressed
# ./BellmanFord -s ../inputs/rMatGraph_WJ_5_100
# ./BellmanFord -s -c ../inputs/rMatGraph_WJ_5_100.compressed

# # 4) PD + OPENMP + BYTE
# make clean && make BellmanFord PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
# ./encoder -s -w ../inputs/rMatGraph_WJ_5_100 ../inputs/rMatGraph_WJ_5_100.compressed
# ./BellmanFord -s ../inputs/rMatGraph_WJ_5_100
# ./BellmanFord -s -c ../inputs/rMatGraph_WJ_5_100.compressed

# # 5) PD + OPENMP + NIBBLE
# make clean && make BellmanFord PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
# ./encoder -s -w ../inputs/rMatGraph_WJ_5_100 ../inputs/rMatGraph_WJ_5_100.compressed
# ./BellmanFord -s ../inputs/rMatGraph_WJ_5_100
# ./BellmanFord -s -c ../inputs/rMatGraph_WJ_5_100.compressed

# # 6) PD + OPENMP + BYTERLE
# make clean && make BellmanFord PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
# ./encoder -s -w ../inputs/rMatGraph_WJ_5_100 ../inputs/rMatGraph_WJ_5_100.compressed
# ./BellmanFord -s ../inputs/rMatGraph_WJ_5_100
# ./BellmanFord -s -c ../inputs/rMatGraph_WJ_5_100.compressed