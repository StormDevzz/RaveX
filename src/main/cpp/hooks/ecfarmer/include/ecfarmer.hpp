#pragma once

#include "ecfarmer_types.hpp"
#include "ecfarmer_calc.hpp"
#include "ecfarmer_tracker.hpp"
#include "ecfarmer_util.hpp"

ECFarmerResult calculateBreak(const char* toolId, int efficiency, int haste, int durability, int maxDura);
