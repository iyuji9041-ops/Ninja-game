package com.example.game.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.example.game.model.ArenaType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precomputes and caches Path and Brush objects per arena size to avoid GC churn
 * and allocation overhead during 60FPS combat rendering.
 */
class ArenaRenderCache {
    var cachedWidth = 0f
    var cachedHeight = 0f
    var cachedGroundY = 0f

    // Cached Brushes: Crimson Pagoda
    var crimsonSkyBrush: Brush? = null
    var crimsonSunBrush: Brush? = null
    var crimsonGroundBrush: Brush? = null

    // Cached Brushes: Bamboo Grove
    var bambooSkyBrush: Brush? = null
    var bambooMoonBrush: Brush? = null
    var bambooGroundBrush: Brush? = null

    // Cached Brushes: Gates of Shadows
    var gatesSkyBrush: Brush? = null
    var gatesEclipseBrush: Brush? = null
    var gatesGroundBrush: Brush? = null

    // Cached Brushes: Moonlit Temple
    var moonlitSkyBrush: Brush? = null
    var moonlitMoonBrush: Brush? = null
    var moonlitGroundBrush: Brush? = null

    // Cached Paths
    val mountainPath = Path()
    val pineTrunkPath = Path()
    val toriiTopBeamPath = Path()
    val pagodaRoofPaths = Array(4) { Path() }
    val pillarPath = Path()
    val templeRoofPath = Path()
    val stoneLanternPath = Path()

    fun updateCacheIfNeeded(w: Float, h: Float, groundY: Float) {
        if (w == cachedWidth && h == cachedHeight && groundY == cachedGroundY) return

        cachedWidth = w
        cachedHeight = h
        cachedGroundY = groundY

        // 1. Crimson Pagoda Brushes
        crimsonSkyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF280B1C),
                Color(0xFF6B1D2F),
                Color(0xFFBA382E),
                Color(0xFFEE7B30),
                Color(0xFFFFB142)
            ),
            startY = 0f,
            endY = groundY
        )
        crimsonGroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B1118),
                Color(0xFF0D080D),
                Color(0xFF050305)
            ),
            startY = groundY,
            endY = h
        )

        // 2. Bamboo Grove Brushes
        bambooSkyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF060B19),
                Color(0xFF0F1E36),
                Color(0xFF143048),
                Color(0xFF1D4A5C)
            ),
            startY = 0f,
            endY = groundY
        )
        bambooGroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A141A),
                Color(0xFF050A0E)
            ),
            startY = groundY,
            endY = h
        )

        // 3. Gates of Shadows Brushes
        gatesSkyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF140722),
                Color(0xFF2C0F45),
                Color(0xFF4C1052),
                Color(0xFF6B1538),
                Color(0xFF8F221E)
            ),
            startY = 0f,
            endY = groundY
        )
        gatesGroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF140817),
                Color(0xFF0A030C)
            ),
            startY = groundY,
            endY = h
        )

        // 4. Moonlit Temple Brushes
        moonlitSkyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050B18),
                Color(0xFF0C1935),
                Color(0xFF162B54),
                Color(0xFF203F74),
                Color(0xFF38639F)
            ),
            startY = 0f,
            endY = groundY
        )
        moonlitGroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C2133),
                Color(0xFF121624),
                Color(0xFF080B13)
            ),
            startY = groundY,
            endY = h
        )

        // Precompute Mountain Path
        mountainPath.reset()
        mountainPath.moveTo(-w * 0.5f, groundY)
        mountainPath.lineTo(-w * 0.5f, groundY * 0.65f)
        mountainPath.lineTo(w * 0.2f, groundY * 0.45f)
        mountainPath.lineTo(w * 0.45f, groundY * 0.68f)
        mountainPath.lineTo(w * 0.75f, groundY * 0.42f)
        mountainPath.lineTo(w * 0.95f, groundY * 0.60f)
        mountainPath.lineTo(w * 1.5f, groundY * 0.52f)
        mountainPath.lineTo(w * 1.5f, groundY)
        mountainPath.close()

        // Precompute Pine Trunk Path
        val pineHeight = h * 0.45f
        pineTrunkPath.reset()
        pineTrunkPath.moveTo(-14f, 0f)
        pineTrunkPath.quadraticTo(-22f, -pineHeight * 0.4f, -10f, -pineHeight * 0.7f)
        pineTrunkPath.lineTo(6f, -pineHeight * 0.7f)
        pineTrunkPath.quadraticTo(12f, -pineHeight * 0.35f, 16f, 0f)
        pineTrunkPath.close()

        // Precompute Pagoda Roof Paths
        val pagodaHeight = h * 0.55f
        val tiers = 4
        val tierHeight = pagodaHeight / tiers
        var currentW = 140f
        for (tier in 0 until tiers) {
            val ty = -(tier * tierHeight)
            val path = pagodaRoofPaths[tier]
            path.reset()
            path.moveTo(-currentW * 0.7f, ty - tierHeight * 0.3f)
            path.quadraticTo(0f, ty - tierHeight * 0.15f, currentW * 0.7f, ty - tierHeight * 0.3f)
            path.lineTo(currentW * 0.55f, ty)
            path.lineTo(-currentW * 0.55f, ty)
            path.close()
            currentW *= 0.82f
        }

        // Precompute Torii Top Kasagi Beam Path
        val beamW = 280f
        val toriiHeight = h * 0.65f
        toriiTopBeamPath.reset()
        toriiTopBeamPath.moveTo(-beamW / 2, -toriiHeight - 15f)
        toriiTopBeamPath.quadraticTo(0f, -toriiHeight + 5f, beamW / 2, -toriiHeight - 15f)
        toriiTopBeamPath.lineTo(beamW / 2 - 10f, -toriiHeight + 18f)
        toriiTopBeamPath.quadraticTo(0f, -toriiHeight + 22f, -beamW / 2 + 10f, -toriiHeight + 18f)
        toriiTopBeamPath.close()

        // Precompute Pillar Path
        val pillarHeight = h * 0.45f
        pillarPath.reset()
        pillarPath.moveTo(-18f, 0f)
        pillarPath.lineTo(-10f, -pillarHeight)
        pillarPath.lineTo(12f, -pillarHeight + 15f)
        pillarPath.lineTo(22f, 0f)
        pillarPath.close()

        // Precompute Temple Roof Path (Traditional Sweeping Eaves)
        val roofSpan = 220f
        val roofRise = 42f
        templeRoofPath.reset()
        templeRoofPath.moveTo(-roofSpan / 2, 0f)
        templeRoofPath.quadraticTo(-roofSpan * 0.25f, -roofRise * 0.3f, 0f, -roofRise)
        templeRoofPath.quadraticTo(roofSpan * 0.25f, -roofRise * 0.3f, roofSpan / 2, 0f)
        templeRoofPath.quadraticTo(roofSpan * 0.22f, 12f, 0f, 15f)
        templeRoofPath.quadraticTo(-roofSpan * 0.22f, 12f, -roofSpan / 2, 0f)
        templeRoofPath.close()

        // Precompute Stone Toro Lantern Path
        stoneLanternPath.reset()
        stoneLanternPath.moveTo(-8f, 0f)
        stoneLanternPath.lineTo(-5f, -32f)
        stoneLanternPath.lineTo(-14f, -36f)
        stoneLanternPath.lineTo(0f, -48f)
        stoneLanternPath.lineTo(14f, -36f)
        stoneLanternPath.lineTo(5f, -32f)
        stoneLanternPath.lineTo(8f, 0f)
        stoneLanternPath.close()
    }
}

object ArenaRenderer {

    private val cache = ArenaRenderCache()

    fun drawArena(
        drawScope: DrawScope,
        arena: ArenaType,
        cameraX: Float,
        gameTime: Float
    ) {
        with(drawScope) {
            val width = size.width
            val height = size.height
            val groundY = height * 0.78f

            // Cache validation without allocations per frame
            cache.updateCacheIfNeeded(width, height, groundY)

            when (arena) {
                ArenaType.CRIMSON_PAGODA -> drawCrimsonPagoda(width, height, groundY, cameraX, gameTime)
                ArenaType.BAMBOO_GROVE -> drawBambooGrove(width, height, groundY, cameraX, gameTime)
                ArenaType.GATES_OF_SHADOWS -> drawGatesOfShadows(width, height, groundY, cameraX, gameTime)
                ArenaType.MOONLIT_TEMPLE -> drawMoonlitTemple(width, height, groundY, cameraX, gameTime)
            }
        }
    }

    private fun DrawScope.drawCrimsonPagoda(
        w: Float,
        h: Float,
        groundY: Float,
        cameraX: Float,
        time: Float
    ) {
        // Sky gradient using cached Brush
        cache.crimsonSkyBrush?.let { brush ->
            drawRect(brush = brush, size = Size(w, groundY))
        }

        // Giant Blood Red Sun with luminous corona
        val sunX = w * 0.5f - cameraX * 0.05f
        val sunY = groundY * 0.48f
        val sunRadius = h * 0.22f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFDE59),
                    Color(0xFFFF4838),
                    Color(0x99D51838),
                    Color(0x00000000)
                ),
                center = Offset(sunX, sunY),
                radius = sunRadius * 1.5f
            ),
            center = Offset(sunX, sunY),
            radius = sunRadius * 1.5f
        )
        drawCircle(
            color = Color(0xFFFF523B),
            center = Offset(sunX, sunY),
            radius = sunRadius
        )

        // Mountain Silhouettes using cached Path with translate
        translate(left = -cameraX * 0.1f, top = 0f) {
            drawPath(cache.mountainPath, color = Color(0x992B0818))
        }

        // Midground Pagoda Temple Silhouette using cached Paths
        val pagodaX = w * 0.72f - cameraX * 0.25f
        drawPagodaSilhouette(pagodaX, groundY, h * 0.55f)

        // Bonsai / Gnarled Pine Tree Silhouette (Left side)
        val treeX = w * 0.12f - cameraX * 0.35f
        drawGnarledPine(treeX, groundY, h * 0.45f)

        // Wooden Dojo Platform Ground using cached Brush
        cache.crimsonGroundBrush?.let { brush ->
            drawRect(
                brush = brush,
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )
        }
        // Red dojo floor trim line
        drawLine(
            color = Color(0xFFC02737),
            start = Offset(0f, groundY),
            end = Offset(w, groundY),
            strokeWidth = 4f
        )

        // Hanging lanterns with pulsating flame aura
        drawLantern(pagodaX - 50f, groundY - 140f, time)
        drawLantern(pagodaX + 50f, groundY - 140f, time)

        // Dynamic drifting Sakura / Momiji leaves blowing across the wind
        drawSakuraWindDrift(w, groundY, cameraX, time)
    }

    private fun DrawScope.drawBambooGrove(
        w: Float,
        h: Float,
        groundY: Float,
        cameraX: Float,
        time: Float
    ) {
        // Sky: Midnight Indigo to Teal Mist
        cache.bambooSkyBrush?.let { brush ->
            drawRect(brush = brush, size = Size(w, groundY))
        }

        // Glowing Full Moon with luminous rings
        val moonX = w * 0.32f - cameraX * 0.04f
        val moonY = groundY * 0.35f
        val moonRadius = h * 0.14f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE8FFFF),
                    Color(0x8876E5FC),
                    Color(0x2200E5FF),
                    Color(0x00000000)
                ),
                center = Offset(moonX, moonY),
                radius = moonRadius * 2.3f
            ),
            center = Offset(moonX, moonY),
            radius = moonRadius * 2.3f
        )
        drawCircle(
            color = Color(0xFFF0FCFF),
            center = Offset(moonX, moonY),
            radius = moonRadius
        )

        // Distant bamboo layers (parallax)
        val numDistantStalks = 14
        for (i in 0 until numDistantStalks) {
            val bx = (i * (w / (numDistantStalks - 1))) - (cameraX * 0.15f) % (w * 1.2f)
            val sway = sin(time * 1.5f + i) * 6f
            drawLine(
                color = Color(0x440B202D),
                start = Offset(bx + sway, 0f),
                end = Offset(bx, groundY),
                strokeWidth = 8f
            )
        }

        // Midground bamboo stalks
        val numMidStalks = 9
        for (i in 0 until numMidStalks) {
            val bx = (i * (w / 7f) + 40f) - (cameraX * 0.32f) % (w * 1.3f)
            val sway = sin(time * 2.0f + i * 2f) * 12f
            drawLine(
                color = Color(0xAA081620),
                start = Offset(bx + sway, 0f),
                end = Offset(bx, groundY),
                strokeWidth = 14f
            )
            // Bamboo joints
            for (j in 1..4) {
                val jy = groundY * (j * 0.2f)
                drawLine(
                    color = Color(0xFF040A0F),
                    start = Offset(bx - 10f + sway * (1f - j * 0.2f), jy),
                    end = Offset(bx + 10f + sway * (1f - j * 0.2f), jy),
                    strokeWidth = 3f
                )
            }
        }

        // Forest Ground
        cache.bambooGroundBrush?.let { brush ->
            drawRect(
                brush = brush,
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )
        }
        drawLine(
            color = Color(0xFF00E5FF),
            start = Offset(0f, groundY),
            end = Offset(w, groundY),
            strokeWidth = 3f
        )

        // Dynamic floating glowing fireflies
        drawBambooFireflies(w, groundY, time)
    }

    private fun DrawScope.drawGatesOfShadows(
        w: Float,
        h: Float,
        groundY: Float,
        cameraX: Float,
        time: Float
    ) {
        // Sky: Ominous Eclipse Purple to Volcanic Orange
        cache.gatesSkyBrush?.let { brush ->
            drawRect(brush = brush, size = Size(w, groundY))
        }

        // Dark Eclipse Moon with Purple Corona
        val eclipseX = w * 0.5f - cameraX * 0.05f
        val eclipseY = groundY * 0.38f
        val eclipseRadius = h * 0.16f
        val pulse = sin(time * 3f) * 0.08f + 1f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xEEBF5AF2),
                    Color(0x889C27B0),
                    Color(0x33673AB7),
                    Color(0x00000000)
                ),
                center = Offset(eclipseX, eclipseY),
                radius = eclipseRadius * 2.6f * pulse
            ),
            center = Offset(eclipseX, eclipseY),
            radius = eclipseRadius * 2.6f * pulse
        )
        // Black eclipse disk
        drawCircle(
            color = Color(0xFF08040C),
            center = Offset(eclipseX, eclipseY),
            radius = eclipseRadius
        )
        drawCircle(
            color = Color(0xFFE040FB),
            center = Offset(eclipseX, eclipseY),
            radius = eclipseRadius,
            style = Stroke(width = 4f)
        )

        // Massive Demonic Torii Gate in Background using cached Path
        val gateX = w * 0.5f - cameraX * 0.2f
        drawDemonToriiGate(gateX, groundY, h * 0.65f, time)

        // Jagged Obsidian Pillars using cached Path
        drawPillar(w * 0.15f - cameraX * 0.35f, groundY)
        drawPillar(w * 0.88f - cameraX * 0.35f, groundY)

        // Magma / Cracked Demonic Ground
        cache.gatesGroundBrush?.let { brush ->
            drawRect(
                brush = brush,
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )
        }
        drawLine(
            color = Color(0xFFE040FB),
            start = Offset(0f, groundY),
            end = Offset(w, groundY),
            strokeWidth = 3f
        )

        // Demonic rising shadow flames / embers
        drawRisingShadowFlames(w, groundY, time)
    }

    private fun DrawScope.drawMoonlitTemple(
        w: Float,
        h: Float,
        groundY: Float,
        cameraX: Float,
        time: Float
    ) {
        // Sky: Midnight Indigo to Silver Mist
        cache.moonlitSkyBrush?.let { brush ->
            drawRect(brush = brush, size = Size(w, groundY))
        }

        // Giant Pearl Moon behind temple rooftop
        val moonX = w * 0.65f - cameraX * 0.04f
        val moonY = groundY * 0.36f
        val moonRadius = h * 0.24f

        // Multi-tier Lunar Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE3F2FD),
                    Color(0x9990CAF9),
                    Color(0x4442A5F5),
                    Color(0x00000000)
                ),
                center = Offset(moonX, moonY),
                radius = moonRadius * 2.2f
            ),
            center = Offset(moonX, moonY),
            radius = moonRadius * 2.2f
        )
        // Solid Moon Disk
        drawCircle(
            color = Color(0xFFF5F9FF),
            center = Offset(moonX, moonY),
            radius = moonRadius
        )
        // Soft crater shading on moon surface
        drawCircle(
            color = Color(0x2280A4C4),
            center = Offset(moonX - moonRadius * 0.25f, moonY - moonRadius * 0.2f),
            radius = moonRadius * 0.32f
        )
        drawCircle(
            color = Color(0x1A80A4C4),
            center = Offset(moonX + moonRadius * 0.3f, moonY + moonRadius * 0.15f),
            radius = moonRadius * 0.42f
        )

        // Drifting Moonlight Clouds across the sky
        drawMoonlitClouds(w, groundY, cameraX, time)

        // Distant Pagoda Spire Silhouette against the moon (Right)
        val spireX = w * 0.78f - cameraX * 0.15f
        drawPagodaSilhouette(spireX, groundY, h * 0.48f)

        // Main Ancient Temple Rooftop Silhouette (Left & Center)
        val templeX = w * 0.28f - cameraX * 0.28f
        drawTempleRoof(templeX, groundY, h * 0.42f)

        // Weathered Temple Roof Tile Floor (Ground)
        cache.moonlitGroundBrush?.let { brush ->
            drawRect(
                brush = brush,
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )
        }
        // Silver-cyan moonlit roof ridge line
        drawLine(
            color = Color(0xFF76D7EA),
            start = Offset(0f, groundY),
            end = Offset(w, groundY),
            strokeWidth = 3.5f
        )
        // Roof tile seam lines
        for (i in 0 until (w / 35f).toInt()) {
            val tx = i * 35f
            drawLine(
                color = Color(0xFF141824),
                start = Offset(tx, groundY),
                end = Offset(tx - 25f, h),
                strokeWidth = 2f
            )
        }

        // Mystical Stone Toro Lanterns on Rooftop
        drawStoneLantern(w * 0.16f - cameraX * 0.35f, groundY, time)
        drawStoneLantern(w * 0.85f - cameraX * 0.35f, groundY, time + 1.5f)
    }

    private fun DrawScope.drawPagodaSilhouette(centerX: Float, groundY: Float, height: Float) {
        val pagodaColor = Color(0xFF12060E)
        val baseY = groundY
        val tiers = 4
        val tierHeight = height / tiers
        var currentW = 140f

        translate(left = centerX, top = baseY) {
            for (tier in 0 until tiers) {
                val ty = -(tier * tierHeight)
                drawPath(cache.pagodaRoofPaths[tier], color = pagodaColor)
                drawRect(
                    color = pagodaColor,
                    topLeft = Offset(-currentW * 0.35f, ty - tierHeight),
                    size = Size(currentW * 0.7f, tierHeight * 0.75f)
                )
                currentW *= 0.82f
            }
            // Pagoda Spire
            drawLine(
                color = pagodaColor,
                start = Offset(0f, -height),
                end = Offset(0f, -height - 35f),
                strokeWidth = 5f
            )
        }
    }

    private fun DrawScope.drawTempleRoof(centerX: Float, groundY: Float, height: Float) {
        val templeColor = Color(0xFF0C101C)
        translate(left = centerX, top = groundY) {
            // Main wide sweeping upper roof
            translate(left = 0f, top = -height) {
                drawPath(cache.templeRoofPath, color = templeColor)
            }
            // Ridge beam
            drawRect(
                color = templeColor,
                topLeft = Offset(-110f, -height + 15f),
                size = Size(220f, height * 0.5f)
            )
            // Lower eaves
            translate(left = 0f, top = -height * 0.45f) {
                drawPath(cache.templeRoofPath, color = templeColor)
            }
            // Supporting columns
            drawRect(
                color = templeColor,
                topLeft = Offset(-80f, -height * 0.45f),
                size = Size(160f, height * 0.45f)
            )
            // Traditional paper shoji window silhouette
            drawRect(
                color = Color(0xFF162540),
                topLeft = Offset(-45f, -height * 0.35f),
                size = Size(90f, height * 0.3f)
            )
            drawLine(
                color = Color(0xFF0C101C),
                start = Offset(0f, -height * 0.35f),
                end = Offset(0f, -height * 0.05f),
                strokeWidth = 3f
            )
        }
    }

    private fun DrawScope.drawStoneLantern(x: Float, groundY: Float, time: Float) {
        val lanternColor = Color(0xFF0D121E)
        translate(left = x, top = groundY) {
            drawPath(cache.stoneLanternPath, color = lanternColor)
            // Glowing interior flame (Spiritual Teal Cyan)
            val flicker = sin(time * 7f) * 0.2f + 0.8f
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.35f * flicker),
                center = Offset(0f, -24f),
                radius = 18f
            )
            drawCircle(
                color = Color(0xFFE0F7FA),
                center = Offset(0f, -24f),
                radius = 5f
            )
        }
    }

    private fun DrawScope.drawMoonlitClouds(
        w: Float,
        groundY: Float,
        cameraX: Float,
        time: Float
    ) {
        val cloudColor = Color(0x334B6E9C)
        val numClouds = 5
        for (i in 0 until numClouds) {
            val cx = ((i * 240f + time * 12f) - cameraX * 0.1f) % (w + 200f) - 100f
            val cy = groundY * (0.18f + i * 0.12f)
            val cw = 160f + (i % 3) * 40f
            drawOval(
                color = cloudColor,
                topLeft = Offset(cx, cy),
                size = Size(cw, 32f)
            )
            drawOval(
                color = cloudColor,
                topLeft = Offset(cx + 35f, cy - 14f),
                size = Size(cw * 0.6f, 30f)
            )
        }
    }

    private fun DrawScope.drawSakuraWindDrift(
        w: Float,
        groundY: Float,
        cameraX: Float,
        time: Float
    ) {
        val numLeaves = 14
        for (i in 0 until numLeaves) {
            val progress = ((time * 0.45f + i * 0.07f) % 1.0f)
            val lx = (progress * (w + 120f) - 60f - cameraX * 0.2f) % (w + 120f)
            val ly = (progress * groundY + sin(time * 3f + i * 1.5f) * 24f) % groundY
            val rot = sin(time * 4f + i) * 8f
            val leafColor = if (i % 2 == 0) Color(0xFFFF5277) else Color(0xFFFF8DA1)

            drawOval(
                color = leafColor.copy(alpha = 0.85f),
                topLeft = Offset(lx, ly),
                size = Size(10f, 6f)
            )
        }
    }

    private fun DrawScope.drawBambooFireflies(
        w: Float,
        groundY: Float,
        time: Float
    ) {
        val numFireflies = 12
        for (i in 0 until numFireflies) {
            val fx = ((i * (w / 11f) + cos(time * 1.2f + i * 2f) * 35f) % w)
            val fy = (groundY * 0.4f + sin(time * 1.5f + i * 1.7f) * (groundY * 0.35f))
            val pulse = (sin(time * 4f + i * 3f) * 0.5f + 0.5f).coerceIn(0.1f, 1f)
            val glowColor = Color(0xFF64FFDA).copy(alpha = 0.5f * pulse)

            drawCircle(
                color = glowColor,
                center = Offset(fx, fy),
                radius = 12f * pulse
            )
            drawCircle(
                color = Color(0xFFE8F5E9),
                center = Offset(fx, fy),
                radius = 2.5f
            )
        }
    }

    private fun DrawScope.drawRisingShadowFlames(
        w: Float,
        groundY: Float,
        time: Float
    ) {
        val numEmbers = 16
        for (i in 0 until numEmbers) {
            val p = ((time * 0.6f + i * 0.06f) % 1.0f)
            val ex = (i * (w / 15f) + sin(time * 3f + i) * 20f) % w
            val ey = groundY - (p * groundY * 0.85f)
            val alpha = (1f - p).coerceIn(0f, 1f)
            val emberColor = if (i % 2 == 0) Color(0xFFE040FB) else Color(0xFFFF1744)

            drawCircle(
                color = emberColor.copy(alpha = alpha * 0.8f),
                center = Offset(ex, ey),
                radius = 3.5f * (1f - p * 0.5f)
            )
        }
    }

    private fun DrawScope.drawGnarledPine(x: Float, groundY: Float, height: Float) {
        val treeColor = Color(0xFF14080F)

        translate(left = x, top = groundY) {
            drawPath(cache.pineTrunkPath, color = treeColor)
            // Pine needle clusters (clouds of foliage)
            drawOval(
                color = treeColor,
                topLeft = Offset(-80f, -height * 0.9f),
                size = Size(90f, 40f)
            )
            drawOval(
                color = treeColor,
                topLeft = Offset(-20f, -height * 1.05f),
                size = Size(100f, 45f)
            )
            drawOval(
                color = treeColor,
                topLeft = Offset(20f, -height * 0.75f),
                size = Size(85f, 38f)
            )
        }
    }

    private fun DrawScope.drawDemonToriiGate(centerX: Float, groundY: Float, height: Float, time: Float) {
        val gateColor = Color(0xFF0F0615)
        val postSpacing = 160f
        val topY = groundY - height

        // Posts
        drawRect(
            color = gateColor,
            topLeft = Offset(centerX - postSpacing / 2 - 12f, topY),
            size = Size(24f, height)
        )
        drawRect(
            color = gateColor,
            topLeft = Offset(centerX + postSpacing / 2 - 12f, topY),
            size = Size(24f, height)
        )

        // Top Main Beam (Kasagi) with cached Path
        translate(left = centerX, top = 0f) {
            drawPath(cache.toriiTopBeamPath, color = gateColor)
        }

        // Second horizontal beam (Nuki)
        drawRect(
            color = gateColor,
            topLeft = Offset(centerX - postSpacing / 2 - 35f, topY + 45f),
            size = Size(postSpacing + 70f, 16f)
        )

        // Glowing Demon Rune in center with demonic eye pulse
        val runePulse = sin(time * 5f) * 0.3f + 0.7f
        drawCircle(
            color = Color(0xFFE040FB).copy(alpha = 0.4f * runePulse),
            center = Offset(centerX, topY + 30f),
            radius = 24f * runePulse
        )
        drawCircle(
            color = Color(0xFFE040FB),
            center = Offset(centerX, topY + 30f),
            radius = 12f
        )
        drawCircle(
            color = Color(0xFFFF1744),
            center = Offset(centerX, topY + 30f),
            radius = 5f
        )
    }

    private fun DrawScope.drawPillar(x: Float, groundY: Float) {
        translate(left = x, top = groundY) {
            drawPath(cache.pillarPath, color = Color(0xFF0C0412))
        }
    }

    private fun DrawScope.drawLantern(x: Float, y: Float, time: Float) {
        val flicker = sin(time * 8f) * 0.15f + 0.85f
        drawCircle(
            color = Color(0xFFFF5722).copy(alpha = 0.4f * flicker),
            center = Offset(x, y + 15f),
            radius = 28f
        )
        drawRect(
            color = Color(0xFFFF9800),
            topLeft = Offset(x - 8f, y + 6f),
            size = Size(16f, 20f)
        )
        drawRect(
            color = Color(0xFF1A0A0E),
            topLeft = Offset(x - 11f, y + 3f),
            size = Size(22f, 5f)
        )
        drawRect(
            color = Color(0xFF1A0A0E),
            topLeft = Offset(x - 11f, y + 24f),
            size = Size(22f, 5f)
        )
    }
}
