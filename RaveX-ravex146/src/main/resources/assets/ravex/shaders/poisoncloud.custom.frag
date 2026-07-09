//                █████████████                      
//            ██████████████████████   
//         ███████████████████████████              
//        ████████████████████████████████           
//       ███████████████████████████████████         
//      ██████████       ██    ██████████████        
//     ████████         ███        ████████████      
//     ███████         █████          ██████████     
//     ██████          ████████         █████████    
//     ██████           █████████         ███████    
//      █████            █████████         ███████   
//       ██████           ████████          ██████   
//                        ███████           ██████   
//                   ██████████              █████   
//                                           █████   
//             ███████                       ████    
//          █████████████                    ███     
//        ██████████████████                ███      
//       █████████████████████             ███       
//      ██         ██████████████        ███         
//                    ███████████████████            
//                        █████████   
//               
// Poison Cloud Made By : Dxchx

// --------------------
// Float
// --------------------
float load     = 0.002;
float upload   = 0.0;
float age      = 0.2;

// --------------------
// Noise
// --------------------

float random(vec2 p) {
    return fract(100.0 * sin(p.x + fract(100.0 * sin(p.y))));
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x)
         + (c - a) * u.y * (1.0 - u.x)
         + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.5;

    for (int i = 0; i < 6; i++) {
        value += amp * noise(p);
        p *= 1.9;
        amp *= 0.6;
    }

    return value;
}

float pattern(vec2 p) {

    vec2 aPos = vec2(
        sin(u_Time * 0.05),
        sin(u_Time * 0.10)
    ) * 6.0;

    float a = fbm(p * 3.0 + aPos);

    vec2 bPos = vec2(
        sin(u_Time * 0.10),
        sin(u_Time * 0.10)
    );

    float b = fbm((p + a) * 0.5 + bPos);

    vec2 cPos = vec2(-0.6, -0.5)
              + vec2(
                    sin(-u_Time * 0.01),
                    sin(u_Time * 0.10)
                ) * 2.0;

    float c = fbm((p + b) * (2.0 * (1.0 + upload)) + cPos);

    return c;
}

// --------------------
// Purp n Green
// --------------------

vec3 palette(float t)
{
    // Base
    vec3 base = vec3(0.05, 0.0, 0.08);

    float wave = cos(6.28318 * t);

    // Purp
    float purple = 0.6 + 0.4 * wave;

    // Green
    float green = 0.15 + 0.35 * cos(6.28318 * t * 1.3 + 1.0);

    vec3 col;
    col.r = purple * 0.8;
    col.b = purple * 0.95;  // balanced violet (not too blue)
    col.g = green;

    // Glow
    col.g += smoothstep(0.6, 1.0, t) * 0.3;

    return base + col;
}

vec3 greyscale(vec3 col, float str) {
    float g = dot(col, vec3(0.299, 0.587, 0.114));
    return mix(col, vec3(g), str);
}

vec4 mainImage(bool isFill) {

    vec2 p = (gl_FragCoord.xy - 0.5 * u_Size.xy) / u_Size.y;

    float v = pow(pattern(p), 2.0);

    vec3 col = greyscale(
        palette(v),
        clamp(age / 10.0, 0.0, 1.0)
    );

    return vec4(col, 1.0);
}