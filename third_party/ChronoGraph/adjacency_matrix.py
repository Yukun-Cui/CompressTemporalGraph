import gzip
from matplotlib import pyplot as plt
import networkx as nx
import numpy as np
import sys

max_node = 0;

with gzip.open(sys.argv[1], 'rt') as f:
  header = True
  for line in f:
    if header:
      header = False
      continue
    splits = line.strip().split()
    source = splits[0]
    target = splits[1]
    if int(source) > max_node:
      max_node = int(source)
    if int(target) > max_node:
      max_node = int(target)

pixels = 1600
divisor = max_node / pixels
array = np.full((pixels, pixels), 0)


with gzip.open(sys.argv[1], 'rt') as f:
  header = True
  for line in f:
    if header:
      header = False
      continue
    splits = line.strip().split()
    source = splits[0]
    target = splits[1]
    array[min(int(int(source) / divisor), (pixels - 1))][min(int(int(target)/divisor), (pixels - 1))] = 1
    

plt.axis('off')
plt.imshow(array, aspect='equal')
plt.show()
