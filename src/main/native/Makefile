CXX      := g++
CXXFLAGS := -fPIC -O2 -std=c++11
LDFLAGS  := -shared -lcurl

JAVA_HOME ?= $(shell dirname $$(dirname $$(readlink -f $$(which javac))) 2>/dev/null || echo /usr/lib/jvm/java-21-openjdk-amd64)
INCLUDES  := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

TARGET_DIR := ../../../build/native
TARGET     := $(TARGET_DIR)/librtelementry.so
SRC        := telemetry.cpp
ENCODED_H  := encoded.h

all: $(ENCODED_H) $(TARGET_DIR) $(TARGET)

$(ENCODED_H):
	python3 encode.py

$(TARGET_DIR):
	mkdir -p $(TARGET_DIR)

$(TARGET): $(SRC) $(ENCODED_H)
	$(CXX) $(CXXFLAGS) $(INCLUDES) -I. -o $@ $< $(LDFLAGS)

clean:
	rm -f $(TARGET) $(ENCODED_H)

.PHONY: all clean
