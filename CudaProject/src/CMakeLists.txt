cmake_minimum_required(VERSION 3.3)
project(cuda)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

find_package(CUDA QUIET REQUIRED)
# Pass options to NVCC
set(
        CUDA_NVCC_FLAGS
        ${CUDA_NVCC_FLAGS};
        -O3 -gencode arch=compute_30,code=sm_30
)

# Specify include directories
include_directories(
        kernels
        utility
)


set(SOURCE_FILES common.h)
cuda_add_executable(cuda_hello-test main.cu greeter.cpp ${SOURCE_FILES})
cuda_add_executable(cuda_vector_sum-test cuda-vector-sum-test.cu ${SOURCE_FILES})
cuda_add_executable(cuda_gpu_info-test gpu-info.cu ${SOURCE_FILES})