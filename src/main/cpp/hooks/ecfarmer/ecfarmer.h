#pragma once

#include "include/ecfarmer_types.h"
#include "include/ecfarmer_calc.h"
#include "include/ecfarmer_tracker.h"
#include "include/ecfarmer_util.h"

ECFarmerResult calculateBreak(const char* toolId, int efficiency, int haste, int durability, int maxDura);
