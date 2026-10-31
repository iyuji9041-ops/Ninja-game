cmake_minimum_required(VERSION 3.16)
project(ShadowCombat CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(include)

set(ENGINE_SOURCES
    src/Fighter.cpp
    src/ParticleSystem.cpp
    src/AIController.cpp
    src/CombatEngine.cpp
    src/TerminalRenderer.cpp
)

add_executable(shadow_combat_cpp src/main.cpp ${ENGINE_SOURCES})

add_executable(test_combat tests/test_combat.cpp ${ENGINE_SOURCES})

enable_testing()
add_test(NAME CombatEngineTest COMMAND test_combat)
