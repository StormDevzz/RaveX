#include "model.hpp"
#include "include/model_math.hpp"

namespace model {

Animation interpolateTrack(const AnimationTrack& track, float time) {
    Animation result;
    (void)track; (void)time;
    return result;
}

void applyAnimationToBones(ModelData& model, const Animation& anim, float time) {
    (void)model; (void)anim; (void)time;
}

std::vector<Animation> splitAnimations(const ModelData& model) {
    return model.animations;
}

} 
