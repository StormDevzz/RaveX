vec2 hashG(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float gnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float va = dot(hashG(i), f);
    float vb = dot(hashG(i + vec2(1, 0)), f - vec2(1, 0));
    float vc = dot(hashG(i + vec2(0, 1)), f - vec2(0, 1));
    float vd = dot(hashG(i + vec2(1, 1)), f - vec2(1, 1));
    return mix(mix(va, vb, u.x), mix(vc, vd, u.x), u.y);
}

vec3 hashG3(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
             dot(p, vec3(269.5, 183.3, 246.1)),
             dot(p, vec3(113.5, 271.9, 124.6)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float gnoise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(mix(mix(dot(hashG3(i + vec3(0,0,0)), f - vec3(0,0,0)),
                       dot(hashG3(i + vec3(1,0,0)), f - vec3(1,0,0)), u.x),
                   mix(dot(hashG3(i + vec3(0,1,0)), f - vec3(0,1,0)),
                       dot(hashG3(i + vec3(1,1,0)), f - vec3(1,1,0)), u.x), u.y),
               mix(mix(dot(hashG3(i + vec3(0,0,1)), f - vec3(0,0,1)),
                       dot(hashG3(i + vec3(1,0,1)), f - vec3(1,0,1)), u.x),
                   mix(dot(hashG3(i + vec3(0,1,1)), f - vec3(0,1,1)),
                       dot(hashG3(i + vec3(1,1,1)), f - vec3(1,1,1)), u.x), u.y), u.z);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    mat2 rot = mat2(0.866, 0.5, -0.5, 0.866);
    for (int i = 0; i < 8; i++) {
        v += a * gnoise(p);
        p = rot * p * 2.1 + vec2(1.7, 9.2);
        a *= 0.47;
    }
    return v * 0.5 + 0.5;
}

float fbmRidged(vec2 p) {
    float v = 0.0, a = 0.5;
    mat2 rot = mat2(0.866, 0.5, -0.5, 0.866);
    for (int i = 0; i < 6; i++) {
        float n = gnoise(p);
        v += a * (1.0 - abs(n));
        p = rot * p * 2.1 + vec2(1.7, 9.2);
        a *= 0.45;
    }
    return v;
}

vec2 voronoi(vec2 p) {
    vec2 n = floor(p);
    vec2 f = fract(p);
    float md = 8.0;
    float md2 = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 o = hashG(n + g) * 0.5 + 0.5;
            float d = length(g + o - f);
            if (d < md) { md2 = md; md = d; }
            else if (d < md2) { md2 = d; }
        }
    }
    return vec2(md, md2);
}

float mercurySurface(vec2 p, float t) {
    vec2 q = vec2(fbm(p + t * 0.12), fbm(p + vec2(5.2, 1.3) - t * 0.1));
    vec2 r = vec2(fbm(p + 4.0 * q + vec2(1.7, 9.2) + t * 0.07),
                  fbm(p + 4.0 * q + vec2(8.3, 2.8) - t * 0.08));
    vec2 s = vec2(fbm(p + 3.0 * r + vec2(3.1, 7.4) + t * 0.04),
                  fbm(p + 3.0 * r + vec2(6.5, 4.1) - t * 0.05));
    float base = fbm(p + 3.5 * s);
    float ridge = fbmRidged(p * 3.0 + s * 2.0 + t * 0.06);
    return base + ridge * 0.08;
}

float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float metaballs(vec2 uv, float t) {
    float field = 1e5;
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        float phase = fi * 1.618 + t * (0.3 + fi * 0.05);
        vec2 center = vec2(sin(phase * 0.7 + fi * 2.4) * 0.55,
                           cos(phase * 0.5 + fi * 1.7) * 0.45);
        float radius = 0.1 + 0.05 * sin(t * 0.8 + fi * 3.0);
        float d = length(uv - center) - radius;
        field = smin(field, d, 0.15);
    }
    return field;
}

vec3 envMap(vec3 rd, float t) {
    float y = rd.y;
    float xza = atan(rd.x, rd.z);

    vec3 sky = mix(vec3(0.4, 0.5, 0.75), vec3(0.85, 0.9, 1.0), smoothstep(0.0, 0.9, y));
    sky += vec3(0.15, 0.1, 0.05) * exp(-3.0 * y);
    vec3 ground = mix(vec3(0.1, 0.08, 0.06), vec3(0.25, 0.2, 0.18), smoothstep(-0.8, 0.0, y));
    vec3 col = mix(ground, sky, smoothstep(-0.08, 0.08, y));

    float horizon = exp(-12.0 * y * y);
    col += vec3(0.9, 0.6, 0.3) * horizon * 0.25;
    col += vec3(0.4, 0.2, 0.6) * horizon * 0.1 * (sin(xza * 2.0) * 0.5 + 0.5);

    float cn1 = gnoise3(rd * 4.0 + vec3(t * 0.08, 0.0, t * 0.05)) * 0.5 + 0.5;
    float cn2 = gnoise3(rd * 10.0 - vec3(0.0, t * 0.06, t * 0.04)) * 0.5 + 0.5;
    float cn3 = gnoise3(rd * 20.0 + vec3(t * 0.03)) * 0.5 + 0.5;
    col *= 0.8 + 0.4 * cn1;
    col += vec3(0.15, 0.13, 0.12) * smoothstep(0.5, 0.75, cn2) * 0.5;
    col += vec3(0.05) * smoothstep(0.55, 0.7, cn3);

    for (int i = 0; i < 5; i++) {
        float fi = float(i);
        vec3 ld = normalize(vec3(
            sin(fi * 2.1 + t * 0.12) * (0.8 + fi * 0.1),
            0.2 + fi * 0.15 + sin(t * 0.2 + fi) * 0.1,
            cos(fi * 1.7 + t * 0.08)
        ));
        float d = max(dot(rd, ld), 0.0);
        col += vec3(1.0, 0.97, 0.93) * pow(d, 200.0) * 3.0;
        col += vec3(1.0, 0.95, 0.9) * pow(d, 30.0) * 0.4;
        col += vec3(0.9, 0.9, 1.0) * pow(d, 6.0) * 0.08;
    }

    return col;
}

float D_GGX(float NdotH, float r) {
    float a = r * r; float a2 = a * a;
    float d = NdotH * NdotH * (a2 - 1.0) + 1.0;
    return a2 / (3.14159 * d * d + 0.0001);
}

float D_GGX_Aniso(float NdotH, float TdotH, float BdotH, float ax, float ay) {
    float d = TdotH * TdotH / (ax * ax) + BdotH * BdotH / (ay * ay) + NdotH * NdotH;
    return 1.0 / (3.14159 * ax * ay * d * d + 0.0001);
}

float G_Smith(float NdotV, float NdotL, float r) {
    float k = (r + 1.0); k = k * k / 8.0;
    return (NdotV / (NdotV * (1.0 - k) + k)) * (NdotL / (NdotL * (1.0 - k) + k));
}

vec3 F_Schlick(float cos0, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cos0, 0.0, 1.0), 5.0);
}

vec3 thinFilmMulti(float cosTheta, float t1, float t2) {
    float nf1 = 1.38, nf2 = 1.52;
    float cosT1 = sqrt(max(0.0, 1.0 - (1.0 - cosTheta * cosTheta) / (nf1 * nf1)));
    float cosT2 = sqrt(max(0.0, 1.0 - (1.0 - cosTheta * cosTheta) / (nf2 * nf2)));
    float opd1 = 2.0 * nf1 * t1 * cosT1;
    float opd2 = 2.0 * nf2 * t2 * cosT2;
    vec3 wl = vec3(650.0, 510.0, 475.0);
    vec3 ph1 = 2.0 * 3.14159 * opd1 / (wl * 0.001);
    vec3 ph2 = 2.0 * 3.14159 * opd2 / (wl * 0.001);
    vec3 R1 = 0.5 + 0.5 * cos(ph1 + 3.14159);
    vec3 R2 = 0.5 + 0.5 * cos(ph2 + 3.14159);
    return R1 * R2;
}

float caustics(vec2 uv, float t) {
    float c = 0.0;
    for (int i = 0; i < 3; i++) {
        float fi = float(i) + 1.0;
        vec2 p = uv * (4.0 * fi) + t * vec2(0.12 * fi, -0.08 * fi);
        vec2 v = voronoi(p + gnoise(p * 0.5) * 0.8);
        float edge = smoothstep(0.0, 0.15, v.y - v.x);
        c += (1.0 - edge) * pow(1.0 - v.x, 3.0) / fi;
    }
    return c;
}

vec3 sss(vec3 N, vec3 L, vec3 V, float thickness) {
    vec3 scatterDir = normalize(L + N * 0.5);
    float scatter = pow(clamp(dot(V, -scatterDir), 0.0, 1.0), 3.0);
    return vec3(0.7, 0.75, 0.8) * scatter * thickness * 0.15;
}

vec3 shade(vec2 uv, float t) {
    float meta = metaballs(uv, t);
    float metaShape = 1.0 - smoothstep(-0.02, 0.05, meta);
    float metaEdge = 1.0 - smoothstep(0.0, 0.02, abs(meta));

    float surface = mercurySurface(uv * 1.8, t);

    vec2 vor = voronoi(uv * 12.0 + surface * 3.0);
    float microDetail = smoothstep(0.0, 0.1, vor.y - vor.x) * 0.02;

    float eps = 0.002;
    float hC = mercurySurface(uv * 1.8, t) + microDetail;
    float hL = mercurySurface((uv + vec2(-eps, 0.0)) * 1.8, t);
    float hR = mercurySurface((uv + vec2(eps, 0.0)) * 1.8, t);
    float hD = mercurySurface((uv + vec2(0.0, -eps)) * 1.8, t);
    float hU = mercurySurface((uv + vec2(0.0, eps)) * 1.8, t);
    vec3 N = normalize(vec3((hL - hR) * 0.6, (hD - hU) * 0.6, eps * 2.0));

    float meps = 0.005;
    float mC = metaballs(uv, t);
    float mL = metaballs(uv + vec2(-meps, 0.0), t);
    float mR = metaballs(uv + vec2(meps, 0.0), t);
    float mD = metaballs(uv + vec2(0.0, -meps), t);
    float mU = metaballs(uv + vec2(0.0, meps), t);
    vec2 mGrad = vec2(mL - mR, mD - mU) / (2.0 * meps);
    N = normalize(N + vec3(mGrad * 0.02 * metaEdge, 0.0));

    vec3 T = normalize(cross(N, vec3(0.0, 1.0, 0.001)));
    vec3 B = cross(N, T);

    vec3 V = vec3(0.0, 0.0, 1.0);
    float NdotV = max(dot(N, V), 0.0);
    vec3 R = reflect(-V, N);

    float roughness = 0.03 + 0.05 * (1.0 - metaShape) + microDetail * 2.0;
    vec3 env = envMap(R, t);
    vec3 envBlur = envMap(normalize(R + vec3(0.05, 0.0, 0.0)), t)
                 + envMap(normalize(R + vec3(-0.05, 0.0, 0.0)), t)
                 + envMap(normalize(R + vec3(0.0, 0.05, 0.0)), t)
                 + envMap(normalize(R + vec3(0.0, -0.05, 0.0)), t);
    envBlur *= 0.25;
    env = mix(env, envBlur, roughness * 5.0);

    vec3 F0 = vec3(0.78, 0.77, 0.74);

    float aniso = 0.3 + 0.2 * sin(surface * 10.0 + t);
    float ax = roughness * (1.0 + aniso);
    float ay = roughness * (1.0 - aniso * 0.5);

    vec3 specTotal = vec3(0.0);
    vec3 sssTotal = vec3(0.0);
    vec3 lD[5]; vec3 lC[5];
    lD[0] = normalize(vec3(sin(t*0.7)*0.8, cos(t*0.5)*0.6, 1.0));
    lD[1] = normalize(vec3(cos(t*0.4)*0.6, sin(t*0.6)*0.7, 0.9));
    lD[2] = normalize(vec3(-sin(t*0.3)*0.5, 0.8, cos(t*0.5)*0.7));
    lD[3] = normalize(vec3(cos(t*0.8)*0.4, -sin(t*0.35)*0.3, 1.1));
    lD[4] = normalize(vec3(sin(t*0.25)*0.7, cos(t*0.45)*0.4, 0.85));
    lC[0] = vec3(1.0, 0.97, 0.92) * 1.8;
    lC[1] = vec3(0.9, 0.92, 1.0) * 1.4;
    lC[2] = vec3(1.0, 0.95, 0.85) * 1.0;
    lC[3] = vec3(0.85, 0.9, 1.0) * 0.7;
    lC[4] = vec3(0.95, 0.88, 1.0) * 0.5;

    for (int i = 0; i < 5; i++) {
        vec3 L = lD[i];
        vec3 H = normalize(L + V);
        float NdL = max(dot(N, L), 0.0);
        float NdH = max(dot(N, H), 0.0);
        float VdH = max(dot(V, H), 0.0);
        float TdH = dot(T, H);
        float BdH = dot(B, H);

        float Da = D_GGX_Aniso(NdH, TdH, BdH, ax, ay);
        float Di = D_GGX(NdH, roughness);
        float D = mix(Di, Da, 0.4);
        float G = G_Smith(NdotV, NdL, roughness);
        vec3 F = F_Schlick(VdH, F0);

        specTotal += (D * G * F) / (4.0 * NdotV * NdL + 0.001) * lC[i] * NdL;
        sssTotal += sss(N, L, V, 1.0 - metaShape) * lC[i] * 0.3;
    }

    float film1 = 280.0 + 220.0 * surface + 60.0 * sin(t * 0.3);
    float film2 = 150.0 + 100.0 * fbmRidged(uv * 4.0 + t * 0.05);
    vec3 tf = thinFilmMulti(NdotV, film1, film2);
    float irisStr = pow(1.0 - NdotV, 2.5) * 0.7;

    float caust = caustics(uv + N.xy * 0.15, t);

    vec3 col = vec3(0.0);
    col += env * F_Schlick(NdotV, F0) * 0.75;
    col += specTotal;
    col += sssTotal;
    col = mix(col, col * tf * 1.6, irisStr);
    col += vec3(0.95, 0.97, 1.0) * caust * 0.15 * metaShape;
    col += vec3(0.85, 0.9, 1.0) * metaEdge * 0.2;
    float innerGlow = pow(metaEdge, 2.0);
    col += vec3(1.0, 0.98, 0.95) * innerGlow * 0.1;
    col *= smoothstep(-0.8, 0.3, uv.y) * 0.15 + 0.85;
    col *= mix(vec3(1.0), vec3(0.84, 0.84, 0.86), 0.25);
    col = mix(col, col * u_FillColor.rgb * 1.8, 0.12);

    return col;
}

vec3 postProcess(vec3 col, vec2 uv) {
    float dist = length(uv) * 0.8;
    float caStr = dist * dist * 0.015;
    col.r *= 1.0 + caStr;
    col.b *= 1.0 - caStr;

    float lum = dot(col, vec3(0.2126, 0.7152, 0.0722));
    float bloomMask = smoothstep(0.7, 1.2, lum);
    col += col * bloomMask * 0.3;

    col = (col * (2.51 * col + 0.03)) / (col * (2.43 * col + 0.59) + 0.14);

    float vig = 1.0 - dist * 0.3;
    col *= vig;

    float grain = (hashG(uv * 500.0 + fract(col.xy * 100.0)).x) * 0.015;
    col += grain;

    return clamp(col, 0.0, 1.0);
}

vec4 mainImage(bool outline) {
    if (outline) return u_OutlineColor;

    float t = u_Time * 0.5;
    float invMin = 1.0 / min(u_Size.x, u_Size.y);

    vec2 offsets[4];
    offsets[0] = vec2(-0.125, -0.375);
    offsets[1] = vec2( 0.375, -0.125);
    offsets[2] = vec2(-0.375,  0.125);
    offsets[3] = vec2( 0.125,  0.375);

    vec3 col = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        vec2 uv = (gl_FragCoord.xy + offsets[i] - 0.5 * u_Size) * invMin;
        col += shade(uv, t);
    }
    col *= 0.25;

    vec2 uvCenter = (gl_FragCoord.xy - 0.5 * u_Size) * invMin;
    col = postProcess(col, uvCenter);

    return vec4(col, u_FillColor.a);
}
