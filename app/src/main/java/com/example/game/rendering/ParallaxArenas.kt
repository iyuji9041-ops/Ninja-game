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

    // Cached Paths
    val mountainPath = Path()
    val pineTrunkPath = Path()
    val toriiTopBeamPath = Path()
    val pagodaRoofPaths = Array(4) { Path() }
    val pillarPath = Path()

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

        // Giant Blood Red Sun
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
                radius = sunRadius * 1.4f
            ),
            center = Offset(sunX, sunY),
            radius = sunRadius * 1.4f
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

        // Hanging lanterns glow
        drawLantern(pagodaX - 50f, groundY - 140f, time)
        drawLantern(pagodaX + 50f, groundY - 140f, time)
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

        // Glowing Full Moon
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
                radius = moonRadius * 2.2f
            ),
            center = Offset(moonX, moonY),
            radius = moonRadius * 2.2f
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
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xEEBF5AF2),
                    Color(0x889C27B0),
                    Color(0x33673AB7),
                    Color(0x00000000)
                ),
                center = Offset(eclipseX, eclipseY),
                radius = eclipseRadius * 2.5f
            ),
            center = Offset(eclipseX, eclipseY),
            radius = eclipseRadius * 2.5f
        )
        // Black disk
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
        drawDemonToriiGate(gateX, groundY, h * 0.65f)

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

    private fun DrawScope.drawDemonToriiGate(centerX: Float, groundY: Float, height: Float) {
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

        // Glowing Rune in center
        drawCircle(
            color = Color(0xFFE040FB),
            center = Offset(centerX, topY + 30f),
            radius = 12f
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
