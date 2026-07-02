#pragma once

#include "include/ecfarmer_types.hpp"
#include "include/ecfarmer_calc.hpp"
#include "include/ecfarmer_tracker.hpp"
#include "include/ecfarmer_util.hpp"

ECFarmerResult calculateBreak(const char* toolId, int efficiency, int haste, int durability, int maxDura);
