#pragma once
#include <string>

namespace ravex {

const char* lang(const char* key);
const char* langFor(const char* key, const char* code);
const char* currentLanguage();
void setCurrentLanguage(const char* code);
int langCount();
const char* langCodeByIndex(int index);
const char* langDisplayName(const char* code);
const char* langFlag(const char* code);

}
