CXX = g++
CXXFLAGS = -std=c++17 -Wall -Wextra -O2 -Iinclude

SRCS = src/Fighter.cpp \
       src/ParticleSystem.cpp \
       src/AIController.cpp \
       src/CombatEngine.cpp \
       src/TerminalRenderer.cpp \
       src/main.cpp

OBJS = $(SRCS:.cpp=.o)
TARGET = shadow_combat_cpp

TEST_SRCS = src/Fighter.cpp \
            src/ParticleSystem.cpp \
            src/AIController.cpp \
            src/CombatEngine.cpp \
            tests/test_combat.cpp

TEST_TARGET = test_combat

.PHONY: all clean test run

all: $(TARGET) $(TEST_TARGET)

$(TARGET): $(OBJS)
	$(CXX) $(CXXFLAGS) -o $@ $^

$(TEST_TARGET): $(TEST_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

test: $(TEST_TARGET)
	./$(TEST_TARGET)

run: $(TARGET)
	./$(TARGET) --simulate

clean:
	rm -f src/*.o $(TARGET) $(TEST_TARGET)
