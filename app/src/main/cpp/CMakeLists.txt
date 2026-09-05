cmake_minimum_required(VERSION 3.22.1)

project("ufr2engine")

add_library(ufr2engine SHARED ufr2engine.cpp)

target_compile_features(ufr2engine PRIVATE cxx_std_17)

target_link_libraries(ufr2engine android log)
