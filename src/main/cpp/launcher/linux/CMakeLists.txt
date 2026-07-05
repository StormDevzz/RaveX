cmake_minimum_required(VERSION 3.10)
project(kickx_launcher LANGUAGES C CXX)

find_package(PkgConfig REQUIRED)
pkg_check_modules(GTK3 REQUIRED gtk+-3.0)

add_executable(kickx_launcher
    main.cpp
    simple/simple_launcher.cpp
    
    simple/file/file_manager.cpp
    simple/file/path_provider.cpp
    
    simple/state/launcher_state.cpp
    simple/state/instance_config.cpp
    
    simple/button/simple_buttons.cpp
    simple/button/launch_handler.cpp
    
    simple/acc/account_manager.cpp
    simple/acc/offline_auth.cpp
    simple/acc/microsoft_session.cpp
    simple/acc/account_serializer.cpp
    
    simple/network/mojang_api.cpp
    simple/network/microsoft_auth.cpp
    simple/network/http_client.cpp
    simple/network/mojang_downloader.cpp
    simple/network/mojang_parser.cpp
    
    simple/instances/main/main_window.cpp
    simple/instances/main/window_layout.cpp
    simple/instances/main/window_signals.cpp
    simple/instances/main/window_init.cpp
    simple/instances/manager/instance_manager.cpp
    simple/instances/editor/instance_editor.cpp
    simple/instances/console/console_window.cpp
    simple/window/background/custom/ui_styles.cpp
    simple/window/background/custom/bg_loader.cpp
    simple/window/resize/resize_handler.cpp
    simple/window/resize/resize_manager.cpp
    simple/window/resize/resize_animator.cpp
    simple/window/resize/resize_core.cpp
    
    simple/integr/fabric_api.cpp
    simple/integr/fabric_installer.cpp
    simple/integr/fabric_launcher.cpp
    simple/integr/forge_api.cpp
    simple/integr/forge_installer.cpp
    simple/integr/integr_core.cpp
    simple/integr/integr_utils.cpp
    simple/integr/integr_downloader.cpp
    simple/integr/integr_validator.cpp
    simple/integr/integr_config.cpp
    
    simple/event/event_types.cpp
    simple/event/progress_handler.cpp
    simple/event/event_queue.cpp
    simple/event/event_scheduler.cpp
    simple/event/event_core.cpp
    
    simple/sys/cpu_info.cpp
    simple/sys/mem_info.cpp
    simple/sys/disk_info.cpp
    simple/sys/net_info.cpp
    simple/sys/proc_info.cpp
    simple/sys/os_info.cpp
    simple/sys/env_info.cpp
    simple/sys/limits_info.cpp
    simple/sys/perf_info.cpp
    simple/sys/sys_info.cpp
    
    simple/ver/ver_base_check.cpp
    simple/ver/ver_base_calc.cpp
    simple/ver/ver_compare_check.cpp
    simple/ver/ver_compare_calc.cpp
    simple/ver/ver_parser_check.cpp
    simple/ver/ver_parser_calc.cpp
    simple/ver/ver_validator_check.cpp
    simple/ver/ver_validator_calc.cpp
    simple/ver/update_check_check.cpp
    simple/ver/update_check_calc.cpp
    simple/ver/release_fetch_check.cpp
    simple/ver/release_fetch_calc.cpp
    simple/ver/manifest_verify_check.cpp
    simple/ver/manifest_verify_calc.cpp
    simple/ver/hash_check_check.cpp
    simple/ver/hash_check_calc.cpp
    simple/ver/integrity_check_check.cpp
    simple/ver/integrity_check_calc.cpp
    simple/ver/dep_resolve_check.cpp
    simple/ver/dep_resolve_calc.cpp
    simple/ver/compat_check_check.cpp
    simple/ver/compat_check_calc.cpp
    simple/ver/ver_cache_check.cpp
    simple/ver/ver_cache_calc.cpp
    simple/ver/ver_db_check.cpp
    simple/ver/ver_db_calc.cpp
    simple/ver/ver_api_check.cpp
    simple/ver/ver_api_calc.cpp
    simple/ver/ver_sync_check.cpp
    simple/ver/ver_sync_calc.cpp
    
    simple/analyze/crash_logger.cpp
    simple/analyze/sys_monitor.cpp
    simple/analyze/perf_tracker.cpp
    simple/analyze/log_manager.cpp
    simple/analyze/analyze_core.cpp
    
    checks/system_checks.cpp
    checks/fabric_check.cpp
    plugins/github_utility.cpp
    plugins/sys_optimize.cpp
    icon/main/icon_manager.cpp
)
target_include_directories(kickx_launcher PRIVATE
    .
    simple/include
    simple/acc/include
    simple/button/include
    simple/file/include
    simple/instances/main/include
    simple/instances/console/include
    simple/instances/manager/include
    simple/instances/editor/include
    simple/network/include
    simple/state/include
    simple/event/include
    simple/sys/include
    simple/ver/include
    simple/analyze/include
    simple/integr/include
    simple/window/background/custom/include
    simple/window/resize/include
    icon/include
    checks/include
    ${GTK3_INCLUDE_DIRS}
)
target_link_libraries(kickx_launcher PRIVATE pthread ${GTK3_LIBRARIES})
