INPUT=../../inputs/simple_temporal.txt
OUT=../../inputs/simple_temporal.txt.compressed
SRC=0
DST=4
TS=0
TE=100

make cleansrc

# make clean && make EarliestArrival-Compute OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./EarliestArrival-Compute $SRC $TS $TE $INPUT
# ./EarliestArrival-Compute -c $SRC $TS $TE $OUT

# make clean && make EarliestArrival-Compute OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./EarliestArrival-Compute $SRC $TS $TE $INPUT
# ./EarliestArrival-Compute -c $SRC $TS $TE $OUT

# make clean && make EarliestArrival-Compute OPENMP=1 && make encoder OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./EarliestArrival-Compute $SRC $TS $TE $INPUT
# ./EarliestArrival-Compute -c $SRC $TS $TE $OUT

# make clean && make EarliestArrival-Compute PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./EarliestArrival-Compute $SRC $TS $TE $INPUT
# ./EarliestArrival-Compute -c $SRC $TS $TE $OUT

# make clean && make EarliestArrival-Compute PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./EarliestArrival-Compute $SRC $TS $TE $INPUT
# ./EarliestArrival-Compute -c $SRC $TS $TE $OUT

# make clean && make EarliestArrival-Compute PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./EarliestArrival-Compute $SRC $TS $TE $INPUT
# ./EarliestArrival-Compute -c $SRC $TS $TE $OUT


# make clean && make Shortest-Compute OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Shortest-Compute $SRC $TS $TE $INPUT
# ./Shortest-Compute -c $SRC $TS $TE $OUT

# make clean && make Shortest-Compute OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Shortest-Compute $SRC $TS $TE $INPUT
# ./Shortest-Compute -c $SRC $TS $TE $OUT

# make clean && make Shortest-Compute OPENMP=1 && make encoder OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Shortest-Compute $SRC $TS $TE $INPUT
# ./Shortest-Compute -c $SRC $TS $TE $OUT

# make clean && make Shortest-Compute PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Shortest-Compute $SRC $TS $TE $INPUT
# ./Shortest-Compute -c $SRC $TS $TE $OUT

# make clean && make Shortest-Compute PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Shortest-Compute $SRC $TS $TE $INPUT
# ./Shortest-Compute -c $SRC $TS $TE $OUT

# make clean && make Shortest-Compute PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Shortest-Compute $SRC $TS $TE $INPUT
# ./Shortest-Compute -c $SRC $TS $TE $OUT


# make clean && make LatestDeparture-Compute OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./LatestDeparture-Compute $DST $TS $TE $INPUT
# ./LatestDeparture-Compute -c $DST $TS $TE $OUT

# make clean && make LatestDeparture-Compute OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./LatestDeparture-Compute $DST $TS $TE $INPUT
# ./LatestDeparture-Compute -c $DST $TS $TE $OUT

# make clean && make LatestDeparture-Compute OPENMP=1 && make encoder OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./LatestDeparture-Compute $DST $TS $TE $INPUT
# ./LatestDeparture-Compute -c $DST $TS $TE $OUT

# make clean && make LatestDeparture-Compute PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./LatestDeparture-Compute $DST $TS $TE $INPUT
# ./LatestDeparture-Compute -c $DST $TS $TE $OUT

# make clean && make LatestDeparture-Compute PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./LatestDeparture-Compute $DST $TS $TE $INPUT
# ./LatestDeparture-Compute -c $DST $TS $TE $OUT

# make clean && make LatestDeparture-Compute PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./LatestDeparture-Compute $DST $TS $TE $INPUT
# ./LatestDeparture-Compute -c $DST $TS $TE $OUT


# make clean && make Fastest-Compute OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Fastest-Compute $SRC $TS $TE $INPUT
# ./Fastest-Compute -c $SRC $TS $TE $OUT

# make clean && make Fastest-Compute OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Fastest-Compute $SRC $TS $TE $INPUT
# ./Fastest-Compute -c $SRC $TS $TE $OUT

# make clean && make Fastest-Compute OPENMP=1 && make encoder OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Fastest-Compute $SRC $TS $TE $INPUT
# ./Fastest-Compute -c $SRC $TS $TE $OUT

# make clean && make Fastest-Compute PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Fastest-Compute $SRC $TS $TE $INPUT
# ./Fastest-Compute -c $SRC $TS $TE $OUT

# make clean && make Fastest-Compute PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Fastest-Compute $SRC $TS $TE $INPUT
# ./Fastest-Compute -c $SRC $TS $TE $OUT

# make clean && make Fastest-Compute PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./Fastest-Compute $SRC $TS $TE $INPUT
# ./Fastest-Compute -c $SRC $TS $TE $OUT


# make clean && make KCore-WeightedTemporal OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./KCore-WeightedTemporal $INPUT
# ./KCore-WeightedTemporal -c $OUT

# make clean && make KCore-WeightedTemporal OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./KCore-WeightedTemporal $INPUT
# ./KCore-WeightedTemporal -c $OUT

# make clean && make KCore-WeightedTemporal OPENMP=1 && make encoder OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./KCore-WeightedTemporal $INPUT
# ./KCore-WeightedTemporal -c $OUT

# make clean && make KCore-WeightedTemporal PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./KCore-WeightedTemporal $INPUT
# ./KCore-WeightedTemporal -c $OUT

# make clean && make KCore-WeightedTemporal PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./KCore-WeightedTemporal $INPUT
# ./KCore-WeightedTemporal -c $OUT

# make clean && make KCore-WeightedTemporal PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
# rm -f ../../inputs/*.compressed
# ./encoder -w -t $INPUT $OUT
# ./KCore-WeightedTemporal $INPUT
# ./KCore-WeightedTemporal -c $OUT


make clean && make BC-Shortest-Compute OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Shortest-Compute $TS $TE $INPUT
./BC-Shortest-Compute -c $TS $TE $OUT

make clean && make BC-Shortest-Compute OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Shortest-Compute $TS $TE $INPUT
./BC-Shortest-Compute -c $TS $TE $OUT

make clean && make BC-Shortest-Compute OPENMP=1 && make encoder OPENMP=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Shortest-Compute $TS $TE $INPUT
./BC-Shortest-Compute -c $TS $TE $OUT

make clean && make BC-Shortest-Compute PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Shortest-Compute $TS $TE $INPUT
./BC-Shortest-Compute -c $TS $TE $OUT

make clean && make BC-Shortest-Compute PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Shortest-Compute $TS $TE $INPUT
./BC-Shortest-Compute -c $TS $TE $OUT

make clean && make BC-Shortest-Compute PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Shortest-Compute $TS $TE $INPUT
./BC-Shortest-Compute -c $TS $TE $OUT


make clean && make BC-Fastest-Compute OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Fastest-Compute $TS $TE $INPUT
./BC-Fastest-Compute -c $TS $TE $OUT

make clean && make BC-Fastest-Compute OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Fastest-Compute $TS $TE $INPUT
./BC-Fastest-Compute -c $TS $TE $OUT

make clean && make BC-Fastest-Compute OPENMP=1 && make encoder OPENMP=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Fastest-Compute $TS $TE $INPUT
./BC-Fastest-Compute -c $TS $TE $OUT

make clean && make BC-Fastest-Compute PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Fastest-Compute $TS $TE $INPUT
./BC-Fastest-Compute -c $TS $TE $OUT

make clean && make BC-Fastest-Compute PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Fastest-Compute $TS $TE $INPUT
./BC-Fastest-Compute -c $TS $TE $OUT

make clean && make BC-Fastest-Compute PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
rm -f ../../inputs/*.compressed
./encoder -w -t $INPUT $OUT
./BC-Fastest-Compute $TS $TE $INPUT
./BC-Fastest-Compute -c $TS $TE $OUT