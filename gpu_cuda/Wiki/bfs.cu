#pragma once
#include<cuda.h>
#include <curand_kernel.h>
#include "cuda_runtime.h"
#include "device_launch_parameters.h"
#include <thrust/device_vector.h>  
#include <thrust/host_vector.h>
#include <thrust/transform_scan.h>  
#include <thrust/execution_policy.h> // 包含执行策略的头文件
#include <thrust/count.h>
#include <thrust/copy.h>  
#include <thrust/fill.h>   
#include <vector>  
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <iostream>
#include <fstream>
using namespace std;
#include <stdbool.h>
#include <assert.h>
#include <chrono>  
#include "dataset_config.cuh"
#define CHECK(call)                                                        \
do {                                                                       \
    cudaError_t error_code = call;                                             \
    if (error_code != cudaSuccess) {                                          \
        printf(" Line:            %d\n",__LINE__); \
        printf(" Error code:      %d\n",error_code); \
        printf(" Error text:      %s\n",cudaGetErrorString(error_code)); \
        exit(1);                                                          \
    }                                                                    \
} while (0)


void checkError(CUresult error, std::string msg) {
    if (error != CUDA_SUCCESS) {
        printf("%s: %d\n", msg.c_str(), error);
        exit(1);
    }
}
// returns the value of the Nth bit of a byte B
#define CHECK_BIT(B,N) (1 & (B >> N))

// sets the Nth bit of a byte B to 1
#define SET_BIT(B,N) {B |= 1 << N;}

// sets the Nth bit of a byte B to 0
#define CLR_BIT(B,N) {B &= ~(1<<N);}


int H_GAMMA[256 * 256];
int H_ZETA_3[256 * 256];
#define DEFAULT_MIN_MULTIPLE_LENGTH 2
#define DEFAULT_MIN_INTERVAL_LENGTH 4
#define SEED 123
#define MY_INFINITY 0xFFFFFFFF
#define nodesPerBlockMultiplier 4
__constant__ double dampingFactor = 0.85;

__constant__ int gef_low_len;//low bits length of graph's elias-fano code 
__constant__ int tef_low_len;//low bits length of timestampe's elias-fano code 

__device__   int mostSignificantBit(uint64_t value) {
    // __builtin_clz 是 GCC 和 Clang 的内置函数，用于计算前导零的数量
    // 对于其他编译器，请寻找等价的实现
    // 注意，0的MSB是未定义的，这里应当先检查value是否为0
    return value ? 64 - __clzll(value) - 1 : 0;
}

// 定义一个结构体来组合 d_time 和 p 
struct TimeAndP {

    int p;
    int time;
};
//保存节点状态信息 128B
struct NodeState {
    int x;                  // 当前节点
    int d;
    //参考信息
    int ref;                // 参考节点的ref信息
    int cpt;                // 记录block[0...i-2]的和 

    int currblock;          // 当前块的值(当前块还有几个邻居未解码）
    int blockCount;         // 参考块总数量
    //bool valid;             // 有效位，为true则当前参考块有效
    uint64_t refOffset;    // 参考节点的偏移量
    int blockNum;           // 当前所在参考块编号
    int  ref_flag;     //与pre_refvalue一起用以记录ref_nextval状态
    int pre_refvalue;  //记录当前解码到的参考区间值（若大于其他区间值，保存这个值备用）

    // 区间信息
    int currMult;           // 当前重边
    int mlen;               // 重边长度
    int m_prev;             // 重边的上一个值
    uint64_t multOffset;   // 重边的偏移量
    int multiplesCount;     // 重边数量
    int multiplesNum;       // 当前所在重边编号

    int currInterval;       // 当前区间
    int Ilen;               // 区间长度
    int I_prev;             // 区间的上一个值
    uint64_t IOffset;      // 区间的偏移量
    int intervalCount;      // 区间数量
    int intervalNum;        // 当前所在区间编号

    int currResidual;       // 当前剩余部分
    int r_prev;             // 剩余部分的上一个值
    uint64_t residualOffset; // 剩余部分的偏移量
    int residualCount;      // 剩余部分数量
    int residualNum;        // 当前所在剩余部分编号

    int flag;               // 标志位，标志上个值的出处
};

__device__ inline uint64_t atomicCAS(uint64_t* addr, uint64_t compare, uint64_t val) {
    return atomicCAS(reinterpret_cast<unsigned long long int*>(addr),
        static_cast<unsigned long long int>(compare),
        static_cast<unsigned long long int>(val));
} 
__device__ inline uint64_t atomicMin(uint64_t* addr, uint64_t val) {
    return atomicMin(reinterpret_cast<unsigned long long int*>(addr),
        static_cast<unsigned long long int>(val));
}

__device__ __host__ uint64_t toULL(TimeAndP tp) {
    return (static_cast<uint64_t>(tp.time) << 32) | (static_cast<uint32_t>(tp.p) & 0xFFFFFFFF);
}
uint64_t __host__  __device__ u64pow(uint64_t x, uint64_t n)
{
    // printf("here readGamma 64\n");
    uint64_t y = 1;
    while (1) {
        if (n % 2 == 1)
            y *= x;
        n = n / 2;
        if (n == 0)
            break;
        x *= x;
    }
    // printf("here readGamma u64pow y:%lu\n",y);
    return y;
}
uint64_t __device__ v_func(uint64_t v1, uint64_t v2)
{
    if (v2 >= v1)
        return 2 * (v2 - v1);
    else
        return 2 * (v1 - v2) - 1;
}
uint64_t __device__  inv_v_func(uint64_t x)
{
    return x % 2 == 0 ? x >> 1 : -(x >> 1) - 1;
}
uint64_t __device__ unary_decoding(uint8_t* arr, uint64_t* s, ...)
{
    uint64_t res = 0, i = *s;
    while (CHECK_BIT(*(uint8_t*)&arr[i / 8], i % 8) == 0) {
        i++;
    }
    res = i - *s;
    *s = i + 1;
    return res;
}

uint64_t __host__ __device__ readUnary(uint8_t* arr, uint64_t* s, ...)
{
    uint64_t res = 0, i = *s;
    while (CHECK_BIT(*(uint8_t*)&arr[i / 8], 7 - i % 8) == 0) {
        i++;
    }
    res = i - *s;
    *s = i + 1;
    return res;
}


/**
 * Decodes a value, from the array "arr",
 * starting at the bit "*s", and returning the decoed values.
 */
uint64_t __device__ gamma_decoding(uint8_t* arr, uint64_t* s, ...)
{
    uint64_t cpt = *s,
        p = 0, puiss = 1,
        ind, l,
        val = 0;
    l = unary_decoding(arr, &cpt);
    if (l == 0) {
        *s = cpt;
        return 0;
    }
    ind = cpt + l - 2;
    for (p = 0; p < l; p++) {
        val += CHECK_BIT(*(uint8_t*)&arr[(ind - p) / 8], (ind - p) % 8) * puiss;
        puiss *= 2;
    }
    *s = ind + 1;
    return val;
}
//改写JAVA中readGamma
uint64_t __host__ __device__ readGamma(int* GAMMA, uint8_t* arr, uint64_t* s)
{
    //printf("here readGamma\n");
    //提取从s开始的16位bit
    // 计算起始字节的索引和位偏移
    //printf("here readGamma  %lu \n",*s);
    uint64_t byteIndex = (*s) / 8;
    uint8_t bitOffset = (*s) % 8;
    // printf("here readGamma before mask\n");

    uint64_t mask = u64pow(2, 8 - bitOffset) - 1;
    //printf("here readGamma mask\n");
    // 从 arr[byteIndex]、arr[byteIndex + 1] 和 arr[byteIndex + 2] 中提取 16 位数据
    uint16_t result = (static_cast<uint16_t>(arr[byteIndex] & mask) << 8) |
        (static_cast<uint16_t>(arr[byteIndex + 1]) & 0xFF);
    //printf("here readGamma result\n");
    // 如果位偏移不为 0，则从下一个字节中提取一些位
    if (bitOffset != 0) {
        uint8_t bitsFromNextByte = static_cast<uint8_t>(arr[byteIndex + 2] >> (8 - bitOffset));
        result = result << bitOffset;
        result |= (static_cast<uint16_t>(bitsFromNextByte));

    }

    int preComp;

    if ((preComp = GAMMA[result & 0xffff]) != 0) {
        *s += preComp >> 16;

        return preComp & 0xFFFF;
    }
    uint64_t cpt = *s,
        p = 0, puiss = 1,
        ind, l,
        val = 0;
    l = readUnary(arr, &cpt);
    //cout<< "l:"<<l<<endl;
    if (l == 0) {
        *s = cpt;
        return 0;
    }
    ind = cpt + l - 1;
    //cout<<"ind"<<ind<<endl;
    val += u64pow(2, l);
    for (p = 0; p < l; p++) {
        //cout<<"ind-p"<<ind-p<<endl;
        //	cout<< CHECK_BIT( *(uint8_t *) &arr[ (ind - p) / 8],7- (ind - p) % 8)<<endl;
        val += CHECK_BIT(*(uint8_t*)&arr[(ind - p) / 8], 7 - (ind - p) % 8) * puiss;
        puiss *= 2;
    }
    //cout<<"end"<<endl;
    *s = ind + 1;
    val--;
    return val;
}

//重新分配内存
__device__ void* device_realloc(void* original, size_t originalSize, size_t newSize) {
    // Step 1: Allocate new memory block with the new size
    void* newBlock = malloc(newSize);
    if (newBlock == NULL) {
        return NULL; // Allocation failed
    }

    // Step 2: Copy existing data to the new block
    if (original != NULL && originalSize > 0) {
        size_t copySize = originalSize < newSize ? originalSize : newSize; // Min of originalSize and newSize
        memcpy(newBlock, original, copySize);
    }

    // Step 3: Free the original memory block
    free(original);

    // Step 4: Return the new memory block pointer
    return newBlock;
}



class SimpleSelect {
public:
    __device__ SimpleSelect(uint8_t* upperBits, uint64_t length)
        : upperBits(upperBits), length(length) {

        // uint64_t numWords = (length + 63) / 64;
        uint64_t d = 0;


        uint64_t totalBytes = (length + 7) / 8;  // +7 用于向上取整

        for (uint64_t i = 0; i < totalBytes; ++i) {
            // 对于每个字节，使用__builtin_popcount计算其中1的数量
            d += __popc(upperBits[i]);
        }
        numOnes = d;
        onesPerInventory = 1 << (log2OnesPerInventory = mostSignificantBit((d * MAX_ONES_PER_INVENTORY + length - 1) / length));
        onesPerInventoryMask = onesPerInventory - 1;
        InventorySize = (d + onesPerInventory - 1) / onesPerInventory;
        inventory = new  uint64_t[InventorySize + 1]();


        d = 0;

        for (uint64_t i = 0, globalBitIndex = 0; i < totalBytes; i++) {
            for (uint64_t j = 0; j < 8 && globalBitIndex < length; j++, globalBitIndex++) {
                if (upperBits[i] & (1 << j)) {
                    if ((d & onesPerInventoryMask) == 0) {
                        inventory[d >> log2OnesPerInventory] = globalBitIndex;
                    }
                    d++;
                }
            }
        }


        inventory[InventorySize] = length;

        log2LongwordsPerSubinventory = min(MAX_LOG2_LONGWORDS_PER_SUBINVENTORY, max(0, log2OnesPerInventory - 2));//3
        log2OnesPerSub64 = max(0, log2OnesPerInventory - log2LongwordsPerSubinventory);//8
        log2OnesPerSub16 = max(0, log2OnesPerSub64 - 2);//6
        onesPerSub64 = 1 << log2OnesPerSub64;//256
        onesPerSub16 = 1 << log2OnesPerSub16;//64
        onesPerSub16Mask = onesPerSub16 - 1;
        if (onesPerInventory > 1) {
            d = 0;
            int ones;
            uint64_t diff16 = 0, start = 0, span = 0;
            int spilled = 0, inventoryIndex = 0;

            for (int i = 0; i < totalBytes; i++) {
                for (int j = 0; j < 8; j++) {
                    if (i * 8 + j >= length) break;
                    if (upperBits[i] & (1 << j)) {
                        if ((d & onesPerInventoryMask) == 0) {
                            inventoryIndex = d >> (log2OnesPerInventory);
                            start = inventory[inventoryIndex];
                            span = inventory[inventoryIndex + 1] - start;
                            ones = (int)min((int)(numOnes - d), onesPerInventory);

                            // Always count diff16's, minimum 4
                            diff16 += max(4, (ones + onesPerSub16 - 1) >> log2OnesPerSub16);
                            if (span >= MAX_SPAN && onesPerSub64 > 1) {
                                spilled += ones;
                            }
                        }
                        d++;
                    }
                }
            }
            SubinventorySize = (int)((diff16 + 3) / 4);
            ExactSpillSize = spilled;
            subinventory = new uint64_t[SubinventorySize];
            exactSpill = new uint64_t[ExactSpillSize];

            uint64_t offset = 0;
            spilled = 0;
            d = 0;
            //----------------------------
            for (uint64_t i = 0, globalBitIndex = 0; i < totalBytes; i++) {
                for (uint64_t j = 0; j < 8 && globalBitIndex < length; j++, globalBitIndex++) {
                    if (upperBits[i] & (1 << j)) {
                        uint64_t inventoryIndex = d >> log2OnesPerInventory;
                        uint64_t start = inventory[inventoryIndex];
                        uint64_t span = inventory[inventoryIndex + 1] - start;
                        if ((d & onesPerInventoryMask) == 0) {
                            offset = 0; // 重置偏移量
                        }

                        if (span < MAX_SPAN) {


                            if ((d & onesPerSub16Mask) == 0) {

                                // 这里简单地存储每个段的开始位置
                                uint64_t pos = (inventoryIndex << (log2LongwordsPerSubinventory + 2)) + offset++;

                                setSubinventory16(pos, globalBitIndex - start);
                                //subinventory[pos] = globalBitIndex - start;
                            }
                        }
                        else {
                            if (onesPerSub64 == 1) {
                                setSubinventory16((inventoryIndex << log2LongwordsPerSubinventory) + offset++, globalBitIndex);
                                //subinventory[(inventoryIndex << log2LongwordsPerSubinventory) + offset++] = globalBitIndex;
                            }
                            else {
                                if ((d & onesPerInventoryMask) == 0) {
                                    inventory[inventoryIndex] |= 1ULL << 63; // 标记为使用exactSpill

                                    subinventory[inventoryIndex << log2LongwordsPerSubinventory] = spilled;
                                }
                                exactSpill[spilled++] = globalBitIndex;

                            }
                        }
                        d++;
                    }
                }
            }
        }
        else {
            subinventory = nullptr;
            exactSpill = nullptr;
        }

    }
    //获取第rank个数值=============
    __device__     uint64_t select(uint64_t rank) {
        if (rank > numOnes) return static_cast<uint64_t>(-1);

        uint64_t inventoryIndex = rank >> log2OnesPerInventory;

        uint64_t inventoryRank = inventory[inventoryIndex];
        uint64_t subrank = rank & onesPerInventoryMask;

        if (subrank == 0) return inventoryRank & ~(1ULL << 63);

        uint64_t start;
        int residual;

        if (!(inventoryRank & (1ULL << 63))) {
            //  uint64_t tmp = inventoryIndex * (1 << log2LongwordsPerSubinventory) + (subrank >> log2OnesPerSub16);

            start = inventoryRank + getSubinventory16((inventoryIndex << (log2LongwordsPerSubinventory + 2)) + (subrank >> log2OnesPerSub16));
            residual = subrank & onesPerSub16Mask;
        }
        else {
            if (onesPerSub64 == 1) {
                return subinventory[inventoryIndex * (1 << log2LongwordsPerSubinventory) + subrank];
            }
            else {
                return exactSpill[subinventory[inventoryIndex * (1 << log2LongwordsPerSubinventory)] + subrank];
            }
        }

        if (residual == 0) return start;


        uint64_t wordIndex = start / 64;
        uint64_t byteIndex = wordIndex * 8;
        uint64_t bitIndex = start % 64;


        uint64_t word = 0;
        uint64_t* x = reinterpret_cast<uint64_t*>(upperBits + byteIndex);

        word = *x;

        // 应用start位的偏移量，如果有必要
        if (bitIndex != 0) {

            word = word & (~0ULL << bitIndex);
        }

        // 使用word进行位操作
        while (true) {
            int bitCount = __popcll(word);

            if (residual < bitCount) break;
            // 更新wordIndex并读取下一个word

            x++;
            wordIndex++;
            word = *x;

            residual -= bitCount;
        }


        // This would need a custom method to calculate the position of the nth 1 bit in the word.
        return wordIndex * 64 + findNthOnePosition(word, residual);
    }
    __device__   int findNthOnePosition(uint64_t number, int n) {
        int position = 0; // 位置从0开始计数
        int count = 0;
        while (number != 0) {
            if (number & 1) { // 检查当前最低位是否为1
                count++;
                if (count == n + 1) {
                    //if(position!=0) position++;
                    return position; // 返回当前1的位置
                }
                // n--; // 找到一个1，减少计数
            }
            position++; // 移动到下一个位
            number >>= 1; // 右移，检查下一个位
        }
        return -1; // 如果没有找到第N个1，则返回-1
    }


    // 获取subinventory中index位置的16位值
    __device__   uint16_t getSubinventory16(int index) {

        uint64_t value = subinventory[index / 4]; // 每个uint64_t可以存储4个16位的数值
      //   cout<<"get sub 16里的value："<<value<<endl;
        int shift = (3 - (index % 4)) * 16; // 根据位置计算偏移量
        return (value >> shift) & 0xFFFF; // 右移并应用掩码以获取16位的值
    }

    // 设置subinventory中index位置的16位值为val
    __device__  void setSubinventory16(int index, uint16_t val) {
        int shift = (3 - (index % 4)) * 16; // 计算偏移量
        uint64_t mask = 0xFFFFULL << shift; // 创建掩码
        subinventory[index / 4] = (subinventory[index / 4] & ~mask) | (static_cast<uint64_t>(val) << shift); // 清除当前位置的值并设置新值
    }

    ~SimpleSelect() {
        delete[] inventory; // 释放动态分配的内存
        delete[] exactSpill;
        delete[] subinventory;
    }


    static const  uint64_t serialVersionUID = 1L;
    static  const  int MAX_ONES_PER_INVENTORY = 8192;
    static const   int MAX_LOG2_LONGWORDS_PER_SUBINVENTORY = 3;
    static  const  int MAX_SPAN = 1 << 16;
    uint8_t* upperBits; // 用于替代BitVector的原始指针
    uint64_t length; // upperBits数组的大小，以字节为单位
    uint64_t numOnes;
    int numWords;
    // uint64_t length;
    int log2OnesPerInventory;
    int onesPerInventory;
    int onesPerInventoryMask;
    int InventorySize;
    int SubinventorySize;
    int ExactSpillSize;
    uint64_t* inventory;
    uint64_t* subinventory;
    uint64_t* exactSpill;
    //  uint16_t* subinventory16;
    int log2LongwordsPerSubinventory;
    int log2OnesPerSub64;
    int onesPerSub64;
    int log2OnesPerSub16;
    int onesPerSub16;
    int onesPerSub16Mask;
};

__device__  uint64_t getOffset(uint64_t i, int l, uint8_t* lowerBitsVector, SimpleSelect* d_selectUpper) {

    uint64_t low = 0;
    uint64_t startPos = i * l; // 从这个位置开始读取l位
    for (uint64_t bitPos = startPos; bitPos < startPos + l; ++bitPos) {
        uint64_t byteIndex = bitPos / 8;
        uint64_t bitIndex = 7 - (bitPos % 8); // 注意字节中位的顺序
        uint8_t bit = (lowerBitsVector[byteIndex] >> bitIndex) & 1;
        low = (low << 1) | bit; // 将读取的位加到low的最低位
    }

    //  bool found = false; // 新增标志变量
    //  uint64_t onesCount = 0;
     // uint64_t position = 0;
    uint64_t high = 0;

    // 获取高位值：使用选择操作获得第i个1的位置


    high = d_selectUpper->select(i) - i;

    return (high << l) | low;
}


const int SimpleSelect::MAX_LOG2_LONGWORDS_PER_SUBINVENTORY;

//仅获取下一个gamma编码的bit位数并跳过，不解码出真值
void  __device__ skipGamma(uint8_t* arr, uint64_t* s) {
    uint64_t cpt = *s, l;

    l = readUnary(arr, &cpt);
    //cout<< "l:"<<l<<endl;
    if (l == 0) {
        *s = cpt;
        return;
    }


    *s = cpt + l;

    return;



}

uint64_t __device__ readZeta_k(int* ZETA_3, uint8_t* arr, uint64_t* s, ...)
{
    //提取从s开始的16位bit
    // 计算起始字节的索引和位偏移
    uint64_t byteIndex = *s / 8;
    uint8_t bitOffset = *s % 8;
    uint64_t mask = u64pow(2, 8 - bitOffset) - 1;
    // 从 arr[byteIndex]、arr[byteIndex + 1] 和 arr[byteIndex + 2] 中提取 16 位数据
    uint16_t result = (static_cast<uint16_t>(arr[byteIndex] & mask) << 8) |
        (static_cast<uint16_t>(arr[byteIndex + 1]) & 0xFF);

    // 如果位偏移不为 0，则从下一个字节中提取一些位
    if (bitOffset != 0) {
        uint8_t bitsFromNextByte = static_cast<uint8_t>(arr[byteIndex + 2] >> (8 - bitOffset));
        result = result << bitOffset;
        result |= (static_cast<uint16_t>(bitsFromNextByte));

    }

    int preComp;

    if ((preComp = ZETA_3[result & 0xffff]) != 0) {
        *s += preComp >> 16;

        return preComp & 0xFFFF;
    }
    uint64_t val = 0, pos, p = 1, i, left, res,
        h = readUnary(arr, s);
    //cout<<"h:"<<h<<endl;


    uint8_t k = 3;
    pos = *s;
    left = 1 << h * k;

    for (i = h * k + k - 1; i > 0; i--) {
        if (CHECK_BIT(*(uint8_t*)&arr[(pos + i - 1) / 8], 7 - (pos + i - 1) % 8))
            val += p;
        p *= 2;
    }
    if (val < left) {
        *s = pos + h * k + k - 1;
        res = val + left;
    }
    else {
        *s = pos + h * k + k;
        res = (val * 2) + CHECK_BIT(*(uint8_t*)&arr[(pos + h * k + k - 1) / 8], 7 - (pos + h * k + k - 1) % 8);
    }
    return res - 1;
}
uint64_t  __device__ readZeta_k2(uint8_t* arr, uint64_t* s, ...)
{
    uint64_t val = 0, pos, p = 1, i, left, res,
        h = readUnary(arr, s);
    //cout<<"h:"<<h<<endl;


    uint8_t k = 2;
    pos = *s;
    left = 1 << h * k;

    for (i = h * k + k - 1; i > 0; i--) {
        if (CHECK_BIT(*(uint8_t*)&arr[(pos + i - 1) / 8], 7 - (pos + i - 1) % 8))
            val += p;
        p *= 2;
    }
    if (val < left) {
        *s = pos + h * k + k - 1;
        res = val + left;
    }
    else {
        *s = pos + h * k + k;
        res = (val * 2) + CHECK_BIT(*(uint8_t*)&arr[(pos + h * k + k - 1) / 8], 7 - (pos + h * k + k - 1) % 8);
    }
    return res - 1;
}
//获取重边列表中下一个值    currMult 当前重边序号  len 当前重边剩余数量 prev 前一个重边序号
int __device__ nextMult(int* GAMMA, int& x, uint8_t* arr, int& currMult, int& len, int& prev, uint64_t* multOffset) {
    if (*multOffset == 0) {

        currMult = MAX_LABEL;
        return currMult;
    }
    if (currMult == 0) {
        currMult = prev = (int)(inv_v_func(readGamma(GAMMA, arr, multOffset)) + x);
        //  printf("prev:%d\n", prev);
        len = readGamma(GAMMA, arr, multOffset) + DEFAULT_MIN_MULTIPLE_LENGTH;
        len--;
        return currMult;
    }
    if (len == 0 && currMult != 0) {
        currMult = prev = readGamma(GAMMA, arr, multOffset) + prev + 1;
        //printf("currMult:%d\n", currMult);
        len = readGamma(GAMMA, arr, multOffset) + DEFAULT_MIN_MULTIPLE_LENGTH;
        len--;
        return currMult;
    }
    else {
        len--;
        return currMult;
    }

}
//获取连续区间中下一个值    currInterval 当前连续边序号  len 当前连续区间剩余长度  prev 前一个连续边序号
int __device__ nextInterval(int* GAMMA, int& x, uint8_t* arr, int& currInterval, int& len, int& prev, uint64_t* IOffset) {
    if (*IOffset == 0) {
        currInterval = MAX_LABEL;
        return  currInterval;
    }

    if (currInterval == 0) {
        currInterval = prev = (int)(inv_v_func(readGamma(GAMMA, arr, IOffset)) + x);
        // printf("iprev:%d\n", prev);
        len = readGamma(GAMMA, arr, IOffset) + DEFAULT_MIN_INTERVAL_LENGTH;
        len--;
        return currInterval;
    }
    if (len == 0 && currInterval != 0) {
        currInterval = prev = readGamma(GAMMA, arr, IOffset) + prev + 2;
        len = readGamma(GAMMA, arr, IOffset) + DEFAULT_MIN_INTERVAL_LENGTH;
        len--;
        return currInterval;
    }
    else {
        currInterval++;
        prev++;
        len--;
        return currInterval;
    }

}
//获取连续区间中下一个值    currInterval 当前连续边序号  len 当前连续区间剩余长度  prev 前一个连续边序号
int __device__ nextResidual(int* ZETA_3, int& x, uint8_t* arr, int& currResidual, int& prev, uint64_t* residualOffset) {
    if (*residualOffset == 0) {
        currResidual = MAX_LABEL;
        return  currResidual;
    }
    if (currResidual == 0) {
        currResidual = prev = (int)(inv_v_func(readZeta_k(ZETA_3, arr, residualOffset)) + x);
        // printf("rprev:%d\n", *residualOffset);
        return currResidual;
    }

    else {
        currResidual = prev = readZeta_k(ZETA_3, arr, residualOffset) + prev + 1;
        return currResidual;
    }


}
//获取节点x的下一个邻居节点的序号  flag 记录上次取nextValue取的是重边1、连续2、剩余3中的哪个  0代表未取值
int __device__ nextValue(int* GAMMA, int* ZETA_3, int& x, uint8_t* arr,
    int& currMult, int& mlen, int& m_prev, uint64_t* multOffset, int& multiplesCount, int& multiplesNum,
    int& currInterval, int& Ilen, int& I_prev, uint64_t* IOffset, int& intervalCount, int& intervalNum,
    int& currResidual, int& r_prev, uint64_t* residualOffset, int& residualCount, int& residualNum, int& flag) {

    if (multiplesNum >= multiplesCount && intervalNum >= intervalCount && residualNum >= residualCount) {
        currResidual = currInterval = currMult = MAX_LABEL; flag = 3;
        return MAX_LABEL;
    }
    int multValue, intervalValue, residualValue;
    //第一次调用nextValue：
    if (flag == 0) {
        multValue = nextMult(GAMMA, x, arr, currMult, mlen, m_prev, multOffset);
        //   if(x==112202)
       //    printf("multValue:%d\n", multValue);

        intervalValue = nextInterval(GAMMA, x, arr, currInterval, Ilen, I_prev, IOffset);

        residualValue = nextResidual(ZETA_3, x, arr, currResidual, r_prev, residualOffset);

        // printf("intervalValue:%d\n", intervalValue);
        // printf("residualValue:%d\n", residualValue);
        if (multValue < intervalValue && multValue < residualValue) {
            flag = 1;
            return multValue;
        }
        if (multValue > intervalValue && intervalValue < residualValue) {
            flag = 2;
            return intervalValue;
        }
        if (residualValue < intervalValue && multValue > residualValue) {
            flag = 3;
            residualNum++;
            return residualValue;
        }

    }

    if (flag == 1) {//上一个是重边中的值 
        if (mlen != 0) {
            mlen--;
            return currMult;
        }
        else {//重边取新值
            multiplesNum++;
            multValue = multiplesNum < multiplesCount ? nextMult(GAMMA, x, arr, currMult, mlen, m_prev, multOffset) : MAX_LABEL;
            if (multValue == MAX_LABEL) currMult = MAX_LABEL;
            intervalValue = currInterval;
            residualValue = currResidual;
            if (multValue < intervalValue && multValue < residualValue) {
                flag = 1;
                return multValue;
            }
            if (multValue > intervalValue && intervalValue < residualValue) {
                flag = 2;
                return intervalValue;
            }
            if (residualValue < intervalValue && multValue > residualValue) {
                flag = 3;
                residualNum++;
                return residualValue;
            }
        }

    }
    if (flag == 2) {//上一个是连续区间中的值 
        if (Ilen != 0) {
            Ilen--;
            currInterval++;
            I_prev++;
            return  currInterval;
        }
        else {//连续区间取新值
            intervalNum++;
            multValue = currMult;
            intervalValue = intervalNum < intervalCount ? nextInterval(GAMMA, x, arr, currInterval, Ilen, I_prev, IOffset) : MAX_LABEL;
            if (intervalValue == MAX_LABEL) currInterval = MAX_LABEL;
            residualValue = currResidual;
            if (multValue < intervalValue && multValue < residualValue) {
                flag = 1;
                return multValue;
            }
            if (multValue > intervalValue && intervalValue < residualValue) {
                flag = 2;
                return intervalValue;
            }
            if (residualValue < intervalValue && multValue > residualValue) {
                flag = 3;
                residualNum++;
                return residualValue;
            }
        }

    }
    if (flag == 3) {//上一个是剩余区间中的值 
         //剩余区间取新值
        multValue = currMult;
        intervalValue = currInterval;
        residualValue = residualNum < residualCount ? nextResidual(ZETA_3, x, arr, currResidual, r_prev, residualOffset) : MAX_LABEL;

        if (residualValue == MAX_LABEL)    currResidual = MAX_LABEL;

        if (multValue < intervalValue && multValue < residualValue) {
            flag = 1;
            return multValue;
        }
        if (multValue > intervalValue && intervalValue < residualValue) {
            flag = 2;
            return intervalValue;
        }
        if (residualValue < intervalValue && multValue > residualValue) {
            flag = 3;
            residualNum++;
            return residualValue;
        }
    }




}
//nextValue重载 ,用在参考链解码中
int __device__ nextValue(int* GAMMA, int* ZETA_3, int& x, uint8_t* arr,
    NodeState* current   ) {

    if (current->multiplesNum >= current->multiplesCount && current->intervalNum >= current->intervalCount && current->residualNum >= current->residualCount) {
        current->currResidual = current->currInterval = current->currMult = MAX_LABEL; current->flag = 3;
        return MAX_LABEL;
    }
    int multValue, intervalValue, residualValue;
    //第一次调用nextValue：
    if (current->flag == 0) {
        multValue = nextMult(GAMMA, x, arr, current->currMult, current->mlen, current->m_prev,& current->multOffset);
        //   if(x==112202)
       //    printf("multValue:%d\n", multValue);

        intervalValue = nextInterval(GAMMA, x, arr, current->currInterval, current->Ilen, current->I_prev,& current->IOffset);

        residualValue = nextResidual(ZETA_3, x, arr, current->currResidual, current->r_prev,& current->residualOffset);

        // printf("intervalValue:%d\n", intervalValue);
        // printf("residualValue:%d\n", residualValue);
        if (multValue < intervalValue && multValue < residualValue) {
            current->flag = 1;
            return multValue;
        }
        if (multValue > intervalValue && intervalValue < residualValue) {
            current->flag = 2;
            return intervalValue;
        }
        if (residualValue < intervalValue && multValue > residualValue) {
            current->flag = 3;
            current->residualNum++;
            return residualValue;
        }

    }

    if (current->flag == 1) {//上一个是重边中的值 
        if (current->mlen != 0) {
            current->mlen--;
            return current->currMult;
        }
        else {//重边取新值
            current->multiplesNum++;
            multValue = current->multiplesNum < current->multiplesCount ? nextMult(GAMMA, x, arr, current->currMult, current->mlen, current->m_prev, &current->multOffset) : MAX_LABEL;
            if (multValue == MAX_LABEL) current->currMult = MAX_LABEL;
            intervalValue = current->currInterval;
            residualValue = current->currResidual;
            if (multValue < intervalValue && multValue < residualValue) {
                current->flag = 1;
                return multValue;
            }
            if (multValue > intervalValue && intervalValue < residualValue) {
                current->flag = 2;
                return intervalValue;
            }
            if (residualValue < intervalValue && multValue > residualValue) {
                current->flag = 3;
                current->residualNum++;
                return residualValue;
            }
        }

    }
    if (current->flag == 2) {//上一个是连续区间中的值 
        if (current->Ilen != 0) {
            current->Ilen--;
            current->currInterval++;
            current->I_prev++;
            return  current->currInterval;
        }
        else {//连续区间取新值
            current->intervalNum++;
            multValue = current->currMult;
            intervalValue = current->intervalNum < current->intervalCount ? nextInterval(GAMMA, x, arr, current->currInterval, current->Ilen, current->I_prev,& current->IOffset) : MAX_LABEL;
            if (intervalValue == MAX_LABEL) current->currInterval = MAX_LABEL;
            residualValue = current->currResidual;
            if (multValue < intervalValue && multValue < residualValue) {
                current->flag = 1;
                return multValue;
            }
            if (multValue > intervalValue && intervalValue < residualValue) {
                current->flag = 2;
                return intervalValue;
            }
            if (residualValue < intervalValue && multValue > residualValue) {
                current->flag = 3;
                current->residualNum++;
                return residualValue;
            }
        }

    }
    if (current->flag == 3) {//上一个是剩余区间中的值 
         //剩余区间取新值
        multValue = current->currMult;
        intervalValue = current->currInterval;
        residualValue = current->residualNum < current->residualCount ? nextResidual(ZETA_3, x, arr, current->currResidual, current->r_prev,& current->residualOffset) : MAX_LABEL;

        if (residualValue == MAX_LABEL)    current->currResidual = MAX_LABEL;

        if (multValue < intervalValue && multValue < residualValue) {
            current->flag = 1;
            return multValue;
        }
        if (multValue > intervalValue && intervalValue < residualValue) {
            current->flag = 2;
            return intervalValue;
        }
        if (residualValue < intervalValue && multValue > residualValue) {
            current->flag = 3;
            current->residualNum++;
            return residualValue;
        }
    }




}
//获取节点x在graph中的开始位置
uint64_t __device__  get_position(int* GAMMA, uint8_t* arr, uint8_t* d_Offsets, int x) {
    uint64_t position = 0;
    uint64_t s = 0;
    for (int i = 0; i < x + 1; i++) {
        position += readGamma(GAMMA, d_Offsets, &s);
    }
    return position;
}


//获取节点x的出度
uint64_t __device__  outdegreeInternal(int* GAMMA, uint8_t* arr, uint8_t* d_Offsets, int x) {
    uint64_t position = 0;
    uint64_t s = 0;
    for (int i = 0; i < x + 1; i++) {
        position += readGamma(GAMMA, d_Offsets, &s);
    }
    return readGamma(GAMMA, arr, &position);
}


int __device__ ref_nextValue(int* GAMMA, int* ZETA_3, int& x, uint8_t* arr, NodeState* refChain, int ind);

// 模拟递归消除的 nextRefValue 函数
int __device__ nextRefValue(int* GAMMA, int* ZETA_3, int& x, uint8_t* arr, NodeState* refChain, int ind) {
    // 模拟栈
    int stack[4];  // 栈大小为4
    int top = -1;          // 栈顶指针
    int result = 0;

    // 入栈
    stack[++top] = ind;

    while (top >= 0) {
        ind = stack[top--]; // 出栈

        if (refChain[ind].blockNum > refChain[ind].blockCount) {
            return MAX_LABEL; // 参考块已输出完
        }

        if (refChain[ind + 1].ref == 0) {
            if (refChain[ind].currblock == 0 && refChain[ind].ref_flag == 0) {
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum == refChain[ind].blockCount - 1) {
                    refChain[ind].blockNum++;
                }
                refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;

                for (int i = 0; i < refChain[ind].currblock; i++) {
                    nextValue(GAMMA, ZETA_3, refChain[ind + 1].x, arr, &refChain[ind + 1]);
                }
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum == refChain[ind].blockCount - 1) {
                    refChain[ind].currblock = refChain[ind + 1].d - refChain[ind].cpt;
                }
                refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;
            }

            result = nextValue(GAMMA, ZETA_3, refChain[ind + 1].x, arr, &refChain[ind + 1]);
            refChain[ind].currblock--;

            if (refChain[ind].currblock == 0) {
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum == refChain[ind].blockCount) {
                    refChain[ind].blockNum++;
                }
                refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;
                for (int i = 0; i < refChain[ind].currblock; i++) {
                    nextValue(GAMMA, ZETA_3, refChain[ind + 1].x, arr, &refChain[ind + 1]);
                }
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum == refChain[ind].blockCount) {
                    refChain[ind].currblock = refChain[ind + 1].d - refChain[ind].cpt;
                }
                else {
                    refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;
                }
            }
            continue; // 继续处理下一个栈中的值
        }
        else {
            if (refChain[ind].currblock == 0) {
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum == refChain[ind].blockCount - 1) {
                    refChain[ind].blockNum++;
                }
                refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;

                for (int i = 0; i < refChain[ind].currblock; i++) {
                    ref_nextValue(GAMMA, ZETA_3, refChain[ind + 1].x, arr, refChain, ind + 1);
                }
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum == refChain[ind].blockCount - 1) {
                    refChain[ind].currblock = refChain[ind + 1].d - refChain[ind].cpt;
                }
                refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;
            }
            result = ref_nextValue(GAMMA, ZETA_3, refChain[ind + 1].x, arr, refChain, ind + 1);
            refChain[ind].currblock--;
            //  printf("refChain[ind + 1].x:%d\n", refChain[ind + 1].x);
             //  printf("result in  nextrefValue:%d\n", result);
            if (refChain[ind].currblock == 0) {
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum > refChain[ind].blockCount) return result;//只有一个块的情况  blockCount==0  
                if (refChain[ind].blockNum == refChain[ind].blockCount) {
                    refChain[ind].blockNum++; //此时blockNum == blockCount

                }
                //下一个块是无效块，直接跳过
                refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;
                // printf("else, x:%d currblock:%d\n", x, refChain[ind].currblock);
                 // cpt += currblock;
                for (int i = 0; i < refChain[ind].currblock; i++)
                    ref_nextValue(GAMMA, ZETA_3, refChain[ind + 1].x, arr, refChain, ind + 1);

                //更新出下个有效块的信息
                refChain[ind].blockNum++;
                if (refChain[ind].blockNum == refChain[ind].blockCount) {
                    refChain[ind].currblock = refChain[ind + 1].d - refChain[ind].cpt;
                }
                else {
                    refChain[ind].currblock = readGamma(GAMMA, arr, &refChain[ind].refOffset) + 1;  //printf("else,  currblock:%d\n", refChain[ind].currblock);
                }
                //  cpt += currblock;

            }
            stack[++top] = ind + 1; // 推入下一步状态
        }
    }

    return result;
}

// 消除递归的 ref_nextValue 函数
int __device__ ref_nextValue(int* GAMMA, int* ZETA_3, int& x, uint8_t* arr, NodeState* refChain, int ind) {
    // 模拟栈
    int stack[4];  // 栈大小为4
    int top = -1;
    int value_ref, value_interval;

    stack[++top] = ind; // 入栈

    while (top >= 0) {
        ind = stack[top--]; // 出栈

        if (refChain[ind].ref_flag == 0) {
            value_ref = nextRefValue(GAMMA, ZETA_3, x, arr, refChain, ind);
            refChain[ind].pre_refvalue = value_ref;
            value_interval = nextValue(GAMMA, ZETA_3, x, arr, &refChain[ind]);

            if (value_ref < value_interval) {
                refChain[ind].ref_flag = 1;
                return value_ref;
            }
            else {
                refChain[ind].ref_flag = 2;
                return value_interval;
            }
        }

        if (refChain[ind].ref_flag == 1) {
            if (refChain[ind].flag == 1) value_interval = refChain[ind].currMult;
            else if (refChain[ind].flag == 2) value_interval = refChain[ind].currInterval;
            else if (refChain[ind].flag == 3)    value_interval = refChain[ind].currResidual;
            

            refChain[ind].pre_refvalue = value_ref = nextRefValue(GAMMA, ZETA_3, x, arr, refChain, ind);

            if (value_ref < value_interval) {
                refChain[ind].ref_flag = 1;
                return value_ref;
            }
            else {
                refChain[ind].ref_flag = 2;
                return value_interval;
            }
        }

        if (refChain[ind].ref_flag == 2) {
            value_interval = nextValue(GAMMA, ZETA_3, x, arr, &refChain[ind]);
            value_ref = refChain[ind].pre_refvalue;

            if (value_ref < value_interval) {
                refChain[ind].ref_flag = 1;
                return value_ref;
            }
            else {
                refChain[ind].ref_flag = 2;
                return value_interval;
            }
        }
    }

    return MAX_LABEL;
}


__device__ void BFS_get_dest(int dest_size, uint64_t offsetGraph, bool* dv2, int* GAMMA, int* ZETA_3, int x, uint8_t* arr, uint64_t* s, uint8_t* d_Timestamps, uint64_t* t_pos,
    uint8_t* d_lowerBitsVector, SimpleSelect* d_selectUpper, bool isfc, bool* d_Fa, int* d_Xa, int* d_time, int* p, int* d_Ca, int* changed,
    int* d_currentQueue, int* d_nextQueue, int* d_queueSize, int* d_nextQueueSize, uint64_t* d_timeAndP)
{

    //if (x !=33) return;
    // printf("here travel just in\n");
   //if(n==41*32 + 1)
  // printf("here n: %d\n",n);
  // printf("get dest_size: %d\n", dest_size);



    int result;
    int timestamp;
    ///  printf("s:%lu\n", *s);
    if (!isfc) {

        //   printf("here isfc \n" );
        *s = getOffset(x, gef_low_len, d_lowerBitsVector, d_selectUpper) - offsetGraph * 8;
        //    printf("s:%lu\n", *s);
    }
    // printf("here travel\n");
    int d,//出度
        ref, //refIndex,//reference
        extraCount;//剩余邻居数
   // printf("here before read d \n");
    int blockCount = 0, cptblocks = 0, block_size,
        //  i,
        cpt = 0, cptdecoded = 0;
    d = readGamma(GAMMA, arr, s);
    extraCount = d;

    // printf("d:%d\n", d);
     //    cout << "d" << d << endl;
    ref = readUnary(arr, s);
    int tmp_ref = ref;
    //   printf("tmp_ref：%d\n", tmp_ref);
   // NodeState* refChain = (NodeState*)malloc(sizeof(NodeState) * 4);
    NodeState  refChain[4];
    memset(refChain, 0, sizeof(NodeState) * 4);

    // printf("here travel\n");
   // int RefChain[3]; //记录参考链中的ref值，3为最大参考链长度
    int ChainLen = 0;//记录参考链长度
    int originV = x;//当前参考链的最初始节点，或者 当前需要获取邻居序列的节点
    uint64_t tmp_rpos = *s;

    // uint64_t refVpos[3];
    while (tmp_ref > 0) {//参考链未到底

        refChain[ChainLen].x = originV;
        refChain[ChainLen].d = extraCount;
        //  RefChain[ChainLen ] = tmp_ref;
        refChain[ChainLen].ref = tmp_ref;
        uint8_t cv = 1;
        refChain[ChainLen].blockCount = blockCount = readGamma(GAMMA, arr, &tmp_rpos);
        //   printf("参考块数量：%d", blockCount);
        refChain[ChainLen].refOffset = tmp_rpos;
        //  refOffset =  tmp_rpos;
        cptblocks = 0; cpt = 0; cptdecoded = 0;
        if (blockCount != 0) {
            int  currblock = readGamma(GAMMA, arr, &refChain[ChainLen].refOffset);
            refChain[ChainLen].currblock = currblock;
            //   printf("blockCount:%d\n", currblock);

            while (cptblocks < blockCount) {
                block_size = readGamma(GAMMA, arr, &tmp_rpos) + (cptblocks == 0 ? 0 : 1);
                //   printf("偏移量：%lu", tmp_rpos);
                //      printf("参考块大小：%d", block_size);
                if (cv) {
                    cpt += block_size;
                    cptdecoded += block_size;
                    /*for (i = 0; i < block_size; i++) {
                        dest[cptdecoded++] = dest[cpt++];
                    }*/
                }
                else {

                    cpt += block_size;
                }
                cv = !cv;
                cptblocks++;
            }
        }
        //int currblock = readGamma(GAMMA, arr, &refChain[ChainLen].refOffset);
        //refChain[ChainLen].currblock = currblock;
        //// printf("blockCount:%d\n", blockCount);
        //cptblocks = 0; cpt = 0; cptdecoded = 0;
        //while (cptblocks < blockCount) {
        //    block_size = readGamma(GAMMA, arr, &tmp_rpos) + (cptblocks == 0 ? 0 : 1);
        //    //   printf("偏移量：%lu", tmp_rpos);
        //    //      printf("参考块大小：%d", block_size);
        //    if (cv) {
        //        cpt += block_size;
        //        cptdecoded += block_size;
        //        /*for (i = 0; i < block_size; i++) {
        //            dest[cptdecoded++] = dest[cpt++];
        //        }*/
        //    }
        //    else {

        //        cpt += block_size;
        //    }
        //    cv = !cv;
        //    cptblocks++;
        //}
        refChain[ChainLen].cpt = cpt;
        //最后一个不做记录的block
        if (cv) {
            //     printf("x-ref:%d\n", x - ref);
            uint64_t tmp_offsets = getOffset(originV - tmp_ref, gef_low_len, d_lowerBitsVector, d_selectUpper) - offsetGraph * 8;
            // tmp_offset = tmp_offsets;
            cptdecoded += readGamma(GAMMA, arr, &tmp_offsets) - cpt; //dlen: 这里表示被参考节点的出度 可以直接读取x-ref的开始位置，然后readgamma
        }
        if (blockCount == 0) {
            //  printf("当前节点：%d\n",x ); 
            //   printf("当前cptdecoded：%d\n",cptdecoded ); 
            refChain[ChainLen].currblock = cptdecoded;
        }

        originV -= tmp_ref;

        extraCount -= cptdecoded;

        int multiplesCount = 0; // 重边数量

        if (extraCount > 0) {
            // printf("读multiple前s:%lu\n", *s);
            refChain[ChainLen].multiplesCount = multiplesCount = readGamma(GAMMA, arr, &tmp_rpos);
            //   cout << "multiplesCount" << multiplesCount << endl;

            if (multiplesCount != 0) {
                refChain[ChainLen].multOffset = tmp_rpos;
                //跳过重边列表    (值可不读，len可能得读
                for (int i = 0; i < multiplesCount; i++) {
                    skipGamma(arr, &tmp_rpos);
                    extraCount -= readGamma(GAMMA, arr, &tmp_rpos) + DEFAULT_MIN_MULTIPLE_LENGTH;

                }
            }
        }


        int intervalCount = 0;
        if (extraCount > 0) {
            refChain[ChainLen].intervalCount = intervalCount = readGamma(GAMMA, arr, &tmp_rpos);

            if (intervalCount != 0) {
                refChain[ChainLen].IOffset = tmp_rpos;//记录连续区间开始位置
                //跳过连续区间   (值可不读，len可能得读
                for (int i = 0; i < intervalCount; i++) {
                    skipGamma(arr, &tmp_rpos);
                    extraCount -= readGamma(GAMMA, arr, &tmp_rpos) + DEFAULT_MIN_INTERVAL_LENGTH;
                }
            }
        }
        if (extraCount > 0)
            refChain[ChainLen].residualOffset = tmp_rpos; //记录剩余区间开始位置
        int residualCount = extraCount;
        refChain[ChainLen].residualCount = residualCount;

        //更新ref
        // if (tmp_offset != 0) tmp_rpos = tmp_offset;
        tmp_rpos = getOffset(originV, gef_low_len, d_lowerBitsVector, d_selectUpper) - offsetGraph * 8;
        // printf("originV:%d\n", originV);
         //refVpos[ChainLen] = tmp_rpos;
        extraCount = readGamma(GAMMA, arr, &tmp_rpos);
        tmp_ref = readUnary(arr, &tmp_rpos);
        ChainLen++;
    }
    //printf("参考链长度：%d", ChainLen);
    refChain[ChainLen].x = originV;
    refChain[ChainLen].d = extraCount;

    refChain[ChainLen].ref = tmp_ref;
    // printf("here berfore get refferred info\n");
     //参考链最末段节点
    int multiplesCount = 0; // 重边数量

    if (extraCount > 0) {
        // printf("读multiple前s:%lu\n", *s);
        refChain[ChainLen].multiplesCount = multiplesCount = readGamma(GAMMA, arr, &tmp_rpos);
        //   cout << "multiplesCount" << multiplesCount << endl;

        if (multiplesCount != 0) {
            refChain[ChainLen].multOffset = tmp_rpos;
            //跳过重边列表    (值可不读，len可能得读
            for (int i = 0; i < multiplesCount; i++) {
                skipGamma(arr, &tmp_rpos);
                extraCount -= readGamma(GAMMA, arr, &tmp_rpos) + DEFAULT_MIN_MULTIPLE_LENGTH;

            }
        }
    }


    int intervalCount = 0;
    if (extraCount > 0) {
        refChain[ChainLen].intervalCount = intervalCount = readGamma(GAMMA, arr, &tmp_rpos);

        if (intervalCount != 0) {
            refChain[ChainLen].IOffset = tmp_rpos;//记录连续区间开始位置
            //跳过连续区间   (值可不读，len可能得读
            for (int i = 0; i < intervalCount; i++) {
                skipGamma(arr, &tmp_rpos);
                extraCount -= readGamma(GAMMA, arr, &tmp_rpos) + DEFAULT_MIN_INTERVAL_LENGTH;
            }
        }
    }
    if (extraCount > 0)
        refChain[ChainLen].residualOffset = tmp_rpos; //记录剩余区间开始位置
    int residualCount = extraCount;
    refChain[ChainLen].residualCount = residualCount;
    int previous = minTimestamp;
    //遍历
   // printf("here berfore travel\n");
    int index = 0;
    int ind = 0;
    int previous_v = -1;
    while (index < d) {
        //  printf("here berfore ref_nextValue\n");
        result = ref_nextValue(GAMMA, ZETA_3, x, arr, refChain, ind);



        timestamp = (int)(inv_v_func(readZeta_k2(d_Timestamps, t_pos)) + previous);

        //  if(x==112202)
        //         printf(" result:%d    timestamp:%d\n", result, timestamp);
        //  printf("%d,\n", result ); 
        if (result < 1 || result > MAX_LABEL) {
            printf("result:%d, %d\n", result, x);  return;
        }
        if (result != previous_v && result <= MAX_Vertex) {
            TimeAndP newVal;
            newVal.p = x;
            newVal.time = timestamp;
            uint64_t newValLL = toULL(newVal);
            uint64_t maskednewValLL = newValLL | 0x00000000FFFFFFFFULL;
            //  printf("d_timeAndP[x].time:%d\n", static_cast<int>(d_timeAndP[x] >> 32));
            if (static_cast<int>(d_timeAndP[x] >> 32) <= timestamp && atomicMin((uint64_t*)&d_timeAndP[result], maskednewValLL) > maskednewValLL) {
                atomicCAS((uint64_t*)&d_timeAndP[result], maskednewValLL, newValLL);

                if (atomicCAS(&d_Xa[result], 0, 1) == 0) {
                    int pos = atomicAdd(d_nextQueueSize, 1);
                    d_nextQueue[pos] = result;
                }
                d_Ca[result] = d_Ca[x] + 1;

                p[result] = x;

            }
        }
        previous_v = result;

        previous = timestamp;

        index++;
    }
    




}

__device__ void BFS_travel(int off, int i, uint64_t offsetGraph, uint64_t offsetTimestamps, bool* dv2, int* GAMMA, int* ZETA_3, int x, uint8_t* arr, uint64_t* s, uint8_t* d_Timestamps, uint64_t* t_pos,
    uint8_t* d_lowerBitsVector, SimpleSelect* d_selectUpper, uint8_t* d_t_lowerBitsVector, SimpleSelect* d_t_selectUpper, bool* d_Fa, int* d_Xa, int* d_time, int* p, int* d_Ca, int* changed,
    int* d_currentQueue, int* d_nextQueue, int* d_queueSize, int* d_nextQueueSize, uint64_t* d_timeAndP)
{
    // printf("here travel\n" );
    int x1 = x; //此线程编号 
  //  int n =4;
  //if(i==1)
    if (x1 > MAX_LABEL) return;


    // *s = get_position(GAMMA, arr, d_Offsets, x1);
   //  if (n == 31) printf("position:%lu", *s);
    // *t_pos = T_offset[x1];
    uint64_t ref_s;
    uint64_t tmp_s = getOffset(x1, gef_low_len, d_lowerBitsVector, d_selectUpper) - offsetGraph * 8;
    //  printf("tmp_s:%llu\n", tmp_s);
    uint64_t tmp_t = getOffset(x1, tef_low_len, d_t_lowerBitsVector, d_t_selectUpper) - offsetTimestamps * 8;
    // printf("错了错了\n");


    ref_s = tmp_s;
    int d, ref;
    d = readGamma(GAMMA, arr, &tmp_s);
    if (x1 == 794106)
        printf("d:%d\n", d);
    if (d == 0) return;

    //  printf("d:%d\n", d);
     //    cout << "d" << d << endl;
    ref = readUnary(arr, &tmp_s);
    // if (x1 == 112202||x1== 79869 || x1 == 79868 || x1 == 53542)
      //   printf("ref:%d\n",ref);
     //  tmp_s = *s;
    if (ref > 0) {
        //  printf("here travel before\n");
        BFS_get_dest(d, offsetGraph, dv2, GAMMA, ZETA_3, x1, arr, &ref_s, d_Timestamps, &tmp_t, d_lowerBitsVector, d_selectUpper, 1, d_Fa, d_Xa, d_time, p, d_Ca, changed, d_currentQueue, d_nextQueue, d_queueSize, d_nextQueueSize, d_timeAndP);

        return;
    }
    //7664
   //*s = 72151351;
  // *t_pos = 92443537;
   //*s = 66372778;
   //printf("s:\n", *s);

   //if(n==41*32 + 1)
  // printf("here n: %d\n",n);
    else {
        int result;
        int timestamp;
        //successors(0, arr, g_pos); ---------------------------------------------------------------------------------------------------------------------------------------

        uint64_t  multOffset = 0, IOffset = 0, residualOffset = 0;//存储重边列表、连续区间、剩余区间的开始位置的偏移量


        int
            //  refIndex, 
            extraCount;//剩余邻居数

        extraCount = d;
        int multiplesCount = 0; // 重边数量

        if (extraCount > 0) {

            multiplesCount = readGamma(GAMMA, arr, &tmp_s);
            //   cout << "multiplesCount" << multiplesCount << endl;

            if (multiplesCount != 0) {
                multOffset = tmp_s;
                //跳过重边列表    (值可不读，len可能得读
                for (int i = 0; i < multiplesCount; i++) {
                    skipGamma(arr, &tmp_s);
                    extraCount -= readGamma(GAMMA, arr, &tmp_s) + DEFAULT_MIN_MULTIPLE_LENGTH;

                }
            }
        }


        int intervalCount = 0;
        if (extraCount > 0) {
            intervalCount = readGamma(GAMMA, arr, &tmp_s);

            if (intervalCount != 0) {
                IOffset = tmp_s;//记录连续区间开始位置
                //跳过连续区间   (值可不读，len可能得读
                for (int i = 0; i < intervalCount; i++) {
                    skipGamma(arr, &tmp_s);
                    extraCount -= readGamma(GAMMA, arr, &tmp_s) + DEFAULT_MIN_INTERVAL_LENGTH;
                }
            }
        }
        if (extraCount > 0)
            residualOffset = tmp_s; //记录剩余区间开始位置
        int residualCount = extraCount;
        // printf("residualCount:%d", residualCount);

        int previous = minTimestamp;
        int previous_v = -1;
        // int cumt = 0;
        int currMult = 0, mlen = 0, m_prev, multiplesNum = 0;
        int currInterval = 0, Ilen = 0, I_prev, intervalNum = 0;
        int currResidual = 0, r_prev, residualNum = 0, flag = 0;
        int index = 0;
        while (index < d) {
            result = nextValue(GAMMA, ZETA_3, x1, arr,
                currMult, mlen, m_prev, &multOffset, multiplesCount, multiplesNum,
                currInterval, Ilen, I_prev, &IOffset, intervalCount, intervalNum,
                currResidual, r_prev, &residualOffset, residualCount, residualNum, flag);


            timestamp = (int)(inv_v_func(readZeta_k2(d_Timestamps, &tmp_t)) + previous);
            //   if (n == 31) printf("x:%d result:%d\n timestamp:%d\n", x1,result,timestamp);
               //printf("timestamp:%d\n", timestamp);
            if (result < 1 || result >  MAX_LABEL) {
                printf("result:%d, %d\n", result, x);
            }
            if (result != previous_v && result <= MAX_Vertex) {
                TimeAndP newVal;
                newVal.p = x;
                newVal.time = timestamp;
                uint64_t newValLL = toULL(newVal);
                uint64_t maskednewValLL = newValLL | 0x00000000FFFFFFFFULL;
                //  printf("d_timeAndP[x].time:%d\n", static_cast<int>(d_timeAndP[x] >> 32));
                if (static_cast<int>(d_timeAndP[x] >> 32) <= timestamp && atomicMin(&d_timeAndP[result], maskednewValLL) > maskednewValLL) {
                    //   printf("here\n");
                    atomicCAS(&d_timeAndP[result], maskednewValLL, newValLL);


                    if (atomicCAS(&d_Xa[result], 0, 1) == 0) {
                        int pos = atomicAdd(d_nextQueueSize, 1);
                        d_nextQueue[pos] = result;
                    }
                    d_Ca[result] = d_Ca[x] + 1;

                }
            }
            previous_v = result;
            previous = timestamp;
            // cout<<"result timestamp"<<result<<":"<<timestamp<<endl;
            // count_r++;
            index++;
        }



    }
}






 


__global__ void setDevicePointer(uint8_t* d_lowerBitsVector, uint8_t* d_upperBits, SimpleSelect* d_selectUpper, uint64_t SizeofUpperFile) {

    new (d_selectUpper) SimpleSelect(d_upperBits, SizeofUpperFile * 8);


}
__global__ void setTSimpleSelect(uint8_t* d_t_lowerBitsVector, uint8_t* d_t_upperBits, SimpleSelect* d_t_selectUpper, uint64_t SizeofTUpperFile) {

    new (d_t_selectUpper) SimpleSelect(d_t_upperBits, SizeofTUpperFile * 8);


}


 __global__ void simpleBfs(int level,   //LEVEL 有用吗？
    int off, int i, uint64_t offsetGraph, uint64_t offsetTimestamps,
    bool* dv2, int* GAMMA, int* ZETA_3, uint8_t* arr, uint64_t* s, uint8_t* d_Timestamps, uint64_t* t_pos,
    uint8_t* d_lowerBitsVector, SimpleSelect* d_selectUpper, uint8_t* d_t_lowerBitsVector, SimpleSelect* d_t_selectUpper,
    bool* d_Fa, bool* d_Fa2, int* d_Xa, int* d_time, int* p, int* d_Ca, int* changed,
    int* currentQueue, int* nextQueue, int* queueSize, int* nextQueueSize, uint64_t* d_timeAndP) { //Fa节点是否处于当前层次，Xa是否已被遍历，d_time节点被遍历的时间，p节点的前置节点，Ca距离 

    int thid = off + blockDim.x * blockIdx.x + threadIdx.x; //此线程编号 
  //  int n =4;
  //if(i==1) 
    //printf("n:%d\n", MAX_LABEL * (i + 1) / 2);
    if (thid >= *queueSize)  return;
    //  int valueChange = 0;


    if (thid < *queueSize) {
        int  v = currentQueue[thid];
        //有可能当前层Fa=1 ，但是d_Xa为1，没将Fa改为0，有影响吗？
      // printf(" %d, ", v);

    //   if (!d_Xa[thid]) {
         //  d_Xa[thid] = true; // Mark the node as visited


      //目前的思路： 不使用下面的循环，在之前的核函数的基础上，获取到result之后，进行循环内的条件判断。
        BFS_travel(off, i, offsetGraph, offsetTimestamps,
            dv2, GAMMA, ZETA_3, v, arr, s, d_Timestamps, t_pos,
            d_lowerBitsVector, d_selectUpper, d_t_lowerBitsVector, d_t_selectUpper,
            d_Fa2, d_Xa, d_time, p, d_Ca, changed,
            currentQueue, nextQueue, queueSize, nextQueueSize, d_timeAndP);



    }

}
 
 int main() {
     printf("in main");
     // return 0;
     TimeAndP tp;
     tp.time = 0x12345678;  // Example value for time
     tp.p = 0x9abcdef0;     // Example value for p

     // Cast the address of tp to a pointer to long long int
     long long int* tp_as_ll = reinterpret_cast<long long int*>(&tp);


     // Output the values
     std::cout << "Time: " << std::hex << tp.time << std::endl;
     std::cout << "P: " << std::hex << tp.p << std::endl;
     std::cout << "TimeAndP as long long int: " << *tp_as_ll << std::endl;
     std::cout << "TimeAndP as long long int using function: " << toULL(tp) << std::endl;

     size_t heapSize;

     // 调用 cudaDeviceGetLimit 函数查询动态内存分配堆的当前大小
     cudaError_t status = cudaDeviceGetLimit(&heapSize, cudaLimitMallocHeapSize);
     cudaDeviceSetLimit(cudaLimitMallocHeapSize, 256 * 1024 * 1024);
     size_t stackSize = 0;
     cudaDeviceSetLimit(cudaLimitStackSize, 2048);
     cudaDeviceGetLimit(&stackSize, cudaLimitStackSize);
     printf("Current stack size per thread: %zu bytes\n", stackSize);

     // 检查函数调用是否成功
     if (status != cudaSuccess) {
         printf("cudaDeviceGetLimit failed: %s\n", cudaGetErrorString(status));
         return -1;
     }

     // 输出当前堆大小
     printf("Current CUDA malloc heap size: %zu bytes\n", heapSize);



     // int* hostArray = new int[234 * 10000]; // 主机上的数组
      //加载gamma.in.16文件
     FILE* gamma_in_file = tgaOpenFile(gamma_file_path, "r");
     int gamma_value;
     int gif_i = 0;
     while (fscanf(gamma_in_file, "%d,", &gamma_value) == 1) {
         H_GAMMA[gif_i] = gamma_value;
         gif_i++;
     }
     /*
    for(int i=0;i<256*5;i++){
        cout<<GAMMA[i]<<endl;
    }  */
    //加载zeta.in.16文件
     FILE* zeta_in_file = tgaOpenFile(zeta_file_path, "r");
     int zeta_value;
     int zif_i = 0;
     while (fscanf(zeta_in_file, "%d,", &zeta_value) == 1) {
         H_ZETA_3[zif_i] = zeta_value;
         zif_i++;
     }
     /*
     for(int i=256*16;i<256*20;i++){
         cout<<ZETA_3[i]<<endl;
     }  */

     int v2_[1119] = { 0 };//节点0的邻居节点序号 

     FILE* file = tgaOpenFile(graph_file_path, "rb"); //10,019,548  54,523,326
     fseek(file, 0, SEEK_END);

     // 获取当前位置，即文件大小
     uint64_t SizeOfGraphFile = ftell(file);
     //printf("File size: %llu bytes\n", SizeOfGraphFile);

     // 将文件指针重新定位到文件的开始位置
     rewind(file);
     uint64_t i;
     uint8_t* h_Graph = (uint8_t*)malloc(SizeOfGraphFile * sizeof(uint8_t));
     for (i = 0; i < SizeOfGraphFile; i++)
         assert(fread(&h_Graph[i], sizeof(uint8_t), 1, file) == 1);
     fclose(file);
     //加载时间戳文件
     FILE* file2 = tgaOpenFile(timestamps_file_path, "rb"); //13,701,410  22,549,005
     if (file2 == NULL) {
         printf("Failed to open the file.\n");

     }
     fseek(file2, 0, SEEK_END);

     // 获取当前位置，即文件大小
     uint64_t SizeOfTimestampsFile = ftell(file2);
    // printf("File size: %llu bytes\n", SizeOfTimestampsFile);
     int h_glen = log2(SizeOfGraphFile * 8 / MAX_LABEL);
     int h_tlen = log2(SizeOfTimestampsFile * 8 / MAX_LABEL);

     CHECK(cudaMemcpyToSymbol(gef_low_len, &h_glen, sizeof(int)));
     CHECK(cudaMemcpyToSymbol(tef_low_len, &h_tlen, sizeof(int)));
     // 将文件指针重新定位到文件的开始位置
     rewind(file2);
     uint8_t* Timestamps = (uint8_t*)malloc(SizeOfTimestampsFile * sizeof(uint8_t));
     for (i = 0; i < SizeOfTimestampsFile; i++)
         assert(fread(&Timestamps[i], sizeof(uint8_t), 1, file2) == 1);
     fclose(file2);
      
     FILE* low_file = tgaOpenFile(g_low_file_path, "rb"); // 2,262,374 
     if (low_file == NULL) {
         printf("Failed to open the low file.\n");

     }
     fseek(low_file, 0, SEEK_END);

     // 获取当前位置，即文件大小
     uint64_t SizeofLowFile = ftell(low_file);
    // printf("File size: %llu bytes\n", SizeofLowFile);

     // 将文件指针重新定位到文件的开始位置
     rewind(low_file);
     uint8_t* lowerBits = (uint8_t*)malloc(SizeofLowFile * sizeof(uint8_t));
     for (i = 0; i < SizeofLowFile; i++)
         assert(fread(&lowerBits[i], sizeof(uint8_t), 1, low_file) == 1);
     fclose(low_file);
     FILE* upper_file = tgaOpenFile(g_upper_file_path, "rb"); //749,160
     if (upper_file == NULL) {
         printf("Failed to open the upper file.\n");

     }
     fseek(upper_file, 0, SEEK_END);

     // 获取当前位置，即文件大小
     uint64_t SizeofUpperFile = ftell(upper_file);
     //printf("File size: %llu bytes\n", SizeofUpperFile);

     // 将文件指针重新定位到文件的开始位置
     rewind(upper_file);
     uint8_t* upperBits = (uint8_t*)malloc(SizeofUpperFile * sizeof(uint8_t));
     for (i = 0; i < SizeofUpperFile; i++)
         assert(fread(&upperBits[i], sizeof(uint8_t), 1, upper_file) == 1);
     fclose(upper_file);
     uint8_t* d_lowerBitsVector;
     uint8_t* d_upperBits;

     CHECK(cudaMalloc((void**)&d_upperBits, SizeofUpperFile * sizeof(uint8_t)));
     CHECK(cudaMalloc((void**)&d_lowerBitsVector, SizeofLowFile * sizeof(uint8_t)));
     CHECK(cudaMemcpy(d_lowerBitsVector, lowerBits, SizeofLowFile * sizeof(uint8_t), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(d_upperBits, upperBits, SizeofUpperFile * sizeof(uint8_t), cudaMemcpyHostToDevice));
     //  uint64_t max_size_upper = (g_offset[MAX_LABEL - 1] >> efml.l) + (MAX_LABEL - 1);
     SimpleSelect* d_selectUpper;
     CHECK(cudaMalloc((void**)&d_selectUpper, sizeof(SimpleSelect)));
     //时间戳的偏移列表
     FILE* t_low_file = tgaOpenFile(t_low_file_path, "rb"); // 2,262,374 1,939,177 
     if (t_low_file == NULL) {
         printf("Failed to open the low file.\n");

     }
     fseek(t_low_file, 0, SEEK_END);

     // 获取当前位置，即文件大小
     uint64_t SizeofTLowFile = ftell(t_low_file);
     //printf("File size: %llu bytes\n", SizeofTLowFile);

     // 将文件指针重新定位到文件的开始位置
     rewind(t_low_file);
     uint8_t* t_lowerBits = (uint8_t*)malloc(SizeofTLowFile * sizeof(uint8_t));
     for (i = 0; i < SizeofTLowFile; i++)
         assert(fread(&t_lowerBits[i], sizeof(uint8_t), 1, t_low_file) == 1);
     fclose(t_low_file);
     FILE* t_upper_file = tgaOpenFile(t_upper_file_path, "rb"); //749,160 675,525
     if (t_upper_file == NULL) {
         printf("Failed to open the upper file.\n");

     }
     fseek(t_upper_file, 0, SEEK_END);

     // 获取当前位置，即文件大小
     uint64_t SizeofTUpperFile = ftell(t_upper_file);
    // printf("File size: %llu bytes\n", SizeofTUpperFile);

     // 将文件指针重新定位到文件的开始位置
     rewind(t_upper_file);
     uint8_t* t_upperBits = (uint8_t*)malloc(SizeofTUpperFile * sizeof(uint8_t));
     for (i = 0; i < SizeofTUpperFile; i++)
         assert(fread(&t_upperBits[i], sizeof(uint8_t), 1, t_upper_file) == 1);
     fclose(t_upper_file);
     uint8_t* d_t_lowerBitsVector;
     uint8_t* d_t_upperBits;

     CHECK(cudaMalloc((void**)&d_t_upperBits, SizeofTUpperFile * sizeof(uint8_t)));
     CHECK(cudaMalloc((void**)&d_t_lowerBitsVector, SizeofTLowFile * sizeof(uint8_t)));
     CHECK(cudaMemcpy(d_t_lowerBitsVector, t_lowerBits, SizeofTLowFile * sizeof(uint8_t), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(d_t_upperBits, t_upperBits, SizeofTUpperFile * sizeof(uint8_t), cudaMemcpyHostToDevice));
     //  uint64_t max_size_upper = (g_offset[MAX_LABEL - 1] >> efml.l) + (MAX_LABEL - 1);
     SimpleSelect* d_t_selectUpper;
     CHECK(cudaMalloc((void**)&d_t_selectUpper, sizeof(SimpleSelect)));


     //   return 0;


   // int previous = 0;
  //  long long totalDuration = 0;
     auto start1 = std::chrono::high_resolution_clock::now();
     //分配GPU共享内存空间
     int* GAMMA, * ZETA_3;
     uint8_t* d_Timestamps;
     uint8_t* Graph;

     cudaEvent_t c_start, c_stop;
     CHECK(cudaEventCreate(&c_start));
     CHECK(cudaEventCreate(&c_stop));


     CHECK(cudaMalloc((void**)&GAMMA, 256 * 256 * sizeof(int)));
     CHECK(cudaMalloc((void**)&ZETA_3, 256 * 256 * sizeof(int)));

     bool* dv2;
     CHECK(cudaMalloc((void**)&dv2, MAX_LABEL * sizeof(bool)));
     CHECK(cudaMalloc((void**)&d_Timestamps, SizeOfTimestampsFile * sizeof(uint8_t)));
     CHECK(cudaMalloc((void**)&Graph, SizeOfGraphFile * sizeof(uint8_t)));


     // int* dest;
    //  CHECK(cudaMalloc((void**)&dest, 4095 * sizeof(int)));

     auto start2 = std::chrono::high_resolution_clock::now();



     CHECK(cudaMemcpy(d_Timestamps, Timestamps, SizeOfTimestampsFile * sizeof(uint8_t), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(Graph, h_Graph, SizeOfGraphFile * sizeof(uint8_t), cudaMemcpyHostToDevice));

     CHECK(cudaMemcpy(GAMMA, H_GAMMA, 256 * 256 * sizeof(int), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(ZETA_3, H_ZETA_3, 256 * 256 * sizeof(int), cudaMemcpyHostToDevice));
     //CHECK(cudaMemcpy(dv2, v2_, 1119 * sizeof(int), cudaMemcpyHostToDevice));//dv2用来检验正确性
     cudaMemset(dv2, 0, MAX_LABEL * sizeof(bool));
     uint64_t* t_pos;
     cudaMalloc((void**)&t_pos, 1 * sizeof(uint64_t));
     uint64_t* g_pos;
     cudaMalloc((void**)&g_pos, 1 * sizeof(uint64_t));
     cudaMemset(g_pos, 0, sizeof(uint64_t));
     setDevicePointer << <1, 1 >> > (d_lowerBitsVector, d_upperBits, d_selectUpper, SizeofUpperFile);
     setTSimpleSelect << <1, 1 >> > (d_t_lowerBitsVector, d_t_upperBits, d_t_selectUpper, SizeofTUpperFile);
     CHECK(cudaGetLastError());
     CHECK(cudaDeviceSynchronize());
     int* changed;
     CHECK(cudaMallocManaged(&changed, sizeof(int)));
     bool* d_Fa, * d_Fa2;
     int* d_Xa;
     CHECK(cudaMalloc((void**)&d_Fa, MAX_Vertex * sizeof(bool)));
     CHECK(cudaMalloc((void**)&d_Xa, MAX_Vertex * sizeof(int)));
     CHECK(cudaMalloc((void**)&d_Fa2, MAX_Vertex * sizeof(bool)));
     CHECK(cudaMemset(d_Fa2, 0, MAX_Vertex * sizeof(bool)));
     CHECK(cudaMemset(d_Fa, 0, MAX_Vertex * sizeof(bool)));
     CHECK(cudaMemset(d_Xa, 0, MAX_Vertex * sizeof(int)));
     int* d_Ca;
     std::vector<int> distance(MAX_Vertex, std::numeric_limits<int>::max());
     CHECK(cudaMalloc((void**)&d_Ca, MAX_Vertex * sizeof(int)));
     CHECK(cudaMemcpy(d_Ca, distance.data(), MAX_Vertex * sizeof(int), cudaMemcpyHostToDevice));
     int* d_time;
     CHECK(cudaMalloc((void**)&d_time, MAX_Vertex * sizeof(int)));
     CHECK(cudaMemcpy(d_time, distance.data(), MAX_Vertex * sizeof(int), cudaMemcpyHostToDevice));
     int* p;
     CHECK(cudaMalloc((void**)&p, MAX_Vertex * sizeof(int)));
     CHECK(cudaMemset(p, 0, MAX_Vertex * sizeof(int)));
     //设置源节点
     int startVertex = 1;
     int s_Ca = 0;
     bool  s_Fa = true;
     int s_time = -1;
     uint64_t* d_timeAndP;
     CHECK(cudaMalloc((void**)&d_timeAndP, MAX_Vertex * sizeof(uint64_t)));
     std::vector<uint64_t> h_timeAndP(MAX_Vertex);
     TimeAndP tmp;
     for (int i = 0; i < MAX_Vertex; ++i) {

         tmp.p = 0;
         tmp.time = distance[i];
         h_timeAndP[i] = toULL(tmp);

     }
     tmp.p = 0;
     tmp.time = 0;
     h_timeAndP[startVertex] = toULL(tmp);
     CHECK(cudaMemcpy(d_timeAndP, h_timeAndP.data(), MAX_Vertex * sizeof(uint64_t), cudaMemcpyHostToDevice));

     CHECK(cudaMemcpy(&d_Fa[startVertex], &s_Fa, sizeof(bool), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(&d_Ca[startVertex], &s_Ca, sizeof(int), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(&d_time[startVertex], &s_time, sizeof(int), cudaMemcpyHostToDevice));
     int* d_currentQueue, * d_nextQueue;
     int* d_queueSize, * d_nextQueueSize;
     CHECK(cudaMalloc((void**)&d_currentQueue, MAX_Vertex * sizeof(int)));
     CHECK(cudaMalloc((void**)&d_nextQueue, MAX_Vertex * sizeof(int)));
     CHECK(cudaMalloc((void**)&d_queueSize, sizeof(int)));
     CHECK(cudaMalloc((void**)&d_nextQueueSize, sizeof(int)));
     int queueSize = 1;
     int nextQueueSize = 0;
     CHECK(cudaMemcpy(d_queueSize, &queueSize, sizeof(int), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(d_nextQueueSize, &nextQueueSize, sizeof(int), cudaMemcpyHostToDevice));
     CHECK(cudaMemcpy(&d_currentQueue[0], &startVertex, sizeof(int), cudaMemcpyHostToDevice));

     // CHECK(cudaMemcpy(d_t_lowerBitsVector, t_lowerBits, 1939177 * sizeof(uint8_t), cudaMemcpyHostToDevice));
    //  CHECK(cudaMemcpy(d_t_upperBits, t_upperBits, 675525 * sizeof(uint8_t), cudaMemcpyHostToDevice));

     printf("Starting simple parallel bfs.\n");



     auto BFS_start = std::chrono::steady_clock::now();
     *changed = 1;
     int level = 0;
     int b_off = 0;
     while (queueSize > 0  /*&&level<=1*/) {
       //  printf("level :%d\n", level);
       //  printf("queueSize :%d\n", queueSize);
         b_off = 0;
         *changed = 0;



         //    cuLaunchKernel(simpleBfs, 800, 1, 1,  512, 1, 1, 0, 0, args, 0)  ;

         simpleBfs << <queueSize / 512 + 1, 512, 0 >> > (level, b_off, i, 0, 0, dv2, GAMMA, ZETA_3, Graph, g_pos, d_Timestamps, t_pos,
             d_lowerBitsVector, d_selectUpper, d_t_lowerBitsVector, d_t_selectUpper,
             d_Fa, d_Fa2, d_Xa, d_time, p, d_Ca, changed,
             d_currentQueue, d_nextQueue, d_queueSize, d_nextQueueSize, d_timeAndP
             );
         CHECK(cudaGetLastError());
         CHECK(cudaDeviceSynchronize());

         // cudaEventRecord(stop, streams[i]);
         // cudaEventSynchronize(stop);
         // cudaEventElapsedTime(&milliseconds, start, stop);


         std::swap(d_currentQueue, d_nextQueue);
         std::swap(d_queueSize, d_nextQueueSize);
         CHECK(cudaMemcpy(&queueSize, d_queueSize, sizeof(int), cudaMemcpyDeviceToHost));
         //  printf("queueSize:%d\n", queueSize);
         nextQueueSize = 0;
         CHECK(cudaMemcpy(d_nextQueueSize, &nextQueueSize, sizeof(int), cudaMemcpyHostToDevice));
         CHECK(cudaMemset(d_Fa2, 0, MAX_Vertex * sizeof(bool)));

         CHECK(cudaMemset(d_Xa, 0, MAX_Vertex * sizeof(int)));

         level++;

     }
     auto BFS_end = std::chrono::steady_clock::now();
     long BFS_duration = std::chrono::duration_cast<std::chrono::milliseconds>(BFS_end - BFS_start).count();
     printf("BFS Elapsed time in milliseconds : %li ms.\n", BFS_duration);
     cudaFree(d_Fa);
     cudaFree(d_Fa2);
     cudaFree(d_Xa);
     cudaFree(d_time);
     cudaFree(d_Ca);
     cudaFree(p);
     cudaFree(d_currentQueue);
     cudaFree(d_nextQueue);
     cudaFree(d_queueSize);
     cudaFree(d_nextQueueSize);


     cudaFree(GAMMA);
     cudaFree(ZETA_3);
     cudaFree(d_Timestamps);
     cudaFree(Graph);
     cudaFree(t_pos);
     return 0;
 }
