#pragma once

#include <cstdlib>
#include <cstdio>
#include <string>

#ifndef TGA_DEFAULT_COMPRESS_ROOT
#define TGA_DEFAULT_COMPRESS_ROOT "../../../.."
#endif

inline std::string tgaCompressRoot() {
    const char* configured = std::getenv("TGA_COMPRESS_ROOT");
    std::string root = configured != nullptr && configured[0] != '\0'
            ? configured
            : TGA_DEFAULT_COMPRESS_ROOT;
    while (!root.empty() && (root.back() == '/' || root.back() == '\\')) {
        root.pop_back();
    }
    return root;
}

inline std::string tgaResolvePath(const char* relativePath) {
    return tgaCompressRoot() + "/" + relativePath;
}

inline FILE* tgaOpenFile(const char* path, const char* mode) {
    FILE* file = std::fopen(path, mode);
    if (file == nullptr) {
        std::fprintf(
                stderr,
                "Cannot open required file: %s (set TGA_COMPRESS_ROOT if needed)\n",
                path);
        std::exit(EXIT_FAILURE);
    }
    return file;
}

#define TGA_DEFINE_PATH(symbol, relativePath) \
    static const std::string symbol##_storage = tgaResolvePath(relativePath); \
    static const char* const symbol = symbol##_storage.c_str()
