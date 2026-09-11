package com.sukanth.resonance.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of transport symbols Resonance actually uses.
 *
 * Keeping these tiny vectors in the app avoids packaging the 35 MB Material extended-icons AAR.
 */
object ResonanceIcons {
    val MusicNote: ImageVector by lazy {
        vector("MusicNote") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                horizontalLineTo(18f)
                verticalLineTo(7f)
                horizontalLineTo(14f)
                verticalLineTo(16.5f)
                curveTo(14f, 18.43f, 12.21f, 20f, 10f, 20f)
                curveTo(7.79f, 20f, 6f, 18.43f, 6f, 16.5f)
                curveTo(6f, 14.57f, 7.79f, 13f, 10f, 13f)
                curveTo(10.73f, 13f, 11.41f, 13.17f, 12f, 13.46f)
                close()
            }
        }
    }

    val QueueMusic: ImageVector by lazy {
        vector("QueueMusic") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 6f); horizontalLineTo(14f)
                moveTo(4f, 11f); horizontalLineTo(14f)
                moveTo(4f, 16f); horizontalLineTo(10f)
                moveTo(17f, 13f); verticalLineTo(20f)
                curveTo(17f, 21.1f, 15.9f, 22f, 14.5f, 22f)
                curveTo(13.1f, 22f, 12f, 21.2f, 12f, 20.2f)
                curveTo(12f, 19.1f, 13.1f, 18.3f, 14.5f, 18.3f)
                curveTo(15.5f, 18.3f, 16.3f, 18.6f, 17f, 19f)
                moveTo(17f, 13f); horizontalLineTo(21f); verticalLineTo(16f); horizontalLineTo(17f)
            }
        }
    }

    val Pause: ImageVector by lazy {
        vector("Pause") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 5f)
                horizontalLineTo(10f)
                verticalLineTo(19f)
                horizontalLineTo(6f)
                close()
                moveTo(14f, 5f)
                horizontalLineTo(18f)
                verticalLineTo(19f)
                horizontalLineTo(14f)
                close()
            }
        }
    }

    val SkipPrevious: ImageVector by lazy {
        vector("SkipPrevious") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 6f)
                horizontalLineTo(8f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
                moveTo(18f, 6f)
                verticalLineTo(18f)
                lineTo(9.5f, 12f)
                close()
            }
        }
    }

    val SkipNext: ImageVector by lazy {
        vector("SkipNext") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(18f)
                horizontalLineTo(16f)
                close()
                moveTo(6f, 6f)
                lineTo(14.5f, 12f)
                lineTo(6f, 18f)
                close()
            }
        }
    }

    val VolumeOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "VolumeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 9f)
                horizontalLineTo(7f)
                lineTo(12f, 5f)
                verticalLineTo(19f)
                lineTo(7f, 15f)
                horizontalLineTo(3f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16f, 9f)
                lineTo(22f, 15f)
                moveTo(22f, 9f)
                lineTo(16f, 15f)
            }
        }.build()
    }

    val VolumeDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "VolumeDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 9f)
                horizontalLineTo(7f)
                lineTo(12f, 5f)
                verticalLineTo(19f)
                lineTo(7f, 15f)
                horizontalLineTo(3f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 9f)
                curveTo(16.3f, 10.7f, 16.3f, 13.3f, 15f, 15f)
            }
        }.build()
    }

    val VolumeUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "VolumeUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 9f)
                horizontalLineTo(7f)
                lineTo(12f, 5f)
                verticalLineTo(19f)
                lineTo(7f, 15f)
                horizontalLineTo(3f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 9f)
                curveTo(16.3f, 10.7f, 16.3f, 13.3f, 15f, 15f)
                moveTo(18f, 6f)
                curveTo(21.5f, 9.4f, 21.5f, 14.6f, 18f, 18f)
            }
        }.build()
    }

    val RadioButtonUnchecked: ImageVector by lazy {
        ImageVector.Builder(
            name = "RadioButtonUnchecked",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
            }
        }.build()
    }

    val PlaylistPlay: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlaylistPlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(4f, 20.98f)
                curveTo(4f, 21.55f, 4.47f, 22f, 5.04f, 22f)
                horizontalLineTo(18.96f)
                curveTo(19.53f, 22f, 20f, 21.55f, 20f, 20.98f)
                verticalLineTo(18f)
                lineTo(4f, 18f)
                close()
                moveTo(4f, 3.5f)
                curveTo(4f, 4.07f, 4.47f, 4.5f, 5.04f, 4.5f)
                horizontalLineTo(18.96f)
                curveTo(19.53f, 4.5f, 20f, 4.07f, 20f, 3.5f)
                verticalLineTo(1.5f)
                lineTo(4f, 1.5f)
                close()
                moveTo(8f, 12f)
                lineTo(16f, 8.5f)
                verticalLineTo(15.5f)
                lineTo(8f, 12f)
                close()
            }
        }.build()
    }

    val Shuffle: ImageVector by lazy {
        vector("Shuffle") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16f, 3f); horizontalLineTo(21f); verticalLineTo(8f)
                moveTo(4f, 20f); lineTo(21f, 3f)
                moveTo(16f, 16f); lineTo(21f, 21f); horizontalLineTo(16f)
                moveTo(4f, 4f); lineTo(9f, 9f)
            }
        }
    }

    val Repeat: ImageVector by lazy {
        vector("Repeat") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(17f, 2f); lineTo(21f, 6f); lineTo(17f, 10f)
                moveTo(3f, 6f); horizontalLineTo(21f)
                moveTo(7f, 14f); lineTo(3f, 18f); lineTo(7f, 22f)
                moveTo(21f, 18f); horizontalLineTo(3f)
            }
        }
    }

    private fun vector(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()
}
