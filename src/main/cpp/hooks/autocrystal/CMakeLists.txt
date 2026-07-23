cmake_minimum_required(VERSION 3.14)

add_library(ravex_autocrystal SHARED
    main/autocrystal.cpp
    main/autocrystal_jni.cpp
    damage/damage.cpp
    armor/armor.cpp
    effects/effects.cpp
    tracker/entity_tracker.cpp
    conditions/conditions.cpp

    armorfucker/armorfucker.cpp
    armorfucker/armor_durability.cpp
    armorfucker/armor_penetration.cpp
    armorfucker/armor_evaluation.cpp
    armorfucker/armor_prioritizer.cpp

    conditions/player_conditions.cpp
    conditions/world_conditions.cpp
    conditions/grim_conditions.cpp

    damage/damage_calculator.cpp
    damage/explosion_damage.cpp
    damage/raycast_damage.cpp
    damage/armor_damage.cpp
    damage/shield_damage.cpp
    damage/collateral_damage.cpp
    damage/pop_list.cpp
    damage/damage_prediction.cpp
    damage/damage_modifier.cpp
    damage/damage_logger.cpp

    effects/particle_effects.cpp
    effects/render_effects.cpp
    effects/sound_effects.cpp
    effects/visual_effects.cpp

    tracker/target_tracker.cpp
    tracker/crystal_tracker.cpp
    tracker/prediction_tracker.cpp
    tracker/motion_tracker.cpp
    tracker/knockback_tracker.cpp
    tracker/block_scanner.cpp
    tracker/velocity_tracker.cpp

    timings/timings_manager.cpp
    timings/timing_delay.cpp
    timings/timing_ticks.cpp
    timings/timing_latency.cpp
    timings/timing_scheduler.cpp
    timings/timing_predict.cpp
    timings/timing_throttle.cpp
    timings/ncp_timings.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_autocrystal PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_autocrystal PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_autocrystal")
endif()

target_include_directories(ravex_autocrystal PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_autocrystal PRIVATE ravex_common)

set_property(TARGET ravex_autocrystal PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_autocrystal PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_autocrystal PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_autocrystal PRIVATE -O2)
    endif()
endif()

get_filename_component(JAVA_LIB_DIR1 "${JAVA_AWT_LIBRARY}" DIRECTORY)
get_filename_component(JAVA_LIB_DIR2 "${JAVA_JVM_LIBRARY}" DIRECTORY)
set_target_properties(ravex_autocrystal PROPERTIES
    OUTPUT_NAME "ravex_autocrystal"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
    INSTALL_RPATH "${JAVA_LIB_DIR1}:${JAVA_LIB_DIR2}"
    BUILD_RPATH "${JAVA_LIB_DIR1}:${JAVA_LIB_DIR2}"
)

if(WIN32)
    set_target_properties(ravex_autocrystal PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_autocrystal PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_autocrystal PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_autocrystal PROPERTIES SUFFIX ".so")
endif()
