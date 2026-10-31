#include "TerminalRenderer.hpp"
#include <iostream>
#include <sstream>
#include <iomanip>

namespace ShadowCombat {

// ANSI escape codes for colors
static const std::string ANSI_RESET       = "\033[0m";
static const std::string ANSI_RED         = "\033[1;31m";
static const std::string ANSI_GREEN       = "\033[1;32m";
static const std::string ANSI_YELLOW      = "\033[1;33m";
static const std::string ANSI_BLUE        = "\033[1;34m";
static const std::string ANSI_MAGENTA     = "\033[1;35m";
static const std::string ANSI_CYAN        = "\033[1;36m";
static const std::string ANSI_WHITE       = "\033[1;37m";
static const std::string ANSI_BOLD_BLACK  = "\033[1;30m";
static const std::string ANSI_BG_RED      = "\033[41m";
static const std::string ANSI_BG_CYAN     = "\033[46m";
static const std::string ANSI_BG_MAGENTA  = "\033[45m";

TerminalRenderer::TerminalRenderer() {
    clearBuffer();
}

void TerminalRenderer::clearBuffer() {
    for (int y = 0; y < SCREEN_H; ++y) {
        for (int x = 0; x < SCREEN_W; ++x) {
            buffer[y][x] = ' ';
            colorBuffer[y][x] = ANSI_RESET;
        }
    }
}

void TerminalRenderer::setChar(int x, int y, char c, const std::string& color) {
    if (x >= 0 && x < SCREEN_W && y >= 0 && y < SCREEN_H) {
        buffer[y][x] = c;
        colorBuffer[y][x] = color;
    }
}

void TerminalRenderer::drawString(int x, int y, const std::string& str, const std::string& color) {
    for (size_t i = 0; i < str.length(); ++i) {
        setChar(x + static_cast<int>(i), y, str[i], color);
    }
}

void TerminalRenderer::render(const CombatEngine& engine) {
    clearBuffer();

    // 1. Draw Background Parallax & Scenery
    drawArena(engine.arena);

    // 2. Draw Projectiles
    drawProjectiles(engine.projectiles);

    // 3. Draw Fighters
    drawFighter(engine.player, true);
    drawFighter(engine.enemy, false);

    // 4. Draw Particles & FX
    drawParticles(engine.particles);

    // 5. Draw HUD & Health bars
    drawHud(engine);
}

void TerminalRenderer::drawHud(const CombatEngine& engine) {
    // Border top
    for (int x = 0; x < SCREEN_W; ++x) {
        setChar(x, 0, '=', ANSI_BOLD_BLACK);
    }

    // Player name & Weapon
    std::string pName = engine.player.name + " [" + engine.player.weapon.name + "]";
    drawString(2, 1, pName, ANSI_CYAN);

    // Boss name & Weapon
    std::string eName = engine.enemy.name + " [" + engine.enemy.weapon.name + "]";
    drawString(SCREEN_W - static_cast<int>(eName.length()) - 2, 1, eName, ANSI_RED);

    // Round Indicator
    std::string roundStr = "ROUND " + std::to_string(engine.currentRound);
    drawString((SCREEN_W - static_cast<int>(roundStr.length())) / 2, 1, roundStr, ANSI_YELLOW);

    // Player Health Bar (24 chars)
    int barLen = 24;
    float pPct = clamp(engine.player.health / engine.player.maxHealth, 0.0f, 1.0f);
    int pFill = static_cast<int>(pPct * barLen);
    drawString(2, 2, "[", ANSI_WHITE);
    for (int i = 0; i < barLen; ++i) {
        if (i < pFill) {
            setChar(3 + i, 2, '#', (pPct > 0.3f) ? ANSI_GREEN : ANSI_RED);
        } else {
            setChar(3 + i, 2, '.', ANSI_BOLD_BLACK);
        }
    }
    drawString(3 + barLen, 2, "]", ANSI_WHITE);

    // Enemy Health Bar (24 chars)
    float ePct = clamp(engine.enemy.health / engine.enemy.maxHealth, 0.0f, 1.0f);
    int eFill = static_cast<int>(ePct * barLen);
    int eStart = SCREEN_W - barLen - 4;
    drawString(eStart, 2, "[", ANSI_WHITE);
    for (int i = 0; i < barLen; ++i) {
        int idx = barLen - 1 - i;
        if (idx < eFill) {
            setChar(eStart + 1 + i, 2, '#', (ePct > 0.3f) ? ANSI_GREEN : ANSI_RED);
        } else {
            setChar(eStart + 1 + i, 2, '.', ANSI_BOLD_BLACK);
        }
    }
    drawString(eStart + 1 + barLen, 2, "]", ANSI_WHITE);

    // Shadow Energy Gauges
    std::string pShadow = "SHADOW: " + std::to_string(static_cast<int>(engine.player.shadowEnergy)) + "%";
    if (engine.player.isShadowMode) pShadow += " [ACTIVE!]";
    drawString(2, 3, pShadow, engine.player.isShadowMode ? ANSI_MAGENTA : ANSI_CYAN);

    std::string eShadow = "SHADOW: " + std::to_string(static_cast<int>(engine.enemy.shadowEnergy)) + "%";
    if (engine.enemy.isShadowMode) eShadow += " [ACTIVE!]";
    drawString(SCREEN_W - static_cast<int>(eShadow.length()) - 2, 3, eShadow, engine.enemy.isShadowMode ? ANSI_MAGENTA : ANSI_RED);

    // Score / Wins
    std::string wins = "WINS: " + std::to_string(engine.playerRoundWins) + " - " + std::to_string(engine.enemyRoundWins);
    drawString((SCREEN_W - static_cast<int>(wins.length())) / 2, 2, wins, ANSI_WHITE);
}

void TerminalRenderer::drawArena(ArenaType arena) {
    // Ground line at Y = 20
    int groundY = 20;
    for (int x = 0; x < SCREEN_W; ++x) {
        setChar(x, groundY, '_', ANSI_BOLD_BLACK);
        for (int y = groundY + 1; y < SCREEN_H; ++y) {
            setChar(x, y, '#', ANSI_BOLD_BLACK);
        }
    }

    // Sky Silhouettes based on Arena
    switch (arena) {
        case ArenaType::CRIMSON_PAGODA: {
            // Sun
            drawString(34, 5, "   .---.   ", ANSI_YELLOW);
            drawString(34, 6, "  / SUN \\  ", ANSI_YELLOW);
            drawString(34, 7, "  ' === '  ", ANSI_YELLOW);
            // Pagoda silhouette on the far left
            drawString(4, 11, "     /\\     ", ANSI_RED);
            drawString(4, 12, "  ==/__\\==  ", ANSI_RED);
            drawString(4, 13, "   |    |   ", ANSI_RED);
            drawString(4, 14, " ===/--\\=== ", ANSI_RED);
            drawString(4, 15, "  |      |  ", ANSI_RED);
            drawString(4, 16, " =/======\\= ", ANSI_RED);
            drawString(4, 17, "  | [  ] |  ", ANSI_RED);
            drawString(4, 18, "  |  ||  |  ", ANSI_RED);
            drawString(4, 19, "  |  ||  |  ", ANSI_RED);
            break;
        }
        case ArenaType::BAMBOO_GROVE: {
            // Moon
            drawString(60, 4, "  .---.  ", ANSI_CYAN);
            drawString(60, 5, " / MOON\\ ", ANSI_CYAN);
            drawString(60, 6, " ' --- ' ", ANSI_CYAN);
            // Bamboo stalks
            for (int y = 7; y < groundY; ++y) {
                setChar(8, y, '|', ANSI_GREEN);
                setChar(10, y, '|', ANSI_GREEN);
                setChar(22, y, '|', ANSI_GREEN);
                setChar(70, y, '|', ANSI_GREEN);
                setChar(72, y, '|', ANSI_GREEN);
            }
            break;
        }
        case ArenaType::GATES_OF_SHADOWS: {
            // Gates Portal Arch
            drawString(28, 8,  "  /============\\  ", ANSI_MAGENTA);
            drawString(28, 9,  " ||  DEMON GATE || ", ANSI_MAGENTA);
            drawString(28, 10, " ||    (@@)     || ", ANSI_MAGENTA);
            for (int y = 11; y < groundY; ++y) {
                drawString(28, y, " ||             || ", ANSI_MAGENTA);
            }
            break;
        }
        case ArenaType::MOONLIT_TEMPLE: {
            // Giant Moon
            drawString(33, 4, "  .----------.  ", ANSI_WHITE);
            drawString(33, 5, " /   TEMPLE   \\ ", ANSI_CYAN);
            drawString(33, 6, " |    MOON    | ", ANSI_WHITE);
            drawString(33, 7, " \\            / ", ANSI_CYAN);
            drawString(33, 8, "  '----------'  ", ANSI_WHITE);
            // Temple curved rooftop ridge
            drawString(6, 15, "      /\\      ", ANSI_BLUE);
            drawString(4, 16, "  ___/__\\___  ", ANSI_BLUE);
            drawString(2, 17, " /__________\\ ", ANSI_BLUE);
            drawString(4, 18, "   | [||] |   ", ANSI_BLUE);
            drawString(4, 19, "   |======|   ", ANSI_BLUE);
            // Distant Spire
            drawString(68, 13, "  /|\\  ", ANSI_CYAN);
            drawString(67, 14, " =/ \\= ", ANSI_CYAN);
            drawString(67, 15, "  | |  ", ANSI_CYAN);
            drawString(66, 16, " ==|== ", ANSI_CYAN);
            break;
        }
        default:
            break;
    }
}

void TerminalRenderer::drawFighter(const Fighter& f, bool isPlayer) {
    // Map world X (0 - 800) to screen X (0 - 80)
    int fx = static_cast<int>((f.x / 800.0f) * SCREEN_W);
    int groundY = 20;
    int jumpOffset = static_cast<int>(f.y / 20.0f);
    int fy = groundY - 1 - jumpOffset;

    std::string color = f.isShadowMode ? (isPlayer ? ANSI_CYAN : ANSI_MAGENTA) : (isPlayer ? ANSI_WHITE : ANSI_RED);

    // Stance Poses
    switch (f.actionState) {
        case ActionState::IDLE: {
            setChar(fx, fy - 3, 'O', color);     // Head
            setChar(fx, fy - 2, '|', color);     // Torso
            setChar(fx - 1, fy - 2, '/', color); // Left arm
            setChar(fx + 1, fy - 2, '\\', color);// Right arm
            setChar(fx, fy - 1, '|', color);     // Waist
            setChar(fx - 1, fy, '/', color);     // Left leg
            setChar(fx + 1, fy, '\\', color);    // Right leg
            break;
        }
        case ActionState::WALK_FORWARD:
        case ActionState::WALK_BACKWARD: {
            setChar(fx, fy - 3, 'O', color);
            setChar(fx, fy - 2, '|', color);
            setChar(fx - 1, fy - 2, '<', color);
            setChar(fx + 1, fy - 2, '>', color);
            setChar(fx, fy - 1, '|', color);
            setChar(fx - 1, fy, '/', color);
            setChar(fx + 1, fy, '/', color);
            break;
        }
        case ActionState::BLOCKING: {
            setChar(fx, fy - 3, 'O', color);
            setChar(fx, fy - 2, '|', color);
            setChar(fx + (f.facingRight ? 1 : -1), fy - 2, '#', ANSI_YELLOW); // Guard shield
            setChar(fx, fy - 1, '|', color);
            setChar(fx - 1, fy, '/', color);
            setChar(fx + 1, fy, '\\', color);
            break;
        }
        case ActionState::CROUCH: {
            setChar(fx, fy - 2, 'O', color);
            setChar(fx, fy - 1, '<', color);
            setChar(fx, fy, '>', color);
            break;
        }
        case ActionState::PUNCH_JAB:
        case ActionState::PUNCH_HOOK:
        case ActionState::PUNCH_UPPERCUT: {
            setChar(fx, fy - 3, 'O', color);
            setChar(fx, fy - 2, '|', color);
            int armX = f.facingRight ? fx + 1 : fx - 1;
            int fistX = f.facingRight ? fx + 2 : fx - 2;
            setChar(armX, fy - 2, '=', color);
            setChar(fistX, fy - 2, '*', ANSI_YELLOW);
            setChar(fx, fy - 1, '|', color);
            setChar(fx - 1, fy, '/', color);
            setChar(fx + 1, fy, '\\', color);
            break;
        }
        case ActionState::KICK_LOW:
        case ActionState::KICK_ROUNDHOUSE:
        case ActionState::KICK_FLYING: {
            setChar(fx, fy - 3, 'O', color);
            setChar(fx, fy - 2, '/', color);
            setChar(fx, fy - 1, '|', color);
            int legX = f.facingRight ? fx + 1 : fx - 1;
            int footX = f.facingRight ? fx + 2 : fx - 2;
            setChar(legX, fy - 1, '-', color);
            setChar(footX, fy - 1, '>', ANSI_YELLOW);
            break;
        }
        case ActionState::WEAPON_SLASH:
        case ActionState::WEAPON_SPIN:
        case ActionState::WEAPON_PIERCE: {
            setChar(fx, fy - 3, 'O', color);
            setChar(fx, fy - 2, '|', color);
            int wX = f.facingRight ? fx + 1 : fx - 1;
            int tipX = f.facingRight ? fx + 2 : fx - 2;
            int tip2X = f.facingRight ? fx + 3 : fx - 3;
            setChar(wX, fy - 2, '-', color);
            setChar(tipX, fy - 2, '=', ANSI_CYAN);
            setChar(tip2X, fy - 2, '>', ANSI_CYAN);
            setChar(fx, fy - 1, '|', color);
            setChar(fx - 1, fy, '/', color);
            setChar(fx + 1, fy, '\\', color);
            break;
        }
        case ActionState::SHADOW_SURGE: {
            setChar(fx, fy - 3, '@', ANSI_MAGENTA);
            setChar(fx, fy - 2, 'X', ANSI_MAGENTA);
            drawString(fx - 2, fy - 1, "(-*-)", ANSI_MAGENTA);
            setChar(fx - 1, fy, '/', ANSI_MAGENTA);
            setChar(fx + 1, fy, '\\', ANSI_MAGENTA);
            break;
        }
        case ActionState::KNOCKDOWN:
        case ActionState::DEFEAT: {
            drawString(fx - 1, fy, "__o__", color);
            break;
        }
        case ActionState::VICTORY: {
            setChar(fx, fy - 4, '\\', ANSI_YELLOW);
            setChar(fx + 1, fy - 4, '/', ANSI_YELLOW);
            setChar(fx, fy - 3, 'O', color);
            setChar(fx, fy - 2, '|', color);
            setChar(fx, fy - 1, '|', color);
            setChar(fx - 1, fy, '/', color);
            setChar(fx + 1, fy, '\\', color);
            break;
        }
        default:
            setChar(fx, fy - 3, 'O', color);
            setChar(fx, fy - 2, '|', color);
            setChar(fx - 1, fy, '/', color);
            setChar(fx + 1, fy, '\\', color);
            break;
    }
}

void TerminalRenderer::drawProjectiles(const std::vector<Projectile>& projectiles) {
    for (const auto& p : projectiles) {
        int px = static_cast<int>((p.x / 800.0f) * SCREEN_W);
        int py = 20 - static_cast<int>(p.y / 20.0f);
        setChar(px, py, '+', ANSI_YELLOW);
    }
}

void TerminalRenderer::drawParticles(const ParticleSystem& particles) {
    for (const auto& p : particles.getParticles()) {
        int px = static_cast<int>((p.pos.x / 800.0f) * SCREEN_W);
        int py = 20 - static_cast<int>(p.pos.y / 20.0f);
        char c = '*';
        std::string col = ANSI_YELLOW;
        if (p.type == ParticleType::BLOOD_SPLITTER) {
            c = '.';
            col = ANSI_RED;
        } else if (p.type == ParticleType::SHADOW_FLAME) {
            c = '~';
            col = ANSI_MAGENTA;
        }
        setChar(px, py, c, col);
    }

    for (const auto& t : particles.getFloatingTexts()) {
        int tx = static_cast<int>((t.pos.x / 800.0f) * SCREEN_W);
        int ty = 20 - static_cast<int>(t.pos.y / 20.0f);
        drawString(tx, ty, t.text, ANSI_YELLOW);
    }
}

void TerminalRenderer::present() const {
    // Clear screen and reset cursor
    std::cout << "\033[H";
    for (int y = 0; y < SCREEN_H; ++y) {
        std::string currentSeq = "";
        for (int x = 0; x < SCREEN_W; ++x) {
            if (colorBuffer[y][x] != currentSeq) {
                currentSeq = colorBuffer[y][x];
                std::cout << currentSeq;
            }
            std::cout << buffer[y][x];
        }
        std::cout << ANSI_RESET << "\n";
    }
    std::cout << std::flush;
}

} // namespace ShadowCombat
