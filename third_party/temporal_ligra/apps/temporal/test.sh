INPUT=../../inputs/simple_temporal.txt
OUT=../../inputs/simple_temporal.txt.compressed
SRC=0
DST=4
TS=0
TE=100

make clean && make EarliestArrival OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./EarliestArrival $INPUT $SRC $TS $TE

make clean && make EarliestArrival OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./EarliestArrival $INPUT $SRC $TS $TE

make clean && make EarliestArrival OPENMP=1 && make encoder OPENMP=1
./encoder -s -w -t $INPUT $OUT
./EarliestArrival $INPUT $SRC $TS $TE

make clean && make EarliestArrival PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./EarliestArrival $INPUT $SRC $TS $TE

make clean && make EarliestArrival PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./EarliestArrival $INPUT $SRC $TS $TE

make clean && make EarliestArrival PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
./encoder -s -w -t $INPUT $OUT
./EarliestArrival $INPUT $SRC $TS $TE


make clean && make Shortest OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./Shortest $INPUT $SRC $TS $TE

make clean && make Shortest OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./Shortest $INPUT $SRC $TS $TE

make clean && make Shortest OPENMP=1 && make encoder OPENMP=1
./encoder -s -w -t $INPUT $OUT
./Shortest $INPUT $SRC $TS $TE

make clean && make Shortest PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./Shortest $INPUT $SRC $TS $TE

make clean && make Shortest PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./Shortest $INPUT $SRC $TS $TE

make clean && make Shortest PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
./encoder -s -w -t $INPUT $OUT
./Shortest $INPUT $SRC $TS $TE


make clean && make LatestDeparture OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./LatestDeparture $INPUT $DST $TS $TE

make clean && make LatestDeparture OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./LatestDeparture $INPUT $DST $TS $TE

make clean && make LatestDeparture OPENMP=1 && make encoder OPENMP=1
./encoder -s -w -t $INPUT $OUT
./LatestDeparture $INPUT $DST $TS $TE

make clean && make LatestDeparture PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./LatestDeparture $INPUT $DST $TS $TE

make clean && make LatestDeparture PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./LatestDeparture $INPUT $DST $TS $TE

make clean && make LatestDeparture PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
./encoder -s -w -t $INPUT $OUT
./LatestDeparture $INPUT $DST $TS $TE


make clean && make Fastest OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./Fastest $INPUT $SRC $TS $TE

make clean && make Fastest OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./Fastest $INPUT $SRC $TS $TE

make clean && make Fastest OPENMP=1 && make encoder OPENMP=1
./encoder -s -w -t $INPUT $OUT
./Fastest $INPUT $SRC $TS $TE

make clean && make Fastest PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./Fastest $INPUT $SRC $TS $TE

make clean && make Fastest PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./Fastest $INPUT $SRC $TS $TE

make clean && make Fastest PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
./encoder -s -w -t $INPUT $OUT
./Fastest $INPUT $SRC $TS $TE


make clean && make KCore-WeightedTemporal OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./KCore-WeightedTemporal $INPUT

make clean && make KCore-WeightedTemporal OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./KCore-WeightedTemporal $INPUT

make clean && make KCore-WeightedTemporal OPENMP=1 && make encoder OPENMP=1
./encoder -s -w -t $INPUT $OUT
./KCore-WeightedTemporal $INPUT

make clean && make KCore-WeightedTemporal PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./KCore-WeightedTemporal $INPUT

make clean && make KCore-WeightedTemporal PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./KCore-WeightedTemporal $INPUT

make clean && make KCore-WeightedTemporal PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
./encoder -s -w -t $INPUT $OUT
./KCore-WeightedTemporal $INPUT


make clean && make BC-Shortest OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./BC-Shortest $INPUT $TS $TE

make clean && make BC-Shortest OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./BC-Shortest $INPUT $TS $TE

make clean && make BC-Shortest OPENMP=1 && make encoder OPENMP=1
./encoder -s -w -t $INPUT $OUT
./BC-Shortest $INPUT $TS $TE

make clean && make BC-Shortest PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./BC-Shortest $INPUT $TS $TE

make clean && make BC-Shortest PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./BC-Shortest $INPUT $TS $TE

make clean && make BC-Shortest PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
./encoder -s -w -t $INPUT $OUT
./BC-Shortest $INPUT $TS $TE


make clean && make BC-Fastest OPENMP=1 BYTE=1 && make encoder OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./BC-Fastest $INPUT $TS $TE

make clean && make BC-Fastest OPENMP=1 NIBBLE=1 && make encoder OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./BC-Fastest $INPUT $TS $TE

make clean && make BC-Fastest OPENMP=1 && make encoder OPENMP=1
./encoder -s -w -t $INPUT $OUT
./BC-Fastest $INPUT $TS $TE

make clean && make BC-Fastest PD=1 OPENMP=1 BYTE=1 && make encoder PD=1 OPENMP=1 BYTE=1
./encoder -s -w -t $INPUT $OUT
./BC-Fastest $INPUT $TS $TE

make clean && make BC-Fastest PD=1 OPENMP=1 NIBBLE=1 && make encoder PD=1 OPENMP=1 NIBBLE=1
./encoder -s -w -t $INPUT $OUT
./BC-Fastest $INPUT $TS $TE

make clean && make BC-Fastest PD=1 OPENMP=1 && make encoder PD=1 OPENMP=1
./encoder -s -w -t $INPUT $OUT
./BC-Fastest $INPUT $TS $TE