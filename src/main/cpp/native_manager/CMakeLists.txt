if(WIN32)
    enable_language(RC)
    add_executable(ravex_native_manager WIN32
        main/native_manager.cpp
        res/native_manager.rc
    )

    target_link_libraries(ravex_native_manager PRIVATE user32)

    set_target_properties(ravex_native_manager PROPERTIES
        OUTPUT_NAME "ravex_native_manager"
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
    )

    if(MINGW)
        target_link_options(ravex_native_manager PRIVATE -static-libgcc -static-libstdc++ -static)
    endif()

    set_target_properties(ravex_native_manager PROPERTIES SUFFIX ".exe")
endif()
