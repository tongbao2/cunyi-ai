cmake_minimum_required(VERSION 3.22.1)
project(cunyi_doctor)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# llama.cpp (local source)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp-src/ggml ggml_build)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp-src llama_build)

add_library(cunyi_doctor SHARED
    llama_jni.cpp
)

target_include_directories(cunyi_doctor PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp-src
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp-src/ggml/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp-src/include
)

target_link_libraries(cunyi_doctor
    PRIVATE
    llama
    ggml
    log
)
