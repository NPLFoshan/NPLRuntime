include(cmake/ucm.cmake)

cmake_minimum_required(VERSION 3.14)

set (CMAKE_CXX_STANDARD 11)
set (CMAKE_C_STANDARD 11)


set(NPLRUNTIME_PROJECT_NAME "NPLRuntime" CACHE STRING "Custom Project Name")

option(PARAENGINE_CLIENT_DLL "Generate Dll Or Exe" OFF)
option(NPLRUNTIME_STATIC_LIB "static lib or not" OFF)
option(NPLRUNTIME_ICONV "include iconv" ON)
option(NPLRUNTIME_SERVER "Generate server ver" OFF)
option(ANDROID_STUDIO "Generate AndroidStudio ver" OFF)
option(NPLRUNTIME_OCE "include NplOce" OFF)


project(${NPLRUNTIME_PROJECT_NAME})

if (NPLRUNTIME_SERVER)
	set(NPLRUNTIME_RENDERER "NULL" CACHE STRING "Render API")
    set_property(CACHE NPLRUNTIME_RENDERER PROPERTY STRINGS OPENGL NULL)
elseif(ANDROID) 
	if (ANDROID_STUDIO)
		set(CMAKE_ANDROID_STL_TYPE "llvm-libc++_shared")

		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-format-security>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-inconsistent-missing-override>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-gnu-include-next>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-null-conversion>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wtautological-undefined-compare>)
	else()
		set(CMAKE_ANDROID_API_MIN 21)
		set(CMAKE_ANDROID_API 21)
		#set(CMAKE_ANDROID_STL_TYPE "gnustl_shared")
		set(CMAKE_ANDROID_STL_TYPE "llvm-libc++_shared")
		set(CMAKE_ANDROID_ARCH_ABI "armeabi-v7a")
		set(CMAKE_ANDROID_NATIVE_LIB_DIRECTORIES  ${CMAKE_BINARY_DIR}/lib/debug)
		set(CMAKE_ANDROID_ARM_NEON OFF)
		#add_compile_options(-fexceptions -frtti -Wno-psabi -fsigned-char)
		add_definitions(-D__ANDROID__ -DANDROID)

		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-fexceptions>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-frtti>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-fsigned-char>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-std=c++11>)
		# Disable everything for fix compile errors.
		#add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-everything>)
		#add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wmacro-redefined>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-inconsistent-missing-override>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-gnu-include-next>)
		add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-null-conversion>)
	endif()

	set(NPLRUNTIME_RENDERER "OPENGL" CACHE STRING "Render API")
	set_property(CACHE NPLRUNTIME_RENDERER PROPERTY STRINGS OPENGL)
elseif(IOS)
    set(NPLRUNTIME_RENDERER "OPENGL" CACHE STRING "Render API")
    set_property(CACHE NPLRUNTIME_RENDERER PROPERTY STRINGS OPENGL NULL)   
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-address-of-temporary>)
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-inconsistent-missing-override>)
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-gnu-include-next>)
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-null-conversion>)
elseif(APPLE)
    set(NPLRUNTIME_RENDERER "OPENGL" CACHE STRING "Render API")
    set_property(CACHE NPLRUNTIME_RENDERER PROPERTY STRINGS OPENGL NULL)
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-address-of-temporary>)
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-inconsistent-missing-override>)
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-gnu-include-next>)
	add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-Wno-null-conversion>)
elseif(WIN32)
    # Ugly warnings
    IF(MSVC)
        ADD_DEFINITIONS(-D_CRT_SECURE_NO_WARNINGS)
        ADD_DEFINITIONS(-D_SCL_SECURE_NO_WARNINGS)
    ENDIF()
    set(NPLRUNTIME_RENDERER "OPENGL" CACHE STRING "Render API")
    set_property(CACHE NPLRUNTIME_RENDERER PROPERTY STRINGS DIRECTX OPENGL NULL)
else()
    set(NPLRUNTIME_RENDERER "NULL" CACHE STRING "Render API")
    set_property(CACHE NPLRUNTIME_RENDERER PROPERTY STRINGS OPENGL NULL)
endif()

if (APPLE)
    if(NPLRUNTIME_SERVER)
        option(MAC_SERVER "server mode" ON)
    else()
        option(MAC_SERVER "server mode" OFF)
    endif()
endif()


if (NPLRUNTIME_SERVER)
	option(NPLRUNTIME_LUAJIT21 "build luajit21 support GC64" ON)
    option(NPLRUNTIME_SUPPORT_FBX "support FBX files (assimp)" OFF)
    option(NPLRUNTIME_PHYSICS "include physics" OFF)
    option(NPLRUNTIME_LUAJIT20   "build luajit2.0.4" OFF)
    option(NPLRUNTIME_LUA51   "build lua5.1.5" OFF)
	option(NPLRUNTIME_AUDIO "include audio" OFF)
	
# Win32 Default Option
elseif(WIN32)
	option(NPLRUNTIME_LUAJIT21 "build luajit21 support GC64" OFF)
	option(NPLRUNTIME_LUAJIT20   "build luajit2.0.4" ON)
	option(NPLRUNTIME_LUA51   "build lua5.1.5" OFF)
	option(NPLRUNTIME_SUPPORT_FBX "support FBX files (assimp)" ON)
	option(NPLRUNTIME_PHYSICS "include physics" ON)
	option(NPLRUNTIME_AUDIO "include audio" ON)
	option(NPLRUNTIME_HAQI_LAUNCHER "haqi launcher" OFF)
elseif(IOS)
    option(NPLRUNTIME_LUAJIT21 "build luajit21 support GC64" ON)
    option(NPLRUNTIME_SUPPORT_FBX "support FBX files (assimp)" ON)
    option(NPLRUNTIME_PHYSICS "include physics" ON)
    option(NPLRUNTIME_LUAJIT20   "build luajit2.0.4" OFF)
    option(NPLRUNTIME_LUA51   "build lua5.1.5" OFF)	
	option(NPLRUNTIME_AUDIO "include audio" ON)
	#option(NPLRUNTIME_OCE "include NplOce" OFF)
elseif (MAC_SERVER)
	option(NPLRUNTIME_LUAJIT21 "build luajit21 support GC64" ON)
    option(NPLRUNTIME_SUPPORT_FBX "support FBX files (assimp)" OFF)
    option(NPLRUNTIME_PHYSICS "include physics" OFF)
    option(NPLRUNTIME_LUAJIT20   "build luajit2.0.4" OFF)
    option(NPLRUNTIME_LUA51   "build lua5.1.5" OFF)
	option(NPLRUNTIME_AUDIO "include audio" OFF)
# OSX Default Option
elseif(APPLE)
	option(NPLRUNTIME_LUAJIT21 "build luajit21 support GC64" ON)
	option(NPLRUNTIME_SUPPORT_FBX "support FBX files (assimp)" ON)
	option(NPLRUNTIME_PHYSICS "include physics" ON)
	option(NPLRUNTIME_LUAJIT20   "build luajit2.0.4" OFF)
    option(NPLRUNTIME_LUA51   "build lua5.1.5" OFF)	
	option(NPLRUNTIME_AUDIO "include audio" ON)
	option(NPLRUNTIME_HAQI_LAUNCHER "haqi launcher" OFF)
elseif (ANDROID)
	option(NPLRUNTIME_LUAJIT21 "build luajit21 support GC64" ON)
	option(NPLRUNTIME_SUPPORT_FBX "support FBX files (assimp)" ON)
	option(NPLRUNTIME_LUAJIT20   "build luajit2.0.4" OFF)
	option(NPLRUNTIME_LUA51   "build lua5.1.5" OFF)
	option(NPLRUNTIME_PHYSICS "include physics" ON)
	option(NPLRUNTIME_AUDIO "include audio" ON)
else()
    option(NPLRUNTIME_LUAJIT21 "build luajit21 support GC64" ON)
    option(NPLRUNTIME_SUPPORT_FBX "support FBX files (assimp)" OFF)
    option(NPLRUNTIME_PHYSICS "include physics" OFF)
    option(NPLRUNTIME_LUAJIT20   "build luajit2.0.4" ON)
    option(NPLRUNTIME_LUA51   "build lua5.1.5" OFF)
	option(NPLRUNTIME_AUDIO "include audio" OFF)
endif()

if(APPLE)
    # making executable relocatable 
    set(CMAKE_MACOSX_RPATH TRUE)
else()
    # making executable relocatable 
    SET(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
    SET(CMAKE_BUILD "$ORIGIN/")
endif()

# Using static lib for other modules if main NPLRuntime is built as static lib. 
if(NPLRUNTIME_STATIC_LIB OR ANDROID OR IOS)
	set(SQLITE_STATICLIB TRUE)
	set(PHYSICS_STATICLIB TRUE)
	set(AUDIO_STATICLIB TRUE)
endif()

# This is the directory to keep all binary
if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
	set(OUTPUT_BIN_DIR   ${PROJECT_SOURCE_DIR}/../ParaWorld/bin64/)
else()
	set(OUTPUT_BIN_DIR   ${PROJECT_SOURCE_DIR}/../ParaWorld/bin32/)
endif()
 

# Output directory
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)

if (MSVC)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_DEBUG ${CMAKE_BINARY_DIR}/bin)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR}/bin)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_MinSizeRel ${CMAKE_BINARY_DIR}/bin)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RelWithDebInfo ${CMAKE_BINARY_DIR}/bin)
endif (MSVC)

set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -DDEBUG -D_DEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -DDEBUG -D_DEBUG")



if(${NPLRUNTIME_RENDERER} STREQUAL "NULL")
	
else()
	option(PARAENGINE_CLIENT_DLL "Generate Dll Or Exe" OFF)

endif()

if (NOT ANDROID AND NOT NPLRUNTIME_STATIC_LIB)
	set(NPLRUNTIME_NPLMono2 TRUE)
endif()

if (NPLRUNTIME_SERVER)
	set(NPLRUNTIME_NPLMono2 TRUE)
endif ()


if(MSVC)
    ucm_set_runtime(STATIC)
endif()


add_subdirectory(externals)
add_subdirectory(Plugins)
add_subdirectory(ParaEngine)
add_subdirectory(RenderSystem)
add_subdirectory(Platform)

#add_subdirectory(PhysicsBT)
#add_subdirectory(ParaEngineClientApp)

if(PARAENGINE_CLIENT_DLL)
	if(NOT NPLRUNTIME_STATIC_LIB) 
			# this app is for testing the client dll
		add_subdirectory (ParaEngineClientApp)
	endif()
endif()

# if(WIN32)
# 	if (NPLRUNTIME_HAQI_LAUNCHER) 
# 		add_subdirectory (${CLIENT_SOURCE_DIR}/trunk/HaqiLauncher ${CLIENT_BINARY_DIR}/HaqiLauncher)
# 		add_subdirectory (${SERVER_SOURCE_DIR}/AutoUpdater ${CLIENT_BINARY_DIR}/AutoUpdater)
# 	endif()
# endif()
