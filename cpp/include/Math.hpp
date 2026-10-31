#pragma once

#include <cmath>
#include <algorithm>

namespace ShadowCombat {

constexpr float PI = 3.14159265358979323846f;

struct Vec2 {
    float x = 0.0f;
    float y = 0.0f;

    constexpr Vec2() = default;
    constexpr Vec2(float px, float py) : x(px), y(py) {}

    Vec2 operator+(const Vec2& o) const { return Vec2(x + o.x, y + o.y); }
    Vec2 operator-(const Vec2& o) const { return Vec2(x - o.x, y - o.y); }
    Vec2 operator*(float s) const { return Vec2(x * s, y * s); }
    Vec2& operator+=(const Vec2& o) { x += o.x; y += o.y; return *this; }
    Vec2& operator-=(const Vec2& o) { x -= o.x; y -= o.y; return *this; }
    Vec2& operator*=(float s) { x *= s; y *= s; return *this; }

    float lengthSquared() const { return x * x + y * y; }
    float length() const { return std::sqrt(lengthSquared()); }

    Vec2 normalized() const {
        float l = length();
        return (l > 0.0001f) ? Vec2(x / l, y / l) : Vec2(0.0f, 0.0f);
    }
};

struct Rect {
    float left = 0.0f;
    float top = 0.0f;
    float right = 0.0f;
    float bottom = 0.0f;

    constexpr Rect() = default;
    constexpr Rect(float l, float t, float r, float b)
        : left(l), top(t), right(r), bottom(b) {}

    bool intersects(const Rect& o) const {
        return left < o.right && right > o.left && bottom < o.top && top > o.bottom;
    }

    float width() const { return right - left; }
    float height() const { return bottom - top; }
};

inline float clamp(float v, float minVal, float maxVal) {
    return std::max(minVal, std::min(maxVal, v));
}

inline float lerp(float a, float b, float t) {
    return a + (b - a) * clamp(t, 0.0f, 1.0f);
}

} // namespace ShadowCombat
