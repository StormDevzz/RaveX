#include <imgui.h>
#include <imgui_impl_sdl2.h>
#include <imgui_impl_opengl3.h>
#include <SDL2/SDL.h>
#include <GL/gl3w.h>
#include <iostream>
#include <string>
#include <vector>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <thread>
#include <chrono>

namespace fs = std::filesystem;

// Discord-style colors
ImVec4 discord_bg = ImVec4(0.11f, 0.11f, 0.12f, 1.0f);
ImVec4 discord_secondary = ImVec4(0.16f, 0.16f, 0.18f, 1.0f);
ImVec4 discord_accent = ImVec4(0.49f, 0.52f, 0.58f, 1.0f);
ImVec4 discord_hover = ImVec4(0.56f, 0.59f, 0.65f, 1.0f);
ImVec4 discord_text = ImVec4(0.96f, 0.96f, 0.96f, 1.0f);
ImVec4 discord_text_secondary = ImVec4(0.72f, 0.74f, 0.77f, 1.0f);
ImVec4 discord_green = ImVec4(0.33f, 0.73f, 0.49f, 1.0f);
ImVec4 discord_red = ImVec4(0.78f, 0.33f, 0.33f, 1.0f);

class RaveXLauncher {
private:
    SDL_Window* window = nullptr;
    SDL_GLContext gl_context = nullptr;
    bool running = true;
    
    std::string jarPath = "";
    std::vector<std::string> logs;
    float progress = 0.0f;
    std::string status = "Ready to launch";
    bool launching = false;
    bool launchSuccess = false;
    
    // Animation variables
    float logoAlpha = 0.0f;
    float contentAlpha = 0.0f;
    
public:
    bool init() {
        if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_TIMER | SDL_INIT_GAMECONTROLLER) != 0) {
            std::cerr << "SDL_Init Error: " << SDL_GetError() << std::endl;
            return false;
        }
        
        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);
        SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 0);
        
        SDL_WindowFlags window_flags = (SDL_WindowFlags)(
            SDL_WINDOW_OPENGL | 
            SDL_WINDOW_RESIZABLE | 
            SDL_WINDOW_ALLOW_HIGHDPI
        );
        
        window = SDL_CreateWindow(
            "RaveX Launcher",
            SDL_WINDOWPOS_CENTERED,
            SDL_WINDOWPOS_CENTERED,
            800,
            600,
            window_flags
        );
        
        if (!window) {
            std::cerr << "SDL_CreateWindow Error: " << SDL_GetError() << std::endl;
            return false;
        }
        
        gl_context = SDL_GL_CreateContext(window);
        SDL_GL_MakeCurrent(window, gl_context);
        SDL_GL_SetSwapInterval(1); // Enable vsync
        
        if (gl3wInit() != 0) {
            std::cerr << "gl3wInit failed" << std::endl;
            return false;
        }
        
        IMGUI_CHECKVERSION();
        ImGui::CreateContext();
        ImGuiIO& io = ImGui::GetIO();
        io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
        io.IniFilename = nullptr; // Disable .ini file
        
        ImGui::StyleColorsDark();
        applyDiscordStyle();
        
        ImGui_ImplSDL2_InitForOpenGL(window, gl_context);
        ImGui_ImplOpenGL3_Init("#version 130");
        
        // Find default jar path
        findDefaultJarPath();
        
        return true;
    }
    
    void applyDiscordStyle() {
        ImGuiStyle& style = ImGui::GetStyle();
        
        style.WindowPadding = ImVec2(20, 20);
        style.WindowRounding = 8.0f;
        style.FramePadding = ImVec2(12, 8);
        style.FrameRounding = 4.0f;
        style.ItemSpacing = ImVec2(12, 8);
        style.ItemInnerSpacing = ImVec2(8, 4);
        style.IndentSpacing = 25.0f;
        style.ScrollbarSize = 8.0f;
        style.ScrollbarRounding = 4.0f;
        style.GrabMinSize = 8.0f;
        style.GrabRounding = 4.0f;
        
        style.Colors[ImGuiCol_WindowBg] = discord_bg;
        style.Colors[ImGuiCol_ChildBg] = discord_secondary;
        style.Colors[ImGuiCol_PopupBg] = discord_secondary;
        style.Colors[ImGuiCol_FrameBg] = discord_secondary;
        style.Colors[ImGuiCol_FrameBgHovered] = discord_accent;
        style.Colors[ImGuiCol_FrameBgActive] = discord_accent;
        style.Colors[ImGuiCol_TitleBg] = discord_secondary;
        style.Colors[ImGuiCol_TitleBgActive] = discord_secondary;
        style.Colors[ImGuiCol_TitleBgCollapsed] = discord_secondary;
        style.Colors[ImGuiCol_MenuBarBg] = discord_secondary;
        style.Colors[ImGuiCol_ScrollbarBg] = discord_secondary;
        style.Colors[ImGuiCol_ScrollbarGrab] = discord_accent;
        style.Colors[ImGuiCol_ScrollbarGrabHovered] = discord_hover;
        style.Colors[ImGuiCol_ScrollbarGrabActive] = discord_hover;
        style.Colors[ImGuiCol_CheckMark] = discord_green;
        style.Colors[ImGuiCol_SliderGrab] = discord_accent;
        style.Colors[ImGuiCol_SliderGrabActive] = discord_hover;
        style.Colors[ImGuiCol_Button] = discord_green;
        style.Colors[ImGuiCol_ButtonHovered] = ImVec4(0.4f, 0.8f, 0.55f, 1.0f);
        style.Colors[ImGuiCol_ButtonActive] = ImVec4(0.35f, 0.75f, 0.5f, 1.0f);
        style.Colors[ImGuiCol_Header] = discord_secondary;
        style.Colors[ImGuiCol_HeaderHovered] = discord_accent;
        style.Colors[ImGuiCol_HeaderActive] = discord_accent;
        style.Colors[ImGuiCol_Separator] = discord_accent;
        style.Colors[ImGuiCol_SeparatorHovered] = discord_hover;
        style.Colors[ImGuiCol_SeparatorActive] = discord_hover;
        style.Colors[ImGuiCol_ResizeGrip] = discord_accent;
        style.Colors[ImGuiCol_ResizeGripHovered] = discord_hover;
        style.Colors[ImGuiCol_ResizeGripActive] = discord_hover;
        style.Colors[ImGuiCol_Tab] = discord_secondary;
        style.Colors[ImGuiCol_TabHovered] = discord_accent;
        style.Colors[ImGuiCol_TabActive] = discord_accent;
        style.Colors[ImGuiCol_TabUnfocused] = discord_secondary;
        style.Colors[ImGuiCol_TabUnfocusedActive] = discord_secondary;
        style.Colors[ImGuiCol_DockingPreview] = discord_green;
        style.Colors[ImGuiCol_DockingEmptyBg] = discord_bg;
        style.Colors[ImGuiCol_PlotLines] = discord_accent;
        style.Colors[ImGuiCol_PlotLinesHovered] = discord_hover;
        style.Colors[ImGuiCol_PlotHistogram] = discord_accent;
        style.Colors[ImGuiCol_PlotHistogramHovered] = discord_hover;
        style.Colors[ImGuiCol_Text] = discord_text;
        style.Colors[ImGuiCol_TextDisabled] = discord_text_secondary;
        style.Colors[ImGuiCol_TextSelectedBg] = discord_accent;
        style.Colors[ImGuiCol_DragDropTarget] = discord_green;
        style.Colors[ImGuiCol_NavHighlight] = discord_accent;
        style.Colors[ImGuiCol_NavWindowingHighlight] = discord_accent;
        style.Colors[ImGuiCol_NavWindowingDimBg] = ImVec4(0.0f, 0.0f, 0.0f, 0.5f);
        style.Colors[ImGuiCol_ModalWindowDimBg] = ImVec4(0.0f, 0.0f, 0.0f, 0.5f);
    }
    
    void findDefaultJarPath() {
        std::string home = getenv("HOME") ? getenv("HOME") : "";
        std::string userprofile = getenv("USERPROFILE") ? getenv("USERPROFILE") : "";
        
        std::vector<std::string> searchPaths;
        
        if (!userprofile.empty()) {
            searchPaths.push_back(userprofile + "\\Downloads\\RaveX-main3\\RaveX-main\\build\\libs\\");
            searchPaths.push_back(userprofile + "\\AppData\\Roaming\\.minecraft\\mods\\");
        }
        
        if (!home.empty()) {
            searchPaths.push_back(home + "/Downloads/RaveX-main3/RaveX-main/build/libs/");
            searchPaths.push_back(home + "/.minecraft/mods/");
        }
        
        for (const auto& path : searchPaths) {
            if (fs::exists(path) && fs::is_directory(path)) {
                for (const auto& entry : fs::directory_iterator(path)) {
                    if (entry.path().extension() == ".jar") {
                        std::string filename = entry.path().filename().string();
                        if (filename.find("sources") == std::string::npos) {
                            jarPath = entry.path().string();
                            return;
                        }
                    }
                }
            }
        }
    }
    
    void browseJarFile() {
#ifdef _WIN32
        OPENFILENAME ofn;
        char szFile[260] = {0};
        
        ZeroMemory(&ofn, sizeof(ofn));
        ofn.lStructSize = sizeof(ofn);
        ofn.hwndOwner = nullptr;
        ofn.lpstrFile = szFile;
        ofn.nMaxFile = sizeof(szFile);
        ofn.lpstrFilter = "JAR Files\0*.jar\0All Files\0*.*\0";
        ofn.nFilterIndex = 1;
        ofn.lpstrFileTitle = nullptr;
        ofn.nMaxFileTitle = 0;
        ofn.lpstrInitialDir = nullptr;
        ofn.Flags = OFN_PATHMUSTEXIST | OFN_FILEMUSTEXIST;
        
        if (GetOpenFileName(&ofn)) {
            jarPath = szFile;
        }
#else
        // Linux file dialog using zenity
        char buffer[512];
        FILE* pipe = popen("zenity --file-selection --file-filter='JAR Files | *.jar' --title='Select RaveX JAR'", "r");
        if (pipe) {
            if (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
                jarPath = buffer;
                jarPath.erase(jarPath.find_last_not_of("\n") + 1);
            }
            pclose(pipe);
        }
#endif
    }
    
    void launchRaveX() {
        if (jarPath.empty()) {
            status = "Please select a JAR file";
            return;
        }
        
        if (!fs::exists(jarPath)) {
            status = "JAR file not found";
            return;
        }
        
        launching = true;
        progress = 0.0f;
        status = "Initializing...";
        logs.clear();
        
        std::thread([this]() {
            try {
                addLog("Starting RaveX launcher...");
                addLog("JAR: " + jarPath);
                
                updateProgress(10.0f, "Checking Java...");
                std::this_thread::sleep_for(std::chrono::milliseconds(500));
                
                updateProgress(20.0f, "Preparing launch command...");
                std::this_thread::sleep_for(std::chrono::milliseconds(500));
                
                std::string javaCmd = "java";
                std::string fullCmd = javaCmd + " -jar \"" + jarPath + "\" -Xmx2G -Xms512M";
                
                addLog("Command: " + fullCmd);
                
                updateProgress(40.0f, "Launching RaveX...");
                
#ifdef _WIN32
                FILE* pipe = _popen(fullCmd.c_str(), "r");
#else
                FILE* pipe = popen(fullCmd.c_str(), "r");
#endif
                
                if (pipe) {
                    char buffer[128];
                    while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
                        addLog(buffer);
                    }
                    
#ifdef _WIN32
                    _pclose(pipe);
#else
                    pclose(pipe);
#endif
                    
                    updateProgress(100.0f, "RaveX closed");
                    launchSuccess = true;
                } else {
                    updateProgress(100.0f, "Launch failed");
                    addLog("Failed to launch process");
                }
                
            } catch (const std::exception& e) {
                status = "Error: " + std::string(e.what());
                addLog(std::string("Error: ") + e.what());
            }
            
            launching = false;
        }).detach();
    }
    
    void updateProgress(float value, const std::string& msg) {
        progress = value;
        status = msg;
    }
    
    void addLog(const std::string& msg) {
        logs.push_back(msg);
        if (logs.size() > 100) {
            logs.erase(logs.begin());
        }
    }
    
    void render() {
        ImGui_ImplOpenGL3_NewFrame();
        ImGui_ImplSDL2_NewFrame();
        ImGui::NewFrame();
        
        // Main window
        ImGui::SetNextWindowPos(ImVec2(0, 0));
        ImGui::SetNextWindowSize(ImGui::GetIO().DisplaySize);
        ImGui::PushStyleVar(ImGuiStyleVar_WindowRounding, 0.0f);
        ImGui::PushStyleVar(ImGuiStyleVar_WindowBorderSize, 0.0f);
        ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(0, 0));
        
        ImGui::Begin("RaveX Launcher", nullptr, 
            ImGuiWindowFlags_NoTitleBar | 
            ImGuiWindowFlags_NoResize | 
            ImGuiWindowFlags_NoMove | 
            ImGuiWindowFlags_NoCollapse | 
            ImGuiWindowFlags_NoDocking);
        
        ImGui::PopStyleVar(3);
        
        // Background
        ImGui::GetWindowDrawList()->AddRectFilled(
            ImVec2(0, 0), 
            ImGui::GetIO().DisplaySize, 
            ImGui::ColorConvertFloat4ToU32(discord_bg)
        );
        
        // Center content
        ImGui::SetCursorPosX(ImGui::GetIO().DisplaySize.x * 0.5f - 300);
        ImGui::SetCursorPosY(ImGui::GetIO().DisplaySize.y * 0.5f - 200);
        
        ImGui::PushStyleColor(ImGuiCol_ChildBg, discord_secondary);
        ImGui::PushStyleVar(ImGuiStyleVar_ChildRounding, 12.0f);
        ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(30, 30));
        
        ImGui::BeginChild("MainContent", ImVec2(600, 400), true);
        ImGui::PopStyleVar(2);
        ImGui::PopStyleColor();
        
        // Logo/Title
        ImGui::PushStyleColor(ImGuiCol_Text, discord_text);
        ImGui::SetCursorPosX(600 * 0.5f - ImGui::CalcTextSize("RaveX Launcher").x * 0.5f);
        ImGui::SetFont(ImGui::GetIO().Fonts->Fonts[0]); // Use default font for now
        ImGui::Text("RaveX Launcher");
        ImGui::PopStyleColor();
        
        ImGui::Spacing();
        ImGui::Spacing();
        
        // JAR file selection
        ImGui::PushStyleColor(ImGuiCol_Text, discord_text_secondary);
        ImGui::Text("RaveX JAR File:");
        ImGui::PopStyleColor();
        
        ImGui::Spacing();
        
        ImGui::PushStyleColor(ImGuiCol_FrameBg, discord_bg);
        ImGui::PushStyleColor(ImGuiCol_Text, discord_text);
        ImGui::InputText("##jarpath", &jarPath, ImGuiInputTextFlags_ReadOnly);
        ImGui::PopStyleColor(2);
        
        ImGui::SameLine();
        
        ImGui::PushStyleColor(ImGuiCol_Button, discord_accent);
        ImGui::PushStyleColor(ImGuiCol_ButtonHovered, discord_hover);
        ImGui::PushStyleColor(ImGuiCol_ButtonActive, discord_hover);
        if (ImGui::Button("Browse")) {
            browseJarFile();
        }
        ImGui::PopStyleColor(3);
        
        ImGui::Spacing();
        ImGui::Spacing();
        
        // Launch button
        ImGui::SetCursorPosX(600 * 0.5f - 100);
        ImGui::PushStyleColor(ImGuiCol_Button, discord_green);
        ImGui::PushStyleColor(ImGuiCol_ButtonHovered, ImVec4(0.4f, 0.8f, 0.55f, 1.0f));
        ImGui::PushStyleColor(ImGuiCol_ButtonActive, ImVec4(0.35f, 0.75f, 0.5f, 1.0f));
        ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, 6.0f);
        
        if (launching) {
            ImGui::PushItemFlag(ImGuiItemFlags_Disabled, true);
            ImGui::Button("Launching...", ImVec2(200, 40));
            ImGui::PopItemFlag();
        } else {
            if (ImGui::Button("Launch RaveX", ImVec2(200, 40))) {
                launchRaveX();
            }
        }
        
        ImGui::PopStyleVar();
        ImGui::PopStyleColor(3);
        
        ImGui::Spacing();
        ImGui::Spacing();
        
        // Progress bar
        if (launching || progress > 0) {
            ImGui::PushStyleColor(ImGuiCol_FrameBg, discord_bg);
            ImGui::PushStyleColor(ImGuiCol_PlotHistogram, discord_green);
            ImGui::ProgressBar(progress / 100.0f, ImVec2(540, 8), "");
            ImGui::PopStyleColor(2);
            
            ImGui::Spacing();
            
            ImGui::PushStyleColor(ImGuiCol_Text, discord_text_secondary);
            ImGui::Text(status.c_str());
            ImGui::PopStyleColor();
        }
        
        ImGui::EndChild();
        
        ImGui::End();
        
        // Log window (small at bottom)
        if (!logs.empty()) {
            ImGui::SetNextWindowPos(ImVec2(0, ImGui::GetIO().DisplaySize.y - 150));
            ImGui::SetNextWindowSize(ImVec2(ImGui::GetIO().DisplaySize.x, 150));
            ImGui::PushStyleVar(ImGuiStyleVar_WindowRounding, 0.0f);
            ImGui::PushStyleVar(ImGuiStyleVar_WindowBorderSize, 0.0f);
            ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(10, 10));
            
            ImGui::Begin("Logs", nullptr,
                ImGuiWindowFlags_NoTitleBar |
                ImGuiWindowFlags_NoResize |
                ImGuiWindowFlags_NoMove |
                ImGuiWindowFlags_NoCollapse |
                ImGuiWindowFlags_NoDocking);
            
            ImGui::PopStyleVar(3);
            
            ImGui::PushStyleColor(ImGuiCol_ChildBg, discord_secondary);
            ImGui::BeginChild("LogContent", ImGui::GetContentRegionAvail(), true);
            ImGui::PopStyleColor();
            
            for (const auto& log : logs) {
                ImGui::PushStyleColor(ImGuiCol_Text, discord_text_secondary);
                ImGui::TextWrapped(log.c_str());
                ImGui::PopStyleColor();
            }
            
            ImGui::SetScrollHereY(1.0f);
            ImGui::EndChild();
            ImGui::End();
        }
        
        ImGui::Render();
        ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
        SDL_GL_SwapWindow(window);
    }
    
    void cleanup() {
        ImGui_ImplOpenGL3_Shutdown();
        ImGui_ImplSDL2_Shutdown();
        ImGui::DestroyContext();
        
        SDL_GL_DeleteContext(gl_context);
        SDL_DestroyWindow(window);
        SDL_Quit();
    }
    
    void run() {
        while (running) {
            SDL_Event event;
            while (SDL_PollEvent(&event)) {
                ImGui_ImplSDL2_ProcessEvent(&event);
                if (event.type == SDL_QUIT) {
                    running = false;
                }
                if (event.type == SDL_KEYDOWN) {
                    if (event.key.keysym.sym == SDLK_ESCAPE) {
                        running = false;
                    }
                }
            }
            
            render();
        }
        
        cleanup();
    }
};

int main(int argc, char** argv) {
    RaveXLauncher launcher;
    
    if (!launcher.init()) {
        return -1;
    }
    
    launcher.run();
    
    return 0;
}
