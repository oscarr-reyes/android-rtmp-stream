cmake_minimum_required(VERSION 3.5)
project(rtmp)


## Set location of prebuilt mobile-ffmpeg
set(MOBILE-FFMPEG "${PROJECT_SOURCE_DIR}/src/main/jni/ffmpeg/android-${ANDROID_ABI}")

if (ANDROID_ABI STREQUAL "armeabi-v7a")
    set(MOBILE-FFMPEG "${PROJECT_SOURCE_DIR}/src/main/jni/ffmpeg/android-arm")
endif ()

if (ANDROID_ABI STREQUAL "arm64-v8a")
    set(MOBILE-FFMPEG "${PROJECT_SOURCE_DIR}/src/main/jni/ffmpeg/android-arm64")
endif ()

## Set include directories
include_directories(${MOBILE-FFMPEG}/ffmpeg/include)

add_library(mobile-ffmpeg-muxer SHARED src/main/jni/ffmpeg_muxer.c)

add_library(libavformat SHARED IMPORTED)
set_target_properties(libavformat PROPERTIES IMPORTED_LOCATION ${MOBILE-FFMPEG}/ffmpeg/lib/libavformat.so)

add_library(libavcodec SHARED IMPORTED)
set_target_properties(libavcodec PROPERTIES IMPORTED_LOCATION ${MOBILE-FFMPEG}/ffmpeg/lib/libavcodec.so)

add_library(libavutil SHARED IMPORTED)
set_target_properties(libavutil PROPERTIES IMPORTED_LOCATION ${MOBILE-FFMPEG}/ffmpeg/lib/libavutil.so)

## Link libraries
find_library(log-lib log)
if (EXISTS ${log-lib})
    include_directories(${log-lib}/include)
    target_link_libraries(mobile-ffmpeg-muxer libavformat libavcodec libavutil ${log-lib})
else ()
    target_link_libraries(mobile-ffmpeg-muxer libavformat libavcodec libavutil)
endif ()

## Include external libraries in APK
set(FFMPEG_LIBS
        ${MOBILE-FFMPEG}/ffmpeg/lib/libavformat.so
        ${MOBILE-FFMPEG}/ffmpeg/lib/libavcodec.so
        ${MOBILE-FFMPEG}/ffmpeg/lib/libavutil.so
        ${MOBILE-FFMPEG}/ffmpeg/lib/libavdevice.so
        ${MOBILE-FFMPEG}/ffmpeg/lib/libswresample.so)

add_custom_command(TARGET mobile-ffmpeg-muxer POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy ${FFMPEG_LIBS} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
