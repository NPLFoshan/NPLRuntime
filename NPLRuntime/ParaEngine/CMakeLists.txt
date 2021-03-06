
include(${PROJECT_SOURCE_DIR}/cmake/ucm.cmake)
include(${PROJECT_SOURCE_DIR}/cmake/ConfigBoost.cmake)

# includes
set(NPLRUNTIME_INCLUDES)
# Libraries 
set(NPLRUNTIME_LIBS)
##############################


ucm_add_dirs(
    Framework
    dirmonitor
    Core
    BMaxModel
    renderer
    protocol
    ParaXModel
    IO
    curllua
    debugtools
    InfoCenter
    BlockEngine
    math
    NPL
    ParaScriptBindings
    2dengine
    PaintEngine
    3dengine
    util
    WebSocket
    terrain
	AutoRigger
TO sources_commmon RECURSIVE
)

if(NPLRUNTIME_RENDERER STREQUAL "OPENGL")
    set(RenderSystem RenderSystemOpenGL)
    ucm_add_dirs(
        OpenGLWrapper
    TO sources_rendersystem RECURSIVE
    )
    file(GLOB SHADER_FILES ${CMAKE_CURRENT_SOURCE_DIR}/shaders/opengl_spec/*.fx)
    foreach( src_file ${SHADER_FILES} )	
        embed_resources_abs(Resource ${src_file})
        list(APPEND EmbeddedResource_Files_OPENGL ${Resource})
    endforeach( src_file ${SHADER_FILES} )
    list(APPEND EmbeddedResource_Files_OPENGL ResourceEmbedded.cpp)
    SOURCE_GROUP("ResEmbedded" FILES ${EmbeddedResource_Files_OPENGL})
    list(APPEND sources_rendersystem ${EmbeddedResource_Files_OPENGL})

	SOURCE_GROUP("ShaderFiles" FILES ${SHADER_FILES})
	SET_SOURCE_FILES_PROPERTIES(${SHADER_FILES} PROPERTIES HEADER_FILE_ONLY TRUE)
	list(APPEND sources_rendersystem ${SHADER_FILES})

elseif(NPLRUNTIME_RENDERER STREQUAL "DIRECTX")
    include(${PROJECT_SOURCE_DIR}/cmake/FindDirectX.cmake)
    set(RenderSystem RenderSystemD3D9)
    ucm_add_dirs(
        Engine
        VoxelMesh
        CadModel
        d3dcommon
    TO sources_rendersystem RECURSIVE
    )

    # Embedded Resource For Direct3D
    file(GLOB SHADER_FILES ${CMAKE_CURRENT_SOURCE_DIR}/shaders/d3d9_spec/*.fx)
    foreach(src_file ${SHADER_FILES})
        get_filename_component(src_filename ${src_file} NAME_WE)
        set(out_file "${CMAKE_CURRENT_BINARY_DIR}/shaders/${src_filename}.fxo")

        add_custom_command(
            OUTPUT ${out_file}
            COMMAND "${DirectX_ROOT_DIR}/Utilities/bin/x86/fxc" /Tfx_2_0 /Gfp /nologo /Fo ${out_file} ${src_file}
            DEPENDS ${src_file}
            COMMENT "fxc compile: ${src_file}"
        )
        embed_resources_abs(Resource ${out_file})
        list(APPEND EmbeddedResource_Files_D3D ${Resource})
    endforeach(src_file ${SHADER_FILES})

    list(APPEND EmbeddedResource_Files_D3D ResourceEmbedded.cpp)

    embed_resources_abs(Resource ${CMAKE_CURRENT_SOURCE_DIR}/res/ParaXmodel.templates)
    list(APPEND EmbeddedResource_Files_D3D ${Resource})	

    embed_resources_abs(Resource ${CMAKE_CURRENT_SOURCE_DIR}/res/cursor.tga)
    list(APPEND EmbeddedResource_Files_D3D ${Resource})

	embed_resources_abs(Resource ${CMAKE_CURRENT_SOURCE_DIR}/res/paraworld.ico)
    list(APPEND EmbeddedResource_Files_D3D ${Resource})

    SOURCE_GROUP("ResEmbedded" FILES ${EmbeddedResource_Files_D3D})

    list(APPEND sources_rendersystem ${EmbeddedResource_Files_D3D})


    # FreeImage
	list(APPEND NPLRUNTIME_LIBS FreeImage)
	if(NPLRUNTIME_RENDERER STREQUAL "DIRECTX")
		ADD_DEFINITIONS(-DUSE_GDI_COMPOSER) # for css skin, etc
	endif()

    #if (WIN32)
    #    option(NPLRUNTIME_FREEIMAGE "include FreeImage" ON)

    #   if (NPLRUNTIME_FREEIMAGE)
    #        ADD_DEFINITIONS(-DUSE_FREEIMAGE) # for reading image files
    #       if(NPLRUNTIME_RENDERER STREQUAL "DIRECTX")
    #            ADD_DEFINITIONS(-DUSE_GDI_COMPOSER) # for css skin, etc
    #        endif()
    #        if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
    #            SET(FREEIMAGE_DASM_FLAGS "x64")
    #        else()
    #            SET(FREEIMAGE_DASM_FLAGS "x32")
    #        endif()
    #        list(APPEND NPLRUNTIME_INCLUDES ${PROJECT_SOURCE_DIR}/externals/FreeImage_3170/Dist/${FREEIMAGE_DASM_FLAGS})
    #        list(APPEND NPLRUNTIME_LIBS ${PROJECT_SOURCE_DIR}/externals/FreeImage_3170/Dist/${FREEIMAGE_DASM_FLAGS}/FreeImage.lib)
    #        configure_file("${PROJECT_SOURCE_DIR}/externals/FreeImage_3170/Dist/${FREEIMAGE_DASM_FLAGS}/FreeImage.dll" "${OUTPUT_BIN_DIR}/FreeImage.dll" COPYONLY)
    #    endif()
    #endif()

elseif(NPLRUNTIME_RENDERER STREQUAL "NULL")
    set(RenderSystem RenderSystemNull)
    list(APPEND sources_rendersystem ResourceEmbedded.cpp)
endif()

list(APPEND NPLRUNTIME_INCLUDES ${PROJECT_SOURCE_DIR}/externals/Pinocchio/include)

# remove some compile time warnings
ADD_DEFINITIONS(-DBOOST_BIND_GLOBAL_PLACEHOLDERS)

###################################################################
## Target

add_library(ParaEngine  ${sources_commmon} ${sources_rendersystem})

target_include_directories(ParaEngine PUBLIC 
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/Core
    ${CMAKE_CURRENT_SOURCE_DIR}/renderer
    ${CMAKE_CURRENT_SOURCE_DIR}/3dengine
    ${CMAKE_CURRENT_SOURCE_DIR}/Engine
    ${CMAKE_CURRENT_SOURCE_DIR}/IO
    ${CMAKE_CURRENT_SOURCE_DIR}/InfoCenter
    ${CMAKE_CURRENT_SOURCE_DIR}/util
    ${CMAKE_CURRENT_SOURCE_DIR}/math
    ${CMAKE_CURRENT_SOURCE_DIR}/ParaScriptBindings
    ${CMAKE_CURRENT_SOURCE_DIR}/NPL
    ${CMAKE_CURRENT_SOURCE_DIR}/Framework/Interface
	${CMAKE_CURRENT_SOURCE_DIR}/AutoRigger
    ${Boost_INCLUDE_DIRS}
    ${NPLRUNTIME_INCLUDES}
)



if(ANDROID)
    set(PLATROM_SPEC_DEFINITIONS 
        PARAENGINE_MOBILE
        PLATFORM_ANDROID
        AUTOUPDATE_PARAENGINE
    )
elseif(WIN32)
    set(PLATROM_SPEC_DEFINITIONS 
        PLATFORM_WINDOWS
        _WIN32_WINNT=0x0600
        _WINSOCK_DEPRECATED_NO_WARNINGS
    )
elseif(IOS)
    set(PLATROM_SPEC_DEFINITIONS
        PARAENGINE_MOBILE
        PLATFORM_IPHONE
    ) 

elseif(MAC_SERVER)

	set(PLATROM_SPEC_DEFINITIONS 
        PLATFORM_MAC MAC_SERVER
    )

    ## Readline support
    include("${PROJECT_SOURCE_DIR}/cmake/FindReadline.cmake")
    # FIND_PACKAGE(Readline)
    IF(READLINE_FOUND)
		LIST(APPEND PLATROM_SPEC_DEFINITIONS
			 NPL_USE_READLINE
		)
		LIST(APPEND PLATFORM_SPE_LIBS ${Readline_LIBRARY})
		MESSAGE("NPL_USE_READLINE read line is found and used")
		INCLUDE_DIRECTORIES(${READLINE_INCLUDE_DIR})
	ELSE()
		MESSAGE("warning: NPL_USE_READLINE NOT FOUND please apt-get install libreadline6 libreadline6-dev")
	ENDIF()
elseif(APPLE AND NOT IOS)
    set(PLATROM_SPEC_DEFINITIONS
    PLATFORM_MAC
    )
elseif(UNIX AND NOT ANDROID)
    ## Readline support
    include("${PROJECT_SOURCE_DIR}/cmake/FindReadline.cmake")
    # FIND_PACKAGE(Readline)
    IF(READLINE_FOUND)
    set(PLATROM_SPEC_DEFINITIONS
         NPL_USE_READLINE
    )
    LIST(APPEND PLATFORM_SPE_LIBS ${Readline_LIBRARY})
    MESSAGE("NPL_USE_READLINE read line is found and used")
    INCLUDE_DIRECTORIES(${READLINE_INCLUDE_DIR})
  ELSE()
    MESSAGE("warning: NPL_USE_READLINE NOT FOUND please apt-get install libreadline6 libreadline6-dev")
  ENDIF()
endif()


get_property(ICONV_LIB_NAME GLOBAL PROPERTY "ICONV_LIB_NAME" ) 
get_property(LUA_LIB_LIST GLOBAL PROPERTY "LUA_LIB_LIST" ) 

##############################
# assimp
if (NPLRUNTIME_SUPPORT_FBX)
	list(APPEND NPLRUNTIME_LIBS assimp)
endif()

if (PHYSICS_STATICLIB)
	list(APPEND NPLRUNTIME_LIBS PhysicsBT)
endif()

if (AUDIO_STATICLIB)
	list(APPEND NPLRUNTIME_LIBS cAudioEngine)
endif()

if (NPLRUNTIME_OCE)
	list(APPEND NPLRUNTIME_LIBS nploce)
	add_dependencies(ParaEngine nploce)
endif()

target_link_libraries(ParaEngine PUBLIC 
    ${RenderSystem}
    ${Boost_LIBRARIES}
    ${NPLRUNTIME_LIBS}
    ${PLATFORM_SPE_LIBS}
	${LUA_LIB_LIST}
    zlib
	libcurl
    jsoncpp
    tinyxpath
    luabind
    ParaSqlite 
	libjpeg
    libpng
	Pinocchio
	${ICONV_LIB_NAME}
)

set(PARAENGINE_DEF_LIST)
list(APPEND PARAENGINE_DEF_LIST USE_FREEIMAGE USE_ICONV TIXML_USE_STL BOOST_SIGNALS_NO_DEPRECATION_WARNING _CRT_SECURE_NO_WARNINGS NPLRUNTIME ${PLATROM_SPEC_DEFINITIONS})

if(WIN32 AND PARAENGINE_CLIENT_DLL)
	list(APPEND PARAENGINE_DEF_LIST NPLRUNTIME_DLL)
endif()

if (SQLITE_STATICLIB)
	list(APPEND PARAENGINE_DEF_LIST SQLITE_STATICLIB)
endif()

if (PHYSICS_STATICLIB)
	list(APPEND PARAENGINE_DEF_LIST PHYSICS_STATICLIB)
endif()

if (AUDIO_STATICLIB)
	list(APPEND PARAENGINE_DEF_LIST STATIC_PLUGIN_CAUDIOENGINE)
endif()

if(NPLRUNTIME_STATIC_LIB)
	list(APPEND PARAENGINE_DEF_LIST CURL_STATICLIB)
	list(APPEND PARAENGINE_DEF_LIST PE_CORE_EXPORTING)
else()
	#ADD_DEFINITIONS(-DPE_CORE_EXPORTING)

	list(APPEND PARAENGINE_DEF_LIST PE_CORE_EXPORTING)
endif()

if (NPLRUNTIME_SERVER)
	list(APPEND PARAENGINE_DEF_LIST NPLRUNTIME_SERVER)
endif()


target_compile_definitions(ParaEngine PUBLIC 
    ${PARAENGINE_DEF_LIST}
)


IF(MSVC)
    ucm_set_runtime(STATIC)
    # enable multiprocessor build option /MP, this will greatly increase compile speed
    SET(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MP")
    SET(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} /MP")
    SET(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /MP")
    SET(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} /MP")
ENDIF()  
