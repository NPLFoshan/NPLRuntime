

if(ANDROID)
	add_library(libcurl INTERFACE)
    target_include_directories(libcurl INTERFACE include/android)
    target_link_libraries(libcurl INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/prebuilt/android/${CMAKE_ANDROID_ARCH_ABI}/libcurl.a para_libssl para_libcrypto)

elseif(IOS)
	add_library(libcurl INTERFACE)
	target_include_directories(libcurl INTERFACE include/ios)
	target_link_libraries(libcurl INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/prebuilt/ios/libcurl.a para_libssl para_libcrypto)
elseif(WIN32)
	option(BUILD_CURL_EXE OFF)
	option(BUILD_CURL_TESTS OFF)
	
	if(NPLRUNTIME_STATIC_LIB)
		set(CURL_STATICLIB TRUE)
	endif()
	
	add_subdirectory(code/curl-7.47.1)
	
	if(NPLRUNTIME_STATIC_LIB)
		target_compile_definitions(libcurl INTERFACE CURL_STATICLIB)
	endif()
	
	target_include_directories(libcurl INTERFACE "${CURL_SOURCE_DIR}/include/")
	target_include_directories(libcurl INTERFACE "${CURL_BINARY_DIR}/include/curl/")
	
elseif (NPLRUNTIME_SERVER)
	FIND_PACKAGE(CURL REQUIRED)
	 add_library(libcurl INTERFACE)
	 target_include_directories(libcurl INTERFACE ${CURL_INCLUDE_DIR})
	 target_link_libraries(libcurl INTERFACE ${CURL_LIBRARIES} para_libssl para_libcrypto)
elseif(APPLE)
	 FIND_PACKAGE(CURL REQUIRED)
	 add_library(libcurl INTERFACE)
	 target_include_directories(libcurl INTERFACE ${CURL_INCLUDE_DIR})
	 target_link_libraries(libcurl INTERFACE ${CURL_LIBRARIES} para_libssl para_libcrypto)
else()  # linux
	 FIND_PACKAGE(CURL REQUIRED)
	 add_library(libcurl INTERFACE)
	 target_include_directories(libcurl INTERFACE ${CURL_INCLUDE_DIR})
	 target_link_libraries(libcurl INTERFACE ${CURL_LIBRARIES} para_libssl para_libcrypto)
endif()