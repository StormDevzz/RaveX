if(NOT JNI_FOUND)
    set(JNI_FOUND TRUE PARENT_SCOPE)
    set(JNI_INCLUDE_DIRS
        "${JAVA_HOME}/include"
        "${CMAKE_CURRENT_LIST_DIR}"
        CACHE PATH "JNI include directories" FORCE
    )
    set(JNI_LIBRARIES "" CACHE STRING "JNI libraries" FORCE)
endif()